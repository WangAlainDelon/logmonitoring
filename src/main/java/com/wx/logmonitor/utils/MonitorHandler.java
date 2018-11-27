package com.wx.logmonitor.utils;

import com.wx.logmonitor.dao.LogMonitorDao;
import com.wx.logmonitor.domain.*;
import com.wx.logmonitor.mail.MailInfo;
import com.wx.logmonitor.mail.MailUtils;
import com.wx.logmonitor.mail.MessageSender;
import com.wx.logmonitor.sms.SMSBase;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Describe: 日志监控的核心类，包括了日志监控系统所有的核心处理。
 * Author:   maoxiangyi
 * Domain:   www.itcast.cn
 * Data:     2015/11/11.
 */
public class MonitorHandler {

    private static Logger logger = Logger.getLogger(MonitorHandler.class);
    //定义一个map，其中appId为Key，以该appId下的所有rule为Value
    private static Map<String, List<Rule>> ruleMap;
    //定义一个map,其中appId为Key，以该appId下的所有user为Value
    private static Map<String, List<User>> userMap;
    //定义一个list，用来封装所有的应用信息
    private static List<App> applist;
    //定义一个list，用来封装所有的用户信息
    private static List<User> userList;
    //定时加载配置文件的标识
    private static boolean reloaded = false;
    //定时加载配置文件的标识
    private static long nextReload = 0l;

    static {
        load();
    }

    /**
     * 解析输入的日志，将数据按照一定的规则进行分割。
     * 判断日志是否合法，主要校验日志所属应用的appId是否存在
     *
     * @param line 一条日志
     * @return
     */
    public static Message parser(String line) {
        //$:匹配输入字符串的结尾位置。如果设置了 RegExp 对象的 Multiline 属性，则 $ 也匹配 '\n' 或 '\r'。要匹配 $ 字符本身，请使用 \$。
        //日志内容分为两个部分：由5个$$$$$符号作为分隔符，第一部分为appid，第二部分为日志内容。
        String[] messageArr = line.split("\\$\\$\\$\\$\\$");
        //对日志进行校验
        if (messageArr.length != 2) {
            return null;
        }
        if (StringUtils.isBlank(messageArr[0]) || StringUtils.isBlank(messageArr[1])) {
            return null;
        }
        //检验当前日志所属的appid是否是经过授权的。授权的就是从数据库应用表查询到所有isonline的应用的集合
        if (apppIdisValid(messageArr[0].trim())) {
            Message message = new Message();
            //这里只为消息的内容和应用的名称赋值
            message.setAppId(messageArr[0].trim());
            message.setLine(messageArr[1]);
            return message;
        }
        return null;
    }

    /**
     * 验证appid是否经过授权
     */
    private static boolean apppIdisValid(String appId) {
        try {
            for (App app : applist) {
                if (app.getId() == Integer.parseInt(appId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 对日志进行规制判定，看看是否触发规则
     * @param message
     * @return
     */
    public static boolean trigger(Message message) {
        //如果规则模型为空，需要初始化加载规则模型
        if (ruleMap == null) {
            load();
        }
        //从规则模型中获取当前appid配置的规则
        System.out.println("打印当前传入应用的id"+message.getAppId());
        List<Rule> keywordByAppIdList = ruleMap.get(message.getAppId());
        for (Rule rule : keywordByAppIdList) {
            //如果日志中包含过滤过的关键词，即为匹配成功
            if (message.getLine().contains(rule.getKeyword())) {
                //第二次为Message对象赋值，赋值为规则的id.和包含的关键字
                message.setRuleId(rule.getId() + "");
                message.setKeyword(rule.getKeyword());
                return true;
            }
        }
        return false;
    }

    /**
     * 加载数据模型，主要是用户列表、应用管理表、组合规则模型、组合用户模型。
     */
    public static synchronized void load() {
        if (userList == null) {
            userList = loadUserList();
        }
        if (applist == null) {
            applist = loadAppList();
        }
        if (ruleMap == null) {
            ruleMap = loadRuleMap();
        }
        if (userMap == null) {
            userMap = loadUserMap();
        }
    }

    /**
     * 访问数据库获取所有有效的app列表
     * @return
     */
    private static List<App> loadAppList() {

        return new LogMonitorDao().getAppList();
    }

    /**
     * 访问数据库获取所有有效用户的列表
     * @return
     */
    private static List<User> loadUserList() {

        return new LogMonitorDao().getUserList();
    }

    /**
     * 封装应用与用户对应的map
     * @return
     */
    private static Map<String, List<User>> loadUserMap() {
        //以应用的appId为key，以应用的所有负责人的userList对象为value。
        //HashMap<String, List<User>>
        HashMap<String, List<User>> map = new HashMap<String, List<User>>();
        for (App app : applist) {
            String userIds = app.getUserId();
            List<User> userListInApp = map.get(app.getId());
            if (userListInApp == null) {
                userListInApp = new ArrayList<User>();
                map.put(app.getId() + "", userListInApp);
            }
            String[] userIdArr = userIds.split(",");
            for (String userId : userIdArr) {
                userListInApp.add(queryUserById(userId));
            }
            map.put(app.getId() + "", userListInApp);
        }
        return map;
    }

    /**
     *  封装应用与规则的map：将代表rule的list转化成一个map，转化的逻辑是从rule.getAppId作为map的key，然后将rule对象作为value传入map
     *  因为规则表中有很不同的应用id比如：1，2，3
     *  一个引用名字可以有多个Rule
     * @return
     */
    private static Map<String, List<Rule>> loadRuleMap() {
        Map<String, List<Rule>> map = new HashMap<String, List<Rule>>();
        LogMonitorDao logMonitorDao = new LogMonitorDao();
        //从数据库中查询所有的Rule返回一个集合列表
        List<Rule> ruleList = logMonitorDao.getRuleList();
        //将代表rule的list转化成一个map，转化的逻辑是，
        // 从rule.getAppId作为map的key，然后将rule对象作为value传入map
        //Map<appId,ruleList>  一个appid的规则信息，保存在一个list中。
        for (Rule rule : ruleList) {
            //第一次循环map为空，然后把应用名字作为key,空的Rule集合作为value传入map，因为map中不允许有重复的key，所以要把规则都放到List中。
            List<Rule> ruleListByAppId = map.get(rule.getAppId()+"");
            if (ruleListByAppId == null) {
                ruleListByAppId = new ArrayList<Rule>();
                map.put(rule.getAppId() + "", ruleListByAppId);
            }
            //向map的value(List<Ruel>)添加rule
            ruleListByAppId.add(rule);
            map.put(rule.getAppId() + "", ruleListByAppId);
        }
        return map;
    }

    /**
     * 通过用户编号获取用户的JavaBean
     * @param userId
     * @return
     */
    private static User queryUserById(String userId) {
        for (User user : userList) {
            if (user.getId() == Integer.parseInt(userId)) {
                return user;
            }
        }
        return null;
    }

    /**
     * 通过app编号，获取当前app的所有负责人列表
     * @param appId
     * @return
     */
    public static List<User> getUserIdsByAppId(String appId) {
        return userMap.get(appId);
    }

    /**
     * 告警模块，用来发送邮件和短信
     * 短信功能由于短信资源匮乏，目前默认返回已发送。
     * @param appId
     * @param message
     */
    public static void notifly(String appId, Message message) {
        //通过appId获取应用负责人的对象,其实是一个，也可以是一组
        List<User> users = getUserIdsByAppId(appId);
        //发送邮件
        if (sendMail(appId, users, message)) {
            //将此消息标记为已发邮件
            message.setIsEmail(1);
        }
        //发送短信
       /* if (sendSMS(appId, users, message)) {
            message.setIsPhone(1);
        }*/
    }

    /**
     * 发送短信的模块
     * 由于短信资源匮乏，目前该功能不开启，默认true，即短信发送成功。
     * 目前发送短信功能使用的是外部接口，外面接口的并发性没法保证，会影响storm程序运行的效率。
     *  后期可以改造为将短信数据发送到外部的消息队里中，然后创建一个worker去发送短信。
     * @param appId
     * @param users
     * @param message
     * @return
     */
    private static boolean sendSMS(String appId, List<User> users, Message message) {
//        return true;
        List<String> mobileList = new ArrayList<String>();
        for (User user : users) {
            mobileList.add(user.getMobile());
        }
        for (App app : applist) {
            if (app.getId() == Integer.parseInt(appId.trim())) {
                message.setAppName(app.getName());
                break;
            }
        }
        String content = "系统【" + message.getAppName() + "】在 " + DateUtils.getDateTime() + " 触发规则 " + message.getRuleId() + ",关键字：" + message.getKeyword();
        return SMSBase.sendSms(listToStringFormat(mobileList), content);
    }

    /**
     * 发送邮件
     * 后期可以改造为将邮件数据发送到外部的消息队里中，然后创建一个worker去发送短信。
     * @param appId
     * @param userList
     * @param message
     * @return
     */
    private static boolean sendMail(String appId, List<User> userList, Message message) {
        List<String> receiver = new ArrayList<String>();
        //拿到负责该应用维护的负责人的邮箱
        for (User user : userList) {
            receiver.add(user.getEmail());
        }
        //通过应用的id拿到具体应用的名称
        for (App app : applist) {
            if (app.getId() == Integer.parseInt(appId.trim())) {
                message.setAppName(app.getName());
                break;
            }
        }
        if (receiver.size() >= 1) {
            String date = DateUtils.getDateTime();
            String content = "系统【" + message.getAppName() + "】在 " + date + " 触发规则 " + message.getRuleId() + " ，过滤关键字为：" + message.getKeyword() + "  错误内容：" + message.getLine();
            MailInfo mailInfo = new MailInfo("系统运行日志监控", content, receiver, null);
            //return MailUtils.sendMail(mailInfo);
            return MessageSender.sendMail(mailInfo);
        }
        return false;
    }

    /**
     * 保存触发规则的信息，将触发信息写入到mysql数据库中。
     *
     * @param record
     */
    public static void save(Record record) {
        new LogMonitorDao().saveRecord(record);
    }

    /**
     * 将list转换为String
     * @param list
     * @return
     */
    private static String listToStringFormat(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                stringBuilder.append(list.get(i));
            } else {
                stringBuilder.append(list.get(i)).append(",");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 配置scheduleLoad重新加载底层数据模型。
     */
    /**
     * thread 4
     * thread 3
     * thread 2
     */
    public static synchronized void reloadDataModel() {
//        * thread 1  reloaded = true   ----> reloaded = false
//        * thread 2  reloaded = false
//        * thread 2  reloaded = false
 //        * thread 2  reloaded = false
        if (reloaded) {
            long start = System.currentTimeMillis();
            userList = loadUserList();
            applist = loadAppList();
            ruleMap = loadRuleMap();
            userMap = loadUserMap();
            reloaded = false;
            nextReload = 0l;
            logger.info("配置文件reload完成，时间："+DateUtils.getDateTime()+" 耗时："+ (System.currentTimeMillis()-start));
        }


    }

    /**
     * 定时加载配置信息
     * 配合reloadDataModel模块一起使用。
     * 主要实现原理如下：
     * 1，获取分钟的数据值，当分钟数据是10的倍数，就会触发reloadDataModel方法，简称reload时间。
     * 2，reloadDataModel方式是线程安全的，在当前worker中只有一个线程能够操作。
     * 3，为了保证当前线程操作完毕之后，其他线程不再重复操作，设置了一个标识符reloaded。
     *      在非reload时间段时，reloaded一直被置为true；
     *      在reload时间段时，第一个线程进入reloadDataModel后，加载完毕之后会将reloaded置为false。
     */
    public static void scheduleLoad() {
//        String date = DateUtils.getDateTime();
//        int now = Integer.parseInt(date.split(":")[1]);
//        if (now % 10 == 0) {//每10分钟加载一次
//            //1,2,3,4,5,6
//            reloadDataModel();
//        }else {
//            reloaded = true;
//        }

        if (System.currentTimeMillis()==nextReload){
            //thread 1,2,3,
            reloadDataModel();
        }


    }
}

package com.wx.logmonitor.bolt;

import com.wx.logmonitor.domain.Message;
import com.wx.logmonitor.utils.MonitorHandler;
import org.apache.log4j.Logger;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import java.util.Map;

/**
 * Describe: 过滤规则信息
 */
//BaseRichBolt 需要手动调ack方法，BaseBasicBolt由storm框架自动调ack方法
public class FilterBolt extends BaseBasicBolt {
    private static Logger logger = Logger.getLogger(FilterBolt.class);
    @Override
    public void prepare(Map stormConf, TopologyContext context) {
        super.prepare(stormConf, context);
    }

    public void execute(Tuple input, BasicOutputCollector collector) {
        //获取KafkaSpout发送出来的数据
        String line = input.getString(0);
        //获取kafka发送的数据，是一个byte数组
//        byte[] value = (byte[]) input.getValue(0);
        //将数组转化成字符串
//        String line = new String(value);
        //对数据进行解析
        // appid   content
        //1  error: Caused by: java.lang.NoClassDefFoundError: com/starit/gejie/dao/SysNameDao
        //把读到的数据转化为一个自定义消息对象，暂时只赋值了消息的内容和应用的名称两个字段
        Message message =  MonitorHandler.parser(line);
        if (message == null) {
            return;
        }
        //对日志进行规制判定，看看是否触发规则，如果满足条件，message的对应规则id和关键字属性会被赋值
        if (MonitorHandler.trigger(message)) {
            //定义两个域，输出到下游进行处理。
            collector.emit(new Values(message.getAppId(), message));
        }
        //定时更新规则信息
        MonitorHandler.scheduleLoad();
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        //定义这两个域，然后输出交给下游Bolt处理。
        declarer.declare(new Fields("appId", "message"));
    }
}

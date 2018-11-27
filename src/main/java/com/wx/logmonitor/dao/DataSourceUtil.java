package com.wx.logmonitor.dao;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.wx.logmonitor.domain.Record;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Date;

/**
 * Describe: 请补充类描述

 */
public class DataSourceUtil {
    private static Logger logger = Logger.getLogger(DataSourceUtil.class);
    private static DataSource dataSource;

    static {
        dataSource = new ComboPooledDataSource("logMonitor");
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource();
        }
        return dataSource;
    }

    public static void main(String[] args) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//        String sql = "SELECT `id`,`name`,`keyword`,`isValid`,`appId` FROM `log_monitor`.`log_monitor_rule` WHERE isValid =1";
//        System.out.println(jdbcTemplate.query(sql, new BeanPropertyRowMapper<RuleField>(RuleField.class)));
        //这个是测试数据库是否联通
        Record record = new Record();
        record.setAppId(1111);
        record.setRuleId(1);
        record.setIsEmail(1);
        record.setIsPhone(1);
        record.setIsColse(0);
        String sql = "INSERT INTO `log_monitor`.`log_monitor_rule_record`" +
                " (`appId`,`ruleId`,`isEmail`,`isPhone`,`isColse`,`noticeInfo`,`updataDate`) " +
                "VALUES ( ?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, record.getAppId(), record.getRuleId(), record.getIsEmail(), record.getIsPhone(), 0, record.getLine(),new Date());

    }
}

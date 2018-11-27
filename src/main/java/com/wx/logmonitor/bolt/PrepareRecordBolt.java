package com.wx.logmonitor.bolt;

import com.wx.logmonitor.domain.Message;
import com.wx.logmonitor.domain.Record;
import com.wx.logmonitor.utils.MonitorHandler;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

/**
 * Describe: 将触发信息保存到mysql数据库中
 */
//BaseRichBolt 需要手动调ack方法，BaseBasicBolt由storm框架自动调ack方法
public class PrepareRecordBolt extends BaseBasicBolt {
    private static Logger logger = Logger.getLogger(PrepareRecordBolt.class);

    public void execute(Tuple input, BasicOutputCollector collector) {
        Message message = (Message) input.getValueByField("message");
        String appId = input.getStringByField("appId");
        //将触发规则的信息进行通知，
        MonitorHandler.notifly(appId, message);
        Record record = new Record();
        try {
            BeanUtils.copyProperties(record, message);
            //定义记录域，输出这个记录交给下游处理
            collector.emit(new Values(record));
        } catch (Exception e) {

        }
    }
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

        declarer.declare(new Fields("record"));
    }

}

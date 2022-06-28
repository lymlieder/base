package com.example.demo.baseFunction;

import com.aliyun.openservices.ons.api.*;
import com.aliyun.openservices.ons.api.order.ConsumeOrderContext;
import com.aliyun.openservices.ons.api.order.MessageOrderListener;
import com.aliyun.openservices.ons.api.order.OrderAction;
import com.aliyun.openservices.ons.api.order.OrderConsumer;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Log4j2
@Component
public class RocketMqGetter {

    Properties properties = new Properties();
    Consumer consumer;

    public RocketMqGetter() {
        Config.fillConfig("rocketMq.properties", properties);
        consumer = ONSFactory.createConsumer(properties);
    }

    public RocketMqGetter(String groupId) {
        Config.fillConfig("rocketMq.properties", properties);
        if (groupId != null)
            properties.setProperty(PropertyKeyConst.GROUP_ID, groupId);
        consumer = ONSFactory.createConsumer(properties);
    }

    public void BookMessage(String topic, MessageListener messageListener) {
        // 集群订阅方式（默认）。
        // properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);
        // 广播订阅方式。
        // properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.BROADCASTING);

        consumer.subscribe(topic, "*", messageListener);
//        MessageListener messageListener1 = new MessageListener() { //订阅多个 Tag
//            public Action consume(Message message, ConsumeContext context) {
//                log.info("Receive: " + message);
//                return Action.CommitMessage;
//            }
//        };
//        订阅另外一个 Topic
//        consumer.subscribe(topic1, "*", messageListener);
        consumer.start();
        log.info("Consumer Started");
    }

    public void BookMessageOrder(String topic, String tags) {

        MessageOrderListener messageListener = new MessageOrderListener() {
            /**
             * 1. 消息消费处理失败或者处理出现异常，返回 OrderAction.Suspend。
             * 2. 消息处理成功，返回 OrderAction.Success。
             */
            @Override
            public OrderAction consume(Message message, ConsumeOrderContext context) {
                log.info(message);
                return OrderAction.Success;
            }
        };
        BookMessageOrder(topic, tags, messageListener);
    }

    public void BookMessageOrder(String topic, String tags, MessageOrderListener messageListener) {
// 在订阅消息前，必须调用 start 方法来启动 Consumer，只需调用一次即可。
        OrderConsumer consumer = ONSFactory.createOrderedConsumer(properties);
        consumer.subscribe(topic, tags, messageListener);
        consumer.start();
    }
}

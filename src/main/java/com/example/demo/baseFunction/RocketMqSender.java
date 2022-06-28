package com.example.demo.baseFunction;

import com.aliyun.openservices.ons.api.*;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.example.demo.CommonFunction;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.callback.Callback;
import java.util.*;
//https://github.com/apache/rocketmq/blob/master/docs/cn/RocketMQ_Example.md

@Log4j2
@RestController
public class RocketMqSender {

    Properties properties = new Properties();
    Producer producer;

    public RocketMqSender() {
        Config.fillConfig("rocketMq.properties", properties);
        producer = ONSFactory.createProducer(properties);
        producer.start();
    }

    public RocketMqSender(String groupId) {
        Config.fillConfig("rocketMq.properties", properties);
        if (groupId != null)
            properties.setProperty(PropertyKeyConst.GROUP_ID, groupId);
        producer = ONSFactory.createProducer(properties);
        producer.start();
    }

    public SendResult sendMessage(String topic, String tags, String key, String message) {

        Message msg = new Message(topic, tags, message.getBytes());
        // 设置代表消息的业务关键属性，请尽可能全局唯一。以方便您在无法正常收到消息情况下，可通过控制台查询消息并补发。
        // 注意：不设置也不会影响消息正常收发。
        if (key != null)
            msg.setKey(key);

        return producer.send(msg);
    }

    public SendResult sendDelayMessage_core(String topic, String tags, String key, String message, long time) {
        // 延时消息，单位毫秒（ms），在指定延迟时间（当前时间之后）进行投递，例如消息在3秒后投递。
        long delayTime = CommonFunction.getTimestamp() + time * 1000;

        Message msg = new Message(topic, tags, message.getBytes());
        // 设置消息需要被投递的时间。
        msg.setStartDeliverTime(delayTime);
        if (key != null)
            msg.setKey(key);

        return producer.send(msg);
    }

    public SendResult sendDelayMessage(String topic, String tags, String key, String message, long delay_s) {
        // 延时消息，单位毫秒（ms），在指定延迟时间（当前时间之后）进行投递，例如消息在3秒后投递。
        long delayTime = CommonFunction.getTimestamp() + delay_s * 1000;

        return sendDelayMessage_core(topic, tags, key, message, delayTime);
    }

    //多线程普通消息
    public void multiThreadSendMessage(String topic, String tag, String message, String key, Map<String, Object> result) {
        Thread thread = new Thread(() -> {
            Message msg = new Message(
                    // 普通消息所属的Topic，切勿使用普通消息的Topic来收发其他类型的消息。
                    topic,
                    // Message Tag可理解为Gmail中的标签，对消息进行再归类，方便Consumer指定过滤条件在消息队列RocketMQ版的服务器过滤。
                    tag,
                    // Message Body可以是任何二进制形式的数据，消息队列RocketMQ版不做任何干预。
                    // 需要Producer与Consumer协商好一致的序列化和反序列化方式。
                    message.getBytes());
            if (key != null)
                msg.setKey(key);
            SendResult sendResult = producer.send(msg);
            // 同步发送消息，只要不抛异常就是成功。
            if (sendResult != null)
                result.put("false", sendResult);
        });
        thread.setUncaughtExceptionHandler((t, e) -> result.put("false", e.getMessage()));
        thread.start();
        result.put("true", "");
    }

    //异步,有回调
    public void asyncSendMessage(String topic, String tag, String message, String key, SendCallback sendCallback) {
        Message msg = new Message(topic, tag, message.getBytes());

        // 设置代表消息的业务关键属性，请尽可能全局唯一。 以方便您在无法正常收到消息情况下，可通过控制台查询消息并补发。
        // 注意：不设置也不会影响消息正常收发。
        if (key != null)
            msg.setKey(key);

        // 异步发送消息, 发送结果通过 callback 返回给客户端。
        producer.sendAsync(msg, sendCallback);
    }

    //异步，无回调
    public void asyncSendMessage(String topic, String tag, String message, String key) {
        // 异步发送消息, 发送结果通过 callback 返回给客户端，默认处理
        asyncSendMessage(topic, tag, message, key, new SendCallback() {
            @Override
            public void onSuccess(final SendResult sendResult) {
                // 消息发送成功。
                log.info("send message success. topic=" + sendResult.getTopic() + ", msgId=" + sendResult.getMessageId());
            }

            @Override
            public void onException(OnExceptionContext context) {
                // 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理。
                log.info("send message failed. topic=" + context.getTopic() + ", msgId=" + context.getMessageId());
            }
        });
    }

    static class MessageDetail {
        public String tag;
        public String shardingKey;
        public String key;
        public String body;
        public int groupId;
    }

    //顺序发送
    public List<SendResult> SendMessageOrder(String topic, List<MessageDetail> messageList) {
        OrderProducer producer = ONSFactory.createOrderProducer(properties);
        // 在发送消息前，必须调用 start 方法来启动 Producer，只需调用一次即可。
        producer.start();
        List<SendResult> sendResultList = new ArrayList<>();
        for (int i = 0; i < messageList.size(); i++) {
            MessageDetail messageDetail = messageList.get(i);
            Message msg = new Message(topic, messageDetail.tag, messageDetail.body.getBytes());
            // 设置代表消息的业务关键属性，请尽可能全局唯一。
            // 以方便您在无法正常收到消息情况下，可通过控制台查询消息并补发。
            // 注意：不设置也不会影响消息正常收发。
            if (messageDetail.key != null)
                msg.setKey(messageDetail.key);
            else
                msg.setKey("index_" + i);
            // 分区顺序消息中区分不同分区的关键字段，Sharding Key 与普通消息的 key 是完全不同的概念。
            // 全局顺序消息，该字段可以设置为任意非空字符串。
            if (messageDetail.shardingKey == null)
                messageDetail.shardingKey = "shardingKey";
            // 发送消息，只要不抛异常就是成功。
            sendResultList.add(producer.send(msg, messageDetail.shardingKey));
        }
        producer.shutdown();
        return sendResultList;
    }

}

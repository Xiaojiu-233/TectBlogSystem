package com.blog.component;

import com.blog.entity.User;
import com.blog.service.UserService;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

//ramq消息消费者
@Component
@Slf4j
public class RabbitMsgConsumer {

    //成员属性
    @Resource
    private UserService userService;

    @Autowired
    private ConnectionFactory connectionFactory;

    //监听队列
    //监听队列DataCacheQueue
    @RabbitListener(queues = "DataCacheQueue")
    public void dataCacheProcess(String message) {
        log.info("DataCacheQueue消费者收到消息  {}" , message);
    }

    //监听队列UserForbidQueue
    @RabbitListener(queues = "UserForbidReadyQueue")
    public void userForbidProcess(String message) {
        log.info("UserForbidQueue消费者收到消息  {}" , message);
        //拆分消息
        String[] sp = message.split("::");
        User user  = userService.getById(Long.parseLong(sp[0]));
        //确定用户真实性
        if(user == null)return;
        //如果当前时间是在解封时间之后，则进行解封，否则不解封
        long unlockTime = Long.parseLong(sp[1]);
        long nowTime = System.currentTimeMillis();
        if(nowTime > unlockTime){
            user.setIsLock(0L);
            userService.updateById(user);
        }
    }

    //监听队列BlogPublishQueue
    @RabbitListener(queues = "BlogPublishReadyQueue")
    public void blogPublishProcess(String message) {
        log.info("blogPublishProcess消费者收到消息  {}" , message);
    }

    //监听队列CancelForbidQueue
    @RabbitListener(queues = "CancelForbidQueue")
    public void cancelForbidMessage(String messageId) throws Exception {
        log.info("CancelForbidQueue消费者收到消息  {}" , messageId);
        Connection connection = connectionFactory.createConnection();
        Channel channel = connection.createChannel(false);
        GetResponse response = channel.basicGet("UserForbidQueue", false);
        while (response != null) {
            String id = response.getProps().getMessageId();
            if (messageId.equals(id)) {
                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                break;
            }
            response = channel.basicGet("UserForbidQueue", false);
        }
        channel.close();
        connection.close();
    }
}

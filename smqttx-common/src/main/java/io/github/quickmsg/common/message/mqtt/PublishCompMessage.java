package io.github.quickmsg.common.message.mqtt;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.quickmsg.common.channel.MqttChannel;
import io.github.quickmsg.common.context.ReceiveContext;
import io.github.quickmsg.common.message.Message;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import lombok.Data;

import java.util.Date;

/**
 * @author luxurong
 */
@Data
public class PublishCompMessage implements Message {

    private int messageId;
    private String connectTime;

    @JsonIgnore
    private MqttChannel mqttChannel;


    public PublishCompMessage(Object message, MqttChannel mqttChannel){
        this.mqttChannel  = mqttChannel;
        this.messageId=((MqttMessageIdVariableHeader) ((MqttMessage) message).variableHeader()).messageId();
        this.connectTime = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT);
    }

}

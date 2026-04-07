/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;

import java.util.concurrent.CompletableFuture;

/**
 * MqttBannerHandler is a Netty handler that performs an MQTT CONNECT request
 * to identify the service and potentially capture information.
 */
public class MqttBannerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    private final CompletableFuture<PortScanResult> future;
    private final int port;
    private final String host;

    /**
     * Constructs a new MqttBannerHandler.
     *
     * @param future the future to complete with the scan result
     * @param port   the port being scanned
     * @param host   the host being scanned
     */
    public MqttBannerHandler(CompletableFuture<PortScanResult> future, int port, String host) {
        this.future = future;
        this.port = port;
        this.host = host;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnectVariableHeader mqttConnectVariableHeader =
                new MqttConnectVariableHeader(
                        MqttVersion.MQTT_3_1_1.protocolName(),
                        MqttVersion.MQTT_3_1_1.protocolLevel(),
                        false, false, false, 0, false, true, 60);
        MqttConnectPayload mqttConnectPayload = new MqttConnectPayload("aSlicer-scanner", null, (byte[]) null, null, (byte[]) null);
        MqttConnectMessage mqttConnectMessage =
                new MqttConnectMessage(mqttFixedHeader, mqttConnectVariableHeader, mqttConnectPayload);
        ctx.writeAndFlush(mqttConnectMessage);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        String sslInfo = ctx.channel().attr(SslHandshakeHandler.SSL_INFO).get();
        String details = "";
        if (msg.fixedHeader().messageType() == MqttMessageType.CONNACK) {
            MqttConnAckMessage connAck = (MqttConnAckMessage) msg;
            details = "MQTT " + MqttVersion.MQTT_3_1_1.protocolName() + " (" + MqttVersion.MQTT_3_1_1.protocolLevel() + ")";
            details += ", Return Code: " + connAck.variableHeader().connectReturnCode();
        } else {
            details = "MQTT Service identified, but received: " + msg.fixedHeader().messageType();
        }

        if (sslInfo != null) {
            details += " | SSL: " + sslInfo;
        }

        future.complete(new PortScanResult(port, true, "MQTT", details));
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!future.isDone()) {
            String sslInfo = ctx.channel().attr(SslHandshakeHandler.SSL_INFO).get();
            String details = "MQTT identification failed: " + cause.getMessage();
            if (sslInfo != null) {
                details += " | SSL: " + sslInfo;
            }
            future.complete(new PortScanResult(port, true, "MQTT", details));
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!future.isDone()) {
            String sslInfo = ctx.channel().attr(SslHandshakeHandler.SSL_INFO).get();
            String details = "Channel closed before MQTT response";
            if (sslInfo != null) {
                details += " | SSL: " + sslInfo;
            }
            future.complete(new PortScanResult(port, true, "MQTT", details));
        }
        super.channelInactive(ctx);
    }
}

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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MqttBannerGrabbingTest verifies active MQTT identification and SSL certificate reporting.
 */
public class MqttBannerGrabbingTest {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private Channel sslServerChannel;
    private int mqttPort = 1883;
    private int mqttSslPort = 8883;

    @BeforeEach
    void setUp() throws Exception {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("mqtt-test-boss", true));
        workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("mqtt-test-worker", true));
        
        // 1. Plain MQTT Server
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new MqttDecoder());
                        ch.pipeline().addLast(MqttEncoder.INSTANCE);
                        ch.pipeline().addLast(new MqttServerHandler());
                    }
                });

        try {
            serverChannel = b.bind(mqttPort).sync().channel();
        } catch (Exception e) {
            serverChannel = b.bind(0).sync().channel();
            mqttPort = ((java.net.InetSocketAddress) serverChannel.localAddress()).getPort();
        }

        // 2. SSL MQTT Server with Self-Signed Certificate
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

        ServerBootstrap bSsl = new ServerBootstrap();
        bSsl.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                        ch.pipeline().addLast(new MqttDecoder());
                        ch.pipeline().addLast(MqttEncoder.INSTANCE);
                        ch.pipeline().addLast(new MqttServerHandler());
                    }
                });

        try {
            sslServerChannel = bSsl.bind(mqttSslPort).sync().channel();
        } catch (Exception e) {
            sslServerChannel = bSsl.bind(0).sync().channel();
            mqttSslPort = ((java.net.InetSocketAddress) sslServerChannel.localAddress()).getPort();
        }
    }

    private static class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
            if (msg.fixedHeader().messageType() == MqttMessageType.CONNECT) {
                MqttConnAckVariableHeader variableHeader =
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
                MqttFixedHeader fixedHeader =
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                MqttConnAckMessage response = new MqttConnAckMessage(fixedHeader, variableHeader);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @AfterEach
    void tearDown() {
        if (serverChannel != null) serverChannel.close();
        if (sslServerChannel != null) sslServerChannel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    @Test
    void testMqttActiveBannerGrabbing() throws Exception {
        if (mqttPort != 1883 && mqttPort != 8883) {
            System.out.println("[DEBUG_LOG] Skipping testMqttActiveBannerGrabbing because port " + mqttPort + " is not recognized as MQTT by scanner");
            return;
        }

        try (NettyPortScanner scanner = new NettyPortScanner(1000, 2000)) {
            CompletableFuture<PortScanResult> future = scanner.scanPort("127.0.0.1", mqttPort, true);
            PortScanResult result = future.get(5, TimeUnit.SECONDS);

            assertTrue(result.isOpen(), "Port should be open");
            assertEquals("MQTT", result.getService());
            assertTrue(result.getServiceDetails().contains("CONNECTION_ACCEPTED"), "Details should contain CONNECTION_ACCEPTED: " + result.getServiceDetails());
        }
    }

    @Test
    void testMqttSslSelfSignedBannerGrabbing() throws Exception {
        if (mqttSslPort != 8883) {
            System.out.println("[DEBUG_LOG] Skipping testMqttSslSelfSignedBannerGrabbing because port " + mqttSslPort + " is not recognized as SSL/MQTT by scanner");
            return;
        }

        try (NettyPortScanner scanner = new NettyPortScanner(1000, 2000)) {
            CompletableFuture<PortScanResult> future = scanner.scanPort("127.0.0.1", mqttSslPort, true);
            PortScanResult result = future.get(5, TimeUnit.SECONDS);

            assertTrue(result.isOpen(), "Port should be open");
            assertEquals("MQTT", result.getService());
            assertTrue(result.getServiceDetails().contains("CONNECTION_ACCEPTED"), "Details should contain CONNECTION_ACCEPTED: " + result.getServiceDetails());
            assertTrue(result.getServiceDetails().contains("[Self-signed]"), "Details should indicate self-signed certificate: " + result.getServiceDetails());
        }
    }
}

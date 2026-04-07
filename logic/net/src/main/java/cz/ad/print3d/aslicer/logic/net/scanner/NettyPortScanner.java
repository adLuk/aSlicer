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
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import javax.net.ssl.SSLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * NettyPortScanner uses Netty to check if a specific port on a given IP address is open.
 */
public class NettyPortScanner implements PortScanner {

    private static final Logger LOGGER = Logger.getLogger(NettyPortScanner.class.getName());
    private static final List<Integer> HTTP_PORTS = Arrays.asList(80, 443, 8080, 5000, 7125, 8000);
    private static final List<Integer> SSL_PORTS = Arrays.asList(443, 990, 8883);
    private static final List<Integer> MQTT_PORTS = Arrays.asList(1883, 8883);

    private final EventLoopGroup group;
    private int timeoutMillis;
    private int bannerTimeoutMillis;
    private SslContext sslContext;

    /**
     * Constructs a new NettyPortScanner with default timeout (500ms) and a shared event loop group.
     */
    public NettyPortScanner() {
        this(500, 2000);
    }

    /**
     * Constructs a new NettyPortScanner with a specified timeout.
     *
     * @param timeoutMillis the timeout for connection attempts in milliseconds
     */
    public NettyPortScanner(int timeoutMillis) {
        this(timeoutMillis, 2000);
    }

    /**
     * Constructs a new NettyPortScanner with a specified connection and banner timeout.
     *
     * @param timeoutMillis       the timeout for connection attempts in milliseconds
     * @param bannerTimeoutMillis the timeout for banner grabbing in milliseconds
     */
    public NettyPortScanner(int timeoutMillis, int bannerTimeoutMillis) {
        this.group = new NioEventLoopGroup(0, new DefaultThreadFactory("netty-port-scanner", true));
        this.timeoutMillis = timeoutMillis;
        this.bannerTimeoutMillis = bannerTimeoutMillis;
        try {
            this.sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            LOGGER.warning("Failed to create SSL context: " + e.getMessage());
        }
    }

    private boolean isHttpPort(int port) {
        return HTTP_PORTS.contains(port);
    }

    private boolean isMqttPort(int port) {
        return MQTT_PORTS.contains(port);
    }

    private boolean isSslPort(int port) {
        return SSL_PORTS.contains(port);
    }

    @Override
    public CompletableFuture<PortScanResult> scanPort(String host, int port) {
        return scanPort(host, port, false);
    }

    @Override
    public CompletableFuture<PortScanResult> scanPort(String host, int port, boolean useBannerGrabbing) {
        CompletableFuture<PortScanResult> future = new CompletableFuture<>();

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        if (useBannerGrabbing) {
                            ch.pipeline().addLast(new ReadTimeoutHandler(bannerTimeoutMillis, TimeUnit.MILLISECONDS));
                            
                            if (isSslPort(port) && sslContext != null) {
                                ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), host, port));
                                ch.pipeline().addLast(new SslHandshakeHandler());
                            }
                            
                            if (isHttpPort(port)) {
                                ch.pipeline().addLast(new HttpClientCodec());
                                ch.pipeline().addLast(new HttpObjectAggregator(65536));
                                ch.pipeline().addLast(new HttpBannerHandler(future, port, host));
                            } else if (isMqttPort(port)) {
                                ch.pipeline().addLast(new MqttDecoder());
                                ch.pipeline().addLast(MqttEncoder.INSTANCE);
                                ch.pipeline().addLast(new MqttBannerHandler(future, port, host));
                            } else {
                                ch.pipeline().addLast(new BannerHandler(future, port));
                            }
                        }
                    }
                });

        ChannelFuture connectFuture = b.connect(host, port);
        connectFuture.addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                future.complete(new PortScanResult(port, false));
            } else if (!useBannerGrabbing) {
                f.channel().close();
                future.complete(new PortScanResult(port, true));
            }
            // If useBannerGrabbing is true, the BannerHandler will complete the future
        });

        future.whenComplete((res, ex) -> {
            if (future.isCancelled()) {
                connectFuture.cancel(true);
                if (connectFuture.channel() != null) {
                    connectFuture.channel().close();
                }
            }
        });

        return future;
    }

    @Override
    public void setTimeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public int getTimeout() {
        return timeoutMillis;
    }

    @Override
    public void stopScan() {
        // NettyPortScanner uses CompletableFuture for each port scan, which should be cancelled individually
        // through NettyNetworkScanner.
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }
}

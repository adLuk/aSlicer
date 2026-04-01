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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * NettyPortScanner uses Netty to check if a specific port on a given IP address is open.
 */
public class NettyPortScanner implements PortScanner {

    private final EventLoopGroup group;
    private final int timeoutMillis;
    private final int bannerTimeoutMillis;

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
        this.group = new NioEventLoopGroup();
        this.timeoutMillis = timeoutMillis;
        this.bannerTimeoutMillis = bannerTimeoutMillis;
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
                            ch.pipeline().addLast(new BannerHandler(future, port));
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
    public void close() {
        group.shutdownGracefully();
    }
}

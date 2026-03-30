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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortScannerTest {

    private NettyPortScanner portScanner;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private int testPort;

    @BeforeEach
    void setUp() throws IOException {
        portScanner = new NettyPortScanner(200);
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        testPort = findFreePort();
    }

    @AfterEach
    void tearDown() {
        portScanner.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private Channel startServer(int port) throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        // Just accept connection
                    }
                });
        ChannelFuture f = b.bind(port).sync();
        return f.channel();
    }

    @Test
    void testScanOpenPort() throws InterruptedException, ExecutionException, TimeoutException {
        Channel serverChannel = startServer(testPort);
        try {
            CompletableFuture<PortScanResult> future = portScanner.scanPort("127.0.0.1", testPort);
            PortScanResult result = future.get(1, TimeUnit.SECONDS);
            assertTrue(result.isOpen(), "Port " + testPort + " should be reported as open");
        } finally {
            serverChannel.close().sync();
        }
    }

    @Test
    void testScanClosedPort() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        // Use a port that is likely closed
        int closedPort = findFreePort();
        CompletableFuture<PortScanResult> future = portScanner.scanPort("127.0.0.1", closedPort);
        PortScanResult result = future.get(1, TimeUnit.SECONDS);
        assertFalse(result.isOpen(), "Port " + closedPort + " should be reported as closed");
    }

    @Test
    void testBannerGrabbing() throws InterruptedException, ExecutionException, TimeoutException {
        String expectedBanner = "Welcome to Printer Server v1.0";
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new io.netty.channel.ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(io.netty.channel.ChannelHandlerContext ctx) {
                                ctx.writeAndFlush(io.netty.buffer.Unpooled.copiedBuffer(expectedBanner, java.nio.charset.StandardCharsets.UTF_8));
                            }
                        });
                    }
                });
        Channel serverChannel = b.bind(testPort).sync().channel();

        try {
            CompletableFuture<PortScanResult> future = portScanner.scanPort("127.0.0.1", testPort, true);
            PortScanResult result = future.get(3, TimeUnit.SECONDS);
            assertTrue(result.isOpen());
            assertEquals("Text Banner", result.getService());
            assertEquals(expectedBanner, result.getServiceDetails());
        } finally {
            serverChannel.close().sync();
        }
    }

    @Test
    void testBannerGrabbingTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        // Server that doesn't send any banner
        Channel serverChannel = startServer(testPort);

        try {
            // Use a short banner timeout to speed up test
            try (NettyPortScanner customScanner = new NettyPortScanner(200, 500)) {
                CompletableFuture<PortScanResult> future = customScanner.scanPort("127.0.0.1", testPort, true);
                PortScanResult result = future.get(2, TimeUnit.SECONDS);
                assertTrue(result.isOpen());
                assertFalse(result.getService() != null && !result.getService().isEmpty(), "Service should be empty if no banner received");
            }
        } finally {
            serverChannel.close().sync();
        }
    }
}

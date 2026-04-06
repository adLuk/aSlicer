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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for active banner grabbing (HTTP GET and handshake-based identification).
 */
public class ActiveBannerGrabbingTest {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private int httpPort;

    @BeforeEach
    void setUp() throws Exception {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("netty-test-boss", true));
        workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("netty-test-worker", true));
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536));
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
                                FullHttpResponse response = new DefaultFullHttpResponse(
                                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                                response.content().writeBytes("<html><body>OctoPrint 1.9.0</body></html>".getBytes());
                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                            }
                        });
                    }
                });

        // Try to bind to 5000 (common OctoPrint port supported by active probing)
        try {
            serverChannel = b.bind(5000).sync().channel();
            httpPort = 5000;
        } catch (Exception e) {
            // Fallback to random port if 5000 is occupied
            serverChannel = b.bind(0).sync().channel();
            httpPort = ((java.net.InetSocketAddress) serverChannel.localAddress()).getPort();
        }
    }

    @AfterEach
    void tearDown() {
        if (serverChannel != null) serverChannel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    @Test
    void testHttpActiveBannerGrabbing() throws Exception {
        // Only run full test if we managed to bind to a port that NettyPortScanner recognizes as HTTP
        // Since 5000 is in the list, it should work.
        if (httpPort != 5000 && httpPort != 8080 && httpPort != 8000) {
            System.out.println("[DEBUG_LOG] Skipping full NettyPortScanner test because bound to non-standard HTTP port: " + httpPort);
            return;
        }

        try (NettyPortScanner scanner = new NettyPortScanner(1000, 2000)) {
            CompletableFuture<PortScanResult> future = scanner.scanPort("127.0.0.1", httpPort, true);
            PortScanResult result = future.get(5, TimeUnit.SECONDS);

            assertTrue(result.isOpen(), "Port should be open");
            assertEquals("HTTP", result.getService());
            assertTrue(result.getServiceDetails().contains("OctoPrint"), "Banner should contain OctoPrint");
            assertTrue(result.getServiceDetails().contains("<html>"), "Banner should be HTML content");
        }
    }

    @Test
    void testActiveIdentificationAndEnrichment() throws Exception {
        if (httpPort != 5000 && httpPort != 8080 && httpPort != 8000) {
            return;
        }

        try (NettyPortScanner portScanner = new NettyPortScanner(1000, 2000)) {
            // 1. Perform active scan
            CompletableFuture<PortScanResult> future = portScanner.scanPort("127.0.0.1", httpPort, true);
            PortScanResult rawResult = future.get(5, TimeUnit.SECONDS);

            // 2. Validate using ServiceValidator (matches patterns)
            DefaultServiceValidator validator = new DefaultServiceValidator();
            
            // Setup OctoPrint profile
            PortDiscoveryConfig octoPort = new PortDiscoveryConfig(httpPort, "OctoPrint Web", Pattern.compile("OctoPrint"));
            PrinterDiscoveryProfile octoProfile = new PrinterDiscoveryProfile("OctoPrint", Set.of(octoPort));
            ScanConfiguration config = new ScanConfiguration(List.of(octoProfile), Collections.emptySet());
            
            PortScanResult validatedResult = validator.validate(httpPort, rawResult.getServiceDetails(), config);
            assertEquals("OctoPrint Web", validatedResult.getService());

            // 3. Enrich device
            DiscoveredDevice device = new DiscoveredDevice("127.0.0.1");
            device.addService(validatedResult);
            
            DeviceEnricher enricher = new DeviceEnricher();
            enricher.enrichFromPortScan(device, config);
            
            assertEquals("OctoPrint", device.getVendor());
        }
    }
}

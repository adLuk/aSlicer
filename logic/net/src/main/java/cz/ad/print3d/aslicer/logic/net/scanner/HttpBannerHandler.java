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
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.concurrent.CompletableFuture;

/**
 * HttpBannerHandler is a Netty handler that performs an active HTTP GET request
 * to capture the response body as a service banner.
 */
public class HttpBannerHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private final CompletableFuture<PortScanResult> future;
    private final int port;
    private final String host;

    /**
     * Constructs a new HttpBannerHandler.
     *
     * @param future the future to complete with the scan result
     * @param port   the port being scanned
     * @param host   the host being scanned
     */
    public HttpBannerHandler(CompletableFuture<PortScanResult> future, int port, String host) {
        this.future = future;
        this.port = port;
        this.host = host;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(request);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        String content = msg.content().toString(CharsetUtil.UTF_8);
        // We provide the content as details so it can be matched by patterns in ServiceValidator
        future.complete(new PortScanResult(port, true, "HTTP", content.trim()));
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!future.isDone()) {
            future.complete(new PortScanResult(port, true, "HTTP", "Error: " + cause.getMessage()));
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!future.isDone()) {
            future.complete(new PortScanResult(port, true, "HTTP", "Channel closed before response"));
        }
        super.channelInactive(ctx);
    }
}

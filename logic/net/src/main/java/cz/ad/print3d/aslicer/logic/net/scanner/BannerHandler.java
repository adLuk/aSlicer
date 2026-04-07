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
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.CompletableFuture;

/**
 * BannerHandler is a Netty handler that captures the service banner
 * and identifies the service using {@link ServiceIdentifier}.
 */
public class BannerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final CompletableFuture<PortScanResult> future;
    private final int port;

    /**
     * Constructs a new BannerHandler.
     *
     * @param future the future to complete with the scan result
     * @param port   the port being scanned
     */
    public BannerHandler(CompletableFuture<PortScanResult> future, int port) {
        this.future = future;
        this.port = port;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        ServiceIdentifier.ServiceInfo info = ServiceIdentifier.identify(msg);
        String sslInfo = ctx.channel().attr(SslHandshakeHandler.SSL_INFO).get();
        String details = info.getDetails();
        if (sslInfo != null) {
            details += "\n[SSL: " + sslInfo + "]";
        }
        future.complete(new PortScanResult(port, true, info.getName(), details));
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        completeWithDefault(ctx);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        completeWithDefault(ctx);
        super.channelInactive(ctx);
    }

    private void completeWithDefault(ChannelHandlerContext ctx) {
        if (!future.isDone()) {
            String sslInfo = ctx.channel().attr(SslHandshakeHandler.SSL_INFO).get();
            if (sslInfo != null) {
                future.complete(new PortScanResult(port, true, null, "[SSL: " + sslInfo + "]"));
            } else {
                future.complete(new PortScanResult(port, true));
            }
        }
    }
}

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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AttributeKey;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * SslHandshakeHandler captures information about the SSL certificate after a successful handshake.
 */
public class SslHandshakeHandler extends ChannelInboundHandlerAdapter {

    /**
     * Attribute key for storing SSL information in the channel.
     */
    public static final AttributeKey<String> SSL_INFO = AttributeKey.valueOf("SSL_INFO");

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
            if (handshakeEvent.isSuccess()) {
                SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
                if (sslHandler != null) {
                    try {
                        Certificate[] certs = sslHandler.engine().getSession().getPeerCertificates();
                        if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                            X509Certificate x509 = (X509Certificate) certs[0];
                            String info = "Subject: " + x509.getSubjectX500Principal().getName();
                            if (isSelfSigned(x509)) {
                                info += " [Self-signed]";
                            }
                            ctx.channel().attr(SSL_INFO).set(info);
                        }
                    } catch (SSLPeerUnverifiedException ignored) {
                        // Handshake succeeded but no peer certs
                    }
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    private boolean isSelfSigned(X509Certificate cert) {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }
}

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

import cz.ad.print3d.aslicer.logic.net.scanner.dto.SsdpServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NettySsdpScanner provides functionality to discover devices on the local network using SSDP (Simple Service Discovery Protocol).
 * It sends M-SEARCH queries and parses responses, including fetching and parsing UPnP device description XML files.
 */
public class NettySsdpScanner implements SsdpScanner {

    private static final Logger LOGGER = Logger.getLogger(NettySsdpScanner.class.getName());
    private static final String SSDP_IP = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    private static final int XML_FETCH_TIMEOUT_MS = 2000;

    private final EventLoopGroup group;
    private final boolean ownGroup;
    private final List<Channel> activeChannels = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new NettySsdpScanner with its own EventLoopGroup.
     */
    public NettySsdpScanner() {
        this(new NioEventLoopGroup(0, new DefaultThreadFactory("netty-ssdp-scanner", true)), true);
    }

    /**
     * Constructs a new NettySsdpScanner with a provided EventLoopGroup.
     *
     * @param group the EventLoopGroup to use
     */
    public NettySsdpScanner(EventLoopGroup group) {
        this(group, false);
    }

    private NettySsdpScanner(EventLoopGroup group, boolean ownGroup) {
        this.group = group;
        this.ownGroup = ownGroup;
    }

    @Override
    public CompletableFuture<Set<SsdpServiceInfo>> discoverDevices(long timeoutMillis, SsdpDiscoveryListener listener, NetworkInterface networkInterface) {
        CompletableFuture<Set<SsdpServiceInfo>> future = new CompletableFuture<>();
        Set<SsdpServiceInfo> discoveredServices = Collections.synchronizedSet(new HashSet<>());
        Set<String> processedUsns = Collections.synchronizedSet(new HashSet<>());

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        if (networkInterface != null) {
                            ch.config().setNetworkInterface(networkInterface);
                        }
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                                String content = packet.content().toString(CharsetUtil.UTF_8);
                                processResponse(content, packet.sender().getAddress().getHostAddress(), discoveredServices, processedUsns, listener);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                LOGGER.log(Level.WARNING, "SSDP channel exception", cause);
                                ctx.close();
                            }
                        });
                    }
                });

        ChannelFuture bindFuture = b.bind(0);
        bindFuture.addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                future.completeExceptionally(f.cause());
                return;
            }

            Channel ch = f.channel();
            activeChannels.add(ch);
            InetSocketAddress ssdpAddr = new InetSocketAddress(SSDP_IP, SSDP_PORT);

            String mSearch = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: " + SSDP_IP + ":" + SSDP_PORT + "\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: ssdp:all\r\n" +
                    "\r\n";

            DatagramPacket packet = new DatagramPacket(
                    Unpooled.copiedBuffer(mSearch, CharsetUtil.UTF_8),
                    ssdpAddr
            );
            ch.writeAndFlush(packet);

            // Wait for responses. We wait slightly longer than timeoutMillis to ensure we catch late responses
            // but we complete the future at timeoutMillis.
            group.schedule(() -> {
                activeChannels.remove(ch);
                ch.close();
                future.complete(new HashSet<>(discoveredServices));
            }, timeoutMillis + 500, TimeUnit.MILLISECONDS);
        });

        future.whenComplete((res, ex) -> {
            activeChannels.remove(bindFuture.channel());
            bindFuture.channel().close();
        });

        return future;
    }

    private void processResponse(String content, String senderIp, Set<SsdpServiceInfo> results, Set<String> processedUsns, SsdpDiscoveryListener listener) {
        Map<String, String> headers = parseHeaders(content);
        String usn = headers.get("USN");
        if (usn == null || !processedUsns.add(usn)) {
            return;
        }

        String location = headers.get("LOCATION");
        String st = headers.get("ST");

        // Use a separate thread to fetch XML to not block the event loop
        CompletableFuture.runAsync(() -> {
            SsdpServiceInfo info = null;
            if (location != null) {
                info = parseXmlDescription(location, usn, st, senderIp, headers);
            }

            if (info == null) {
                // Fallback if XML parsing failed or no location
                int port = 80;
                if (location != null) {
                    try {
                        URL url = URI.create(location).toURL();
                        port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
                    } catch (Exception ignored) {}
                }
                info = new SsdpServiceInfo(usn, st, location, senderIp, port, null, null, null, headers);
            }

            results.add(info);
            if (listener != null) {
                listener.onServiceDiscovered(info);
            }
        }, group);
    }

    private Map<String, String> parseHeaders(String content) {
        Map<String, String> headers = new HashMap<>();
        String[] lines = content.split("\r\n");
        for (String line : lines) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim().toUpperCase();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }
        return headers;
    }

    private SsdpServiceInfo parseXmlDescription(String location, String usn, String st, String senderIp, Map<String, String> headers) {
        try {
            URL url = URI.create(location).toURL();
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(XML_FETCH_TIMEOUT_MS);
            connection.setReadTimeout(XML_FETCH_TIMEOUT_MS);

            try (InputStream is = connection.getInputStream()) {
                return parseXmlDescription(is, url, usn, st, senderIp, headers);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to parse SSDP XML description from " + location, e);
            return null;
        }
    }

    SsdpServiceInfo parseXmlDescription(InputStream is, URL url, String usn, String st, String senderIp, Map<String, String> headers) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            String friendlyName = getTagValue(doc, "friendlyName");
            String manufacturer = getTagValue(doc, "manufacturer");
            String modelName = getTagValue(doc, "modelName");

            int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();

            return new SsdpServiceInfo(usn, st, url.toString(), senderIp, port, friendlyName, manufacturer, modelName, headers);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to parse SSDP XML stream", e);
            return null;
        }
    }

    private String getTagValue(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            Node node = list.item(0);
            return node.getTextContent();
        }
        return null;
    }

    @Override
    public void stopScan() {
        for (Channel ch : activeChannels) {
            ch.close();
        }
        activeChannels.clear();
    }

    @Override
    public void close() {
        if (ownGroup) {
            group.shutdownGracefully();
        }
    }
}

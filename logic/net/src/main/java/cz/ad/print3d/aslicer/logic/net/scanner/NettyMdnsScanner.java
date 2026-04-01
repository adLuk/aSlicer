package cz.ad.print3d.aslicer.logic.net.scanner;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsQueryEncoder;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DatagramDnsResponseDecoder;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.dns.DefaultDnsQuestion;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NettyMdnsScanner provides functionality to discover devices on the local network using mDNS (Multicast DNS).
 * It sends DNS queries for common services used by 3D printers and related devices.
 * 
 * <p>This scanner utilizes Netty's DNS codec to construct and parse mDNS messages.
 * It queries for common service types like _http._tcp.local. and _octoprint._tcp.local.
 * to identify potential 3D printers and management interfaces.</p>
 */
public class NettyMdnsScanner implements MdnsScanner {

    private static final Logger LOGGER = Logger.getLogger(NettyMdnsScanner.class.getName());
    private static final String MDNS_IP = "224.0.0.251";
    private static final int MDNS_PORT = 5353;
    private static final List<String> COMMON_SERVICES = List.of(
            "_http._tcp.local.",
            "_octoprint._tcp.local.",
            "_printer._tcp.local.",
            "_ssh._tcp.local.",
            "_moonraker._tcp.local.",
            "_workstation._tcp.local."
    );

    private final EventLoopGroup group;
    private final boolean ownGroup;

    /**
     * Constructs a new NettyMdnsScanner with its own EventLoopGroup.
     */
    public NettyMdnsScanner() {
        this(new NioEventLoopGroup(1), true);
    }

    /**
     * Constructs a new NettyMdnsScanner with a provided EventLoopGroup.
     *
     * @param group the EventLoopGroup to use
     */
    public NettyMdnsScanner(EventLoopGroup group) {
        this(group, false);
    }

    private NettyMdnsScanner(EventLoopGroup group, boolean ownGroup) {
        this.group = group;
        this.ownGroup = ownGroup;
    }

    @Override
    public CompletableFuture<Set<String>> discoverDevices(long timeoutMillis) {
        CompletableFuture<Set<String>> future = new CompletableFuture<>();
        Set<String> discoveredIps = Collections.synchronizedSet(new HashSet<>());

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ch.pipeline().addLast(new DatagramDnsQueryEncoder());
                        ch.pipeline().addLast(new DatagramDnsResponseDecoder());
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramDnsResponse>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsResponse msg) {
                                String senderIp = msg.sender().getAddress().getHostAddress();
                                discoveredIps.add(senderIp);
                                LOGGER.log(Level.FINE, "mDNS response received from: {0}", senderIp);
                            }
                        });
                    }
                });

        b.bind(0).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture bindFuture) {
                if (!bindFuture.isSuccess()) {
                    future.completeExceptionally(bindFuture.cause());
                    return;
                }

                Channel ch = bindFuture.channel();
                InetSocketAddress mDnsAddr = new InetSocketAddress(MDNS_IP, MDNS_PORT);

                for (String service : COMMON_SERVICES) {
                    DatagramDnsQuery query = new DatagramDnsQuery(null, mDnsAddr, 0);
                    query.addRecord(DnsSection.QUESTION, new DefaultDnsQuestion(service, DnsRecordType.PTR));
                    ch.writeAndFlush(query);
                }

                group.schedule(() -> {
                    ch.close();
                    future.complete(new HashSet<>(discoveredIps));
                }, timeoutMillis, TimeUnit.MILLISECONDS);
            }
        });

        return future;
    }

    @Override
    public void close() {
        if (ownGroup) {
            group.shutdownGracefully();
        }
    }
}

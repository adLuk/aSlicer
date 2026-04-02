package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.*;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NettyMdnsScanner provides functionality to discover devices on the local network using mDNS (Multicast DNS).
 * It sends DNS queries for common services used by 3D printers and related devices.
 * 
 * <p>This scanner utilizes Netty's DNS codec to construct and parse mDNS messages.
 * It queries for common service types like {@code _http._tcp.local.}, {@code _octoprint._tcp.local.},
 * and {@code _printer._tcp.local.} to identify potential 3D printers and management interfaces.</p>
 * 
 * <p>Discovery Process:
 * <ol>
 *   <li>Binds a UDP channel to an ephemeral port.</li>
 *   <li>Sends PTR queries for common service types to the mDNS multicast address (224.0.0.251:5353).</li>
 *   <li>Listens for responses for a specified timeout period.</li>
 *   <li>Parses {@code ANSWER} and {@code ADDITIONAL} sections of mDNS responses.</li>
 *   <li>Correlates PTR, SRV, TXT, and A/AAAA records to build comprehensive {@link MdnsServiceInfo} objects.</li>
 * </ol>
 * </p>
 * 
 * <p>Record Parsing Details:
 * <ul>
 *   <li>{@code PTR}: Links a service type to a specific service instance name.</li>
 *   <li>{@code SRV}: Provides the port number and target hostname for a service instance.</li>
 *   <li>{@code TXT}: Contains key-value pairs with metadata like manufacturer (mfg) and model (mdl).</li>
 *   <li>{@code A/AAAA}: Resolves a hostname to an IP address.</li>
 * </ul>
 * The scanner also uses the sender's IP address as a fallback if no address records are present in the response.
 * </p>
 */
public class NettyMdnsScanner implements MdnsScanner {

    private static final Logger LOGGER = Logger.getLogger(NettyMdnsScanner.class.getName());
    private static final String MDNS_IP = "224.0.0.251";
    private static final int MDNS_PORT = 5353;
    private static final List<String> COMMON_SERVICES = List.of(
            "_http._tcp.local.",
            "_octoprint._tcp.local.",
            "_printer._tcp.local.",
            "_ipp._tcp.local.",
            "_ipps._tcp.local.",
            "_ssh._tcp.local.",
            "_moonraker._tcp.local.",
            "_workstation._tcp.local.",
            "_bambu-network._tcp.local.",
            "_bambulab._tcp.local."
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
    public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis) {
        return discoverDevices(timeoutMillis, null);
    }

    @Override
    public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener) {
        return discoverDevices(timeoutMillis, listener, null);
    }

    @Override
    public CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, NetworkInterface networkInterface) {
        CompletableFuture<Set<MdnsServiceInfo>> future = new CompletableFuture<>();
        Map<String, ServiceBuilder> sessionBuilders = new HashMap<>();
        Map<String, String> sessionHostnameToIp = new HashMap<>();
        Set<MdnsServiceInfo> discoveredServices = Collections.synchronizedSet(new HashSet<>());

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        if (networkInterface != null) {
                            ch.config().setNetworkInterface(networkInterface);
                        }
                        ch.pipeline().addLast(new DatagramDnsQueryEncoder());
                        ch.pipeline().addLast(new DatagramDnsResponseDecoder());
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramDnsResponse>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsResponse msg) {
                                try {
                                    processResponse(msg, sessionBuilders, sessionHostnameToIp, discoveredServices, listener);
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Error processing mDNS response", e);
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                LOGGER.log(Level.WARNING, "mDNS channel exception", cause);
                                ctx.close();
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
                    synchronized (discoveredServices) {
                        future.complete(new HashSet<>(discoveredServices));
                    }
                }, timeoutMillis, TimeUnit.MILLISECONDS);
            }
        });

        return future;
    }

    private void processResponse(DatagramDnsResponse msg, Map<String, ServiceBuilder> builders, Map<String, String> hostnameToIp, Set<MdnsServiceInfo> results, MdnsDiscoveryListener listener) {
        String senderIp = msg.sender().getAddress().getHostAddress();

        // Process all records in ANSWER, AUTHORITY and ADDITIONAL sections
        synchronized (builders) {
            for (DnsSection section : new DnsSection[]{DnsSection.ANSWER, DnsSection.AUTHORITY, DnsSection.ADDITIONAL}) {
                for (int i = 0; i < msg.count(section); i++) {
                    DnsRecord record = msg.recordAt(section, i);
                    String name = record.name();

                    if (record.type() == DnsRecordType.PTR && record instanceof DnsPtrRecord) {
                        String serviceInstanceName = ((DnsPtrRecord) record).hostname();
                        ServiceBuilder builder = builders.computeIfAbsent(serviceInstanceName, k -> new ServiceBuilder());
                        builder.type = name;
                        builder.instanceName = serviceInstanceName;
                    } else if (record.type() == DnsRecordType.SRV) {
                        ServiceBuilder builder = builders.computeIfAbsent(name, k -> new ServiceBuilder());
                        if (builder.instanceName == null) builder.instanceName = name;
                        if (builder.type == null) builder.type = inferTypeFromName(name);
                        parseSrvRecord(record, builder);
                    } else if (record.type() == DnsRecordType.TXT) {
                        ServiceBuilder builder = builders.computeIfAbsent(name, k -> new ServiceBuilder());
                        if (builder.instanceName == null) builder.instanceName = name;
                        if (builder.type == null) builder.type = inferTypeFromName(name);
                        parseTxtRecord(record, builder);
                    } else if (record.type() == DnsRecordType.A || record.type() == DnsRecordType.AAAA) {
                        parseAddressRecord(record, hostnameToIp);
                    }
                }
            }

            // Build/Update results
            for (ServiceBuilder builder : builders.values()) {
                if (builder.instanceName == null) continue;

                String ip = builder.ipAddress;
                if (ip == null && builder.hostname != null) {
                    ip = hostnameToIp.get(builder.hostname);
                }
                if (ip == null) {
                    ip = senderIp; // Fallback to sender IP
                }

                String simpleName = extractSimpleName(builder.instanceName);
                MdnsServiceInfo info = new MdnsServiceInfo(
                        simpleName,
                        builder.type,
                        ip,
                        builder.port,
                        builder.hostname,
                        builder.attributes
                );

                // Check if we already have this info and if it's an update
                boolean isNewOrUpdated = true;
                synchronized (results) {
                    // Try to find if we already have a service with this name and type
                    MdnsServiceInfo existing = results.stream()
                            .filter(s -> s.getName().equals(info.getName()) && Objects.equals(s.getType(), info.getType()))
                            .findFirst()
                            .orElse(null);

                    if (existing != null) {
                        if (existing.equals(info)) {
                            isNewOrUpdated = false;
                        } else {
                            results.remove(existing);
                        }
                    }

                    if (isNewOrUpdated) {
                        results.add(info);
                    }
                }

                if (isNewOrUpdated) {
                    if (listener != null) {
                        listener.onServiceDiscovered(info);
                    }
                }
                LOGGER.log(Level.FINE, "Discovered mDNS service: {0} at {1}:{2}", new Object[]{simpleName, ip, builder.port});
            }
        }
    }

    private void parseSrvRecord(DnsRecord record, ServiceBuilder builder) {
        if (record instanceof DnsRawRecord) {
            ByteBuf content = ((DnsRawRecord) record).content();
            if (content.readableBytes() >= 6) {
                content.markReaderIndex();
                content.readShort(); // priority
                content.readShort(); // weight
                builder.port = content.readUnsignedShort();
                // Target is harder to parse due to name compression if we only have the raw content
                // But often it's just the next part of the packet.
                // However, Netty's DnsRawRecord only contains the RDATA.
                content.resetReaderIndex();
            }
        }
        // If Netty decoded it to a higher level record in the future, we could handle it here
    }

    private void parseTxtRecord(DnsRecord record, ServiceBuilder builder) {
        if (record instanceof DnsRawRecord) {
            ByteBuf content = ((DnsRawRecord) record).content();
            content.markReaderIndex();
            while (content.isReadable()) {
                short length = content.readUnsignedByte();
                if (length == 0) break;
                if (content.readableBytes() < length) break;
                
                String attr = content.readSlice(length).toString(CharsetUtil.UTF_8);
                int idx = attr.indexOf('=');
                if (idx > 0) {
                    builder.attributes.put(attr.substring(0, idx), attr.substring(idx + 1));
                } else {
                    builder.attributes.put(attr, "");
                }
            }
            content.resetReaderIndex();
        }
    }

    private void parseAddressRecord(DnsRecord record, Map<String, String> hostnameToIp) {
        if (record instanceof DnsRawRecord) {
            ByteBuf content = ((DnsRawRecord) record).content();
            if (content.readableBytes() == 4) { // IPv4
                byte[] addr = new byte[4];
                content.getBytes(content.readerIndex(), addr);
                try {
                    hostnameToIp.put(record.name(), InetAddress.getByAddress(addr).getHostAddress());
                } catch (UnknownHostException ignored) {}
            } else if (content.readableBytes() == 16) { // IPv6
                byte[] addr = new byte[16];
                content.getBytes(content.readerIndex(), addr);
                try {
                    hostnameToIp.put(record.name(), InetAddress.getByAddress(addr).getHostAddress());
                } catch (UnknownHostException ignored) {}
            }
        }
    }

    private String extractSimpleName(String serviceInstanceName) {
        int firstDot = serviceInstanceName.indexOf('.');
        if (firstDot > 0) {
            return serviceInstanceName.substring(0, firstDot);
        }
        return serviceInstanceName;
    }

    private String inferTypeFromName(String name) {
        int firstDot = name.indexOf('.');
        if (firstDot > 0 && firstDot < name.length() - 1) {
            return name.substring(firstDot + 1);
        }
        return null;
    }

    private static class ServiceBuilder {
        String instanceName;
        String type;
        String hostname;
        String ipAddress;
        int port;
        Map<String, String> attributes = new HashMap<>();
    }

    @Override
    public void close() {
        if (ownGroup) {
            group.shutdownGracefully();
        }
    }
}

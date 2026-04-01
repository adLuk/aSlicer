package cz.ad.print3d.aslicer.logic.net.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Utility class for IP address operations.
 */
public final class IpUtils {

    private IpUtils() {
        // Private constructor to prevent instantiation.
    }

    /**
     * IP range information.
     */
    public static class IpRange {
        private final String startIp;
        private final String endIp;
        private final String baseIp;
        private final int startHost;
        private final int endHost;

        public IpRange(String startIp, String endIp, String baseIp, int startHost, int endHost) {
            this.startIp = startIp;
            this.endIp = endIp;
            this.baseIp = baseIp;
            this.startHost = startHost;
            this.endHost = endHost;
        }

        public String getStartIp() {
            return startIp;
        }

        public String getEndIp() {
            return endIp;
        }

        public String getBaseIp() {
            return baseIp;
        }

        public int getStartHost() {
            return startHost;
        }

        public int getEndHost() {
            return endHost;
        }

        @Override
        public String toString() {
            return "IpRange{" +
                    "startIp='" + startIp + '\'' +
                    ", endIp='" + endIp + '\'' +
                    '}';
        }
    }

    /**
     * Calculates the IP range for a given IP address and prefix length.
     * Only supports IPv4.
     *
     * @param ipAddress    the IP address string
     * @param prefixLength the network prefix length
     * @return the calculated {@link IpRange}
     * @throws UnknownHostException     if the IP address is invalid
     * @throws IllegalArgumentException if it's not an IPv4 address
     */
    public static IpRange calculateIpRange(String ipAddress, int prefixLength) throws UnknownHostException {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Prefix length must be between 0 and 32");
        }
        InetAddress address = InetAddress.getByName(ipAddress);
        byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Only IPv4 is supported for range calculation");
        }

        int ip = ByteBuffer.wrap(bytes).getInt();
        int mask = (prefixLength == 0) ? 0 : (0xFFFFFFFF << (32 - prefixLength));
        int network = ip & mask;
        int broadcast = network | ~mask;

        // Ensure we stay within standard host limits (1-254) for the baseIp calculation
        // and avoid network and broadcast addresses.
        int firstHost = network + 1;
        int lastHost = broadcast - 1;

        String startIp = intToIp(firstHost);
        String endIp = intToIp(lastHost);

        // For simplicity in the scanner, we extract the first 3 octets if it's a /24
        // If not a /24, it becomes more complex for the current scanner.
        // We'll provide a baseIp that matches the scanner's expected format if possible.
        String baseIp = (firstHost >> 24 & 0xFF) + "." + (firstHost >> 16 & 0xFF) + "." + (firstHost >> 8 & 0xFF) + ".";
        int startH = firstHost & 0xFF;
        int endH = lastHost & 0xFF;

        // If the range spans across multiple /24 blocks, our current scanner might not handle it well
        // as it expects a fixed baseIp. But for most home/office networks, it will be /24.
        return new IpRange(startIp, endIp, baseIp, startH, endH);
    }

    /**
     * Converts an integer IP representation to string format.
     *
     * @param i the integer representation
     * @return the dotted-decimal string representation
     */
    public static String intToIp(int i) {
        return (i >> 24 & 0xFF) + "." + (i >> 16 & 0xFF) + "." + (i >> 8 & 0xFF) + "." + (i & 0xFF);
    }

    /**
     * Parses an IPv4 address from its dotted-decimal string representation.
     *
     * @param ip the dotted-decimal string
     * @return the host part (last octet)
     */
    public static int getHostPart(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return 0;
        return Integer.parseInt(parts[3]);
    }

    /**
     * Extracts the base IP (first 3 octets) from an IPv4 address.
     *
     * @param ip the dotted-decimal string
     * @return the base IP with a trailing dot
     */
    public static String getBaseIp(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return "";
        return parts[0] + "." + parts[1] + "." + parts[2] + ".";
    }
}

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ServiceIdentifier provides logic to identify network services based on their initial response (banner).
 * It can handle both text-based and binary-based responses.
 */
public class ServiceIdentifier {

    /**
     * Regex pattern to identify SSH banner.
     * Example: SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.1
     */
    private static final Pattern SSH_PATTERN = Pattern.compile("^SSH-([0-9.]+)-(.*)");

    /**
     * Regex pattern to identify HTTP response status line.
     * Example: HTTP/1.1 200 OK
     */
    private static final Pattern HTTP_PATTERN = Pattern.compile("^HTTP/([0-9.]+)\\s+([0-9]{3})\\s+(.*)");

    /**
     * Regex pattern to identify generic 3-digit text greetings (like FTP or SMTP).
     * Example: 220 ProFTPD Server
     */
    private static final Pattern GENERIC_TEXT_GREETING = Pattern.compile("^([0-9]{3})\\s+(.*)");

    /**
     * Data class to hold identified service information.
     * Contains the name of the identified service and any additional details
     * parsed from the initial communication banner.
     */
    public static class ServiceInfo {
        /**
         * The human-readable name of the identified service (e.g., "SSH", "HTTP").
         */
        private final String name;

        /**
         * Additional details about the service (e.g., version strings, server info).
         */
        private final String details;

        /**
         * Constructs a new ServiceInfo.
         *
         * @param name    the identified service name
         * @param details details parsed from the response
         */
        public ServiceInfo(String name, String details) {
            this.name = name;
            this.details = details;
        }

        /**
         * Returns the identified service name.
         *
         * @return the service name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns details parsed from the response.
         *
         * @return the service details
         */
        public String getDetails() {
            return details;
        }
    }

    /**
     * Identifies a service from the provided ByteBuf message.
     *
     * @param msg the buffer containing the service response
     * @return ServiceInfo containing identified service name and details
     */
    public static ServiceInfo identify(ByteBuf msg) {
        if (msg == null || msg.readableBytes() == 0) {
            return new ServiceInfo("Unknown", "No data received");
        }

        // Check if it's likely binary data before attempting to convert to string
        if (isBinary(msg)) {
            return identifyBinary(msg);
        }

        // Try to read as UTF-8 string
        String content = msg.toString(StandardCharsets.UTF_8).trim();

        // Check for SSH
        Matcher sshMatcher = SSH_PATTERN.matcher(content);
        if (sshMatcher.find()) {
            return new ServiceInfo("SSH", "Version: " + sshMatcher.group(1) + ", Implementation: " + sshMatcher.group(2));
        }

        // Check for HTTP
        Matcher httpMatcher = HTTP_PATTERN.matcher(content);
        if (httpMatcher.find()) {
            return new ServiceInfo("HTTP", "Version: " + httpMatcher.group(1) + ", Status: " + httpMatcher.group(2) + " " + httpMatcher.group(3));
        }

        // Check for generic 3-digit greetings (FTP, SMTP)
        Matcher greetingMatcher = GENERIC_TEXT_GREETING.matcher(content);
        if (greetingMatcher.find()) {
            String code = greetingMatcher.group(1);
            String text = greetingMatcher.group(2);
            if (code.equals("220")) {
                return new ServiceInfo("FTP/SMTP", text);
            }
            return new ServiceInfo("Generic Greeting", "Code: " + code + ", Text: " + text);
        }

        // Default to text banner
        if (content.isEmpty()) {
            return new ServiceInfo("Unknown", "Non-printable data");
        }
        return new ServiceInfo("Text Banner", content);
    }

    /**
     * Heuristically determines if the provided buffer contains binary data.
     * It checks the first 100 bytes for non-printable characters that are not
     * common whitespace (tab, newline, carriage return).
     *
     * @param msg the buffer to analyze
     * @return true if the buffer likely contains binary data, false otherwise
     */
    private static boolean isBinary(ByteBuf msg) {
        int length = Math.min(msg.readableBytes(), 100);
        for (int i = 0; i < length; i++) {
            byte b = msg.getByte(msg.readerIndex() + i);
            // Non-printable characters (excluding common whitespace) suggest binary data
            if (b < 0x20 && b != 0x09 && b != 0x0A && b != 0x0D) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to identify a service based on known binary protocol patterns.
     * Currently supports MySQL handshake and GIOP (CORBA) headers.
     * If no pattern matches, returns a hexadecimal representation of the first 16 bytes.
     *
     * @param msg the binary buffer to analyze
     * @return ServiceInfo containing identified service name and details
     */
    private static ServiceInfo identifyBinary(ByteBuf msg) {
        int readable = msg.readableBytes();
        
        // Simple binary patterns
        
        // MySQL Handshake: length (3 bytes), seq (1 byte), protocol version (1 byte)
        // Protocol version is usually 10 (0x0A)
        if (readable >= 5 && msg.getByte(msg.readerIndex() + 3) == 0x00 && msg.getByte(msg.readerIndex() + 4) == 0x0A) {
             return new ServiceInfo("MySQL", "Protocol Version: 10");
        }
        
        // GIOP (CORBA)
        if (readable >= 4 && msg.getByte(msg.readerIndex()) == 'G' && msg.getByte(msg.readerIndex() + 1) == 'I' 
                && msg.getByte(msg.readerIndex() + 2) == 'O' && msg.getByte(msg.readerIndex() + 3) == 'P') {
            String version = (readable >= 6) ? (msg.getByte(msg.readerIndex() + 4) + "." + msg.getByte(msg.readerIndex() + 5)) : "unknown";
            return new ServiceInfo("CORBA/GIOP", "Version " + version);
        }

        // Fallback for binary: Hex dump of first 16 bytes
        int dumpLen = Math.min(readable, 16);
        String hex = ByteBufUtil.hexDump(msg, msg.readerIndex(), dumpLen);
        return new ServiceInfo("Binary Data", "Hex: " + hex + (readable > 16 ? "..." : ""));
    }
}

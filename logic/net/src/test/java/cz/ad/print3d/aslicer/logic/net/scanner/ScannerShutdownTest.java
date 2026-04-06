package cz.ad.print3d.aslicer.logic.net.scanner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScannerShutdownTest {

    @Test
    public void testNettyScannersUseDaemonThreads() throws Exception {
        // Create scanners, they should initialize their EventLoopGroups with daemon threads
        NettyPortScanner portScanner = new NettyPortScanner(100, 100);
        NettyMdnsScanner mdnsScanner = new NettyMdnsScanner();
        NettySsdpScanner ssdpScanner = new NettySsdpScanner();

        // Check if there are any non-daemon Netty threads
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int count = Thread.enumerate(threads);
        
        for (int i = 0; i < count; i++) {
            Thread t = threads[i];
            if (t.getName().contains("netty") || t.getName().contains("nioEventLoopGroup")) {
                assertTrue(t.isDaemon(), "Netty thread " + t.getName() + " should be a daemon thread");
            }
        }

        portScanner.close();
        mdnsScanner.close();
        ssdpScanner.close();
    }
}

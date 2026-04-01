package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.NetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.PortScanResult;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for {@link PrinterDiscoveryDialog}.
 * <p>These tests use libGDX HeadlessApplication to simulate the UI environment
 * and verify that the dialog components are correctly initialized, updated,
 * and interact with the scanning logic.</p>
 */
public class PrinterDiscoveryDialogTest {

    @Test
    void testDialogComponents() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    
                    NetworkScanner mockScanner = new NetworkScanner() {
                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            if (listener != null) {
                                listener.onProgress(0.5, "192.168.1.1");
                            }
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
                            return scanHost(host, ports, useBannerGrabbing, null);
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            if (listener != null) {
                                listener.onProgress(1.0, host);
                            }
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public void setTimeout(int timeoutMillis) {}

                        @Override
                        public int getTimeout() { return 500; }

                        @Override
                        public void setIncludeSelfIp(boolean include) {}

                        @Override
                        public boolean isIncludeSelfIp() { return false; }

                        @Override
                        public void stopScan() {
                        }

                        @Override
                        public void close() {}
                    };
                    
                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override
                        public List<NetworkInterfaceInfo> collect() {
                            return Collections.emptyList();
                        }

                        @Override
                        public synchronized CompletableFuture<List<NetworkInterfaceInfo>> collectAsync() {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    dialogRef.set(dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        PrinterDiscoveryDialog dialog = dialogRef.get();
        assertNotNull(dialog, "Dialog should not be null");

        boolean foundCheckBox = false;
        for (Actor actor : dialog.getChildren()) {
            if (actor instanceof Table) {
                foundCheckBox = findCheckBoxInTable((Table) actor);
                if (foundCheckBox) break;
            }
        }
        assertTrue(foundCheckBox, "Deep Scan checkbox should be found in the dialog");

        boolean foundIncludeSelfIp = false;
        for (Actor actor : dialog.getChildren()) {
            if (actor instanceof Table) {
                foundIncludeSelfIp = findIncludeSelfIpCheckBoxInTable((Table) actor);
                if (foundIncludeSelfIp) break;
            }
        }
        assertTrue(foundIncludeSelfIp, "Include self IP checkbox should be found in the dialog");
    }

    private boolean findIncludeSelfIpCheckBoxInTable(Table table) {
        for (Actor child : table.getChildren()) {
            if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                if (cb.getText().toString().contains("Include self IP")) {
                    return true;
                }
            } else if (child instanceof Table) {
                if (findIncludeSelfIpCheckBoxInTable((Table) child)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void testDeepScanCheckboxModeChange() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();
        AtomicReference<List<Integer>> capturedPorts = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    
                    NetworkScanner mockScanner = new NetworkScanner() {
                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports) {
                            capturedPorts.set(ports);
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing) {
                            capturedPorts.set(ports);
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            capturedPorts.set(ports);
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
                            return scanHost(host, ports, useBannerGrabbing, null);
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            if (listener != null) {
                                listener.onProgress(1.0, host);
                            }
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public void setTimeout(int timeoutMillis) {}

                        @Override
                        public int getTimeout() { return 500; }

                        @Override
                        public void setIncludeSelfIp(boolean include) {}

                        @Override
                        public boolean isIncludeSelfIp() { return false; }

                        @Override
                        public void stopScan() {
                        }

                        @Override
                        public void close() {}
                    };
                    
                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override
                        public List<NetworkInterfaceInfo> collect() {
                            return Collections.emptyList();
                        }

                        @Override
                        public synchronized CompletableFuture<List<NetworkInterfaceInfo>> collectAsync() {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    dialog.startIpField.setText("192.168.1.1");
                    dialog.endIpField.setText("192.168.1.1");
                    
                    // Test 1: Default scan (should be normal scan)
                    dialog.deepScanCheckBox.setChecked(false);
                    // We need to call startScan() via reflection or just use the fact it's private and we are in same package?
                    // Oh, it's private. Let's make it package-private too.
                    
                    dialogRef.set(dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        PrinterDiscoveryDialog dialog = dialogRef.get();
        assertNotNull(dialog, "Dialog should not be null");

        // Test 1: Normal scan (checkbox unchecked)
        Gdx.app.postRunnable(() -> {
            dialog.deepScanCheckBox.setChecked(false);
            dialog.startScan();
        });
        
        // Wait a bit for processing
        Thread.sleep(200);
        assertNotNull(capturedPorts.get(), "Ports should be captured for normal scan");
        assertTrue(capturedPorts.get().size() < 10, "Normal scan should have few ports");
        assertTrue(capturedPorts.get().contains(80), "Normal scan should include port 80");

        // Test 2: Deep scan (checkbox checked)
        capturedPorts.set(null);
        Gdx.app.postRunnable(() -> {
            dialog.deepScanCheckBox.setChecked(true);
            dialog.startScan();
        });
        
        Thread.sleep(500); // Port generation for deep scan can take a bit longer
        assertNotNull(capturedPorts.get(), "Ports should be captured for deep scan");
        assertTrue(capturedPorts.get().size() > 65000, "Deep scan should have all ports");
        assertTrue(capturedPorts.get().contains(1), "Deep scan should include port 1");
        assertTrue(capturedPorts.get().contains(65535), "Deep scan should include port 65535");
    }

    @Test
    void testRepetitiveSearchAndCancellation() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();
        AtomicReference<String> capturedBaseIp = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    NetworkScanner mockScanner = new NetworkScanner() {
                        private CompletableFuture<List<DiscoveredDevice>> currentFuture;

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports) {
                            return scanRange(baseIp, startHost, endHost, ports, false);
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing) {
                            return scanRange(baseIp, startHost, endHost, ports, useBannerGrabbing, null);
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            capturedBaseIp.set(baseIp);
                            currentFuture = new CompletableFuture<>();
                            return currentFuture;
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
                            return scanHost(host, ports, useBannerGrabbing, null);
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            if (listener != null) {
                                listener.onProgress(1.0, host);
                            }
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public void setTimeout(int timeoutMillis) {}

                        @Override
                        public int getTimeout() { return 500; }

                        @Override
                        public void setIncludeSelfIp(boolean include) {}

                        @Override
                        public boolean isIncludeSelfIp() { return false; }

                        @Override
                        public void stopScan() {
                            if (currentFuture != null) {
                                currentFuture.cancel(true);
                            }
                        }

                        @Override
                        public void close() {
                        }
                    };

                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override
                        public List<NetworkInterfaceInfo> collect() {
                            return Collections.emptyList();
                        }

                        @Override
                        public synchronized CompletableFuture<List<NetworkInterfaceInfo>> collectAsync() {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    dialogRef.set(dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        PrinterDiscoveryDialog dialog = dialogRef.get();

        // 1. Test search with specific IP
        Gdx.app.postRunnable(() -> {
            dialog.startIpField.setText("10.0.0.1");
            dialog.endIpField.setText("10.0.0.10");
            dialog.startScan();
        });
        Thread.sleep(200);
        assertEquals("10.0.0.", capturedBaseIp.get());

        // 2. Test cancellation
        Gdx.app.postRunnable(() -> dialog.stopScan());
        Thread.sleep(200);
        // After cancellation, it should be possible to start again
        
        // 3. Test repetitive search with different IP
        capturedBaseIp.set(null);
        Gdx.app.postRunnable(() -> {
            dialog.startIpField.setText("192.168.2.1");
            dialog.endIpField.setText("192.168.2.5");
            dialog.startScan();
        });
        Thread.sleep(200);
        assertEquals("192.168.2.", capturedBaseIp.get(), "Search should use updated IP range");
    }

    private boolean findCheckBoxInTable(Table table) {
        for (Actor child : table.getChildren()) {
            if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                if (cb.getText().toString().contains("Deep Scan")) {
                    return true;
                }
            } else if (child instanceof Table) {
                if (findCheckBoxInTable((Table) child)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to find a button by its text.
     */
    private TextButton findButton(Table table, String text) {
        for (Actor actor : table.getChildren()) {
            if (actor instanceof TextButton) {
                TextButton button = (TextButton) actor;
                if (button.getText().toString().equals(text)) {
                    return button;
                }
            } else if (actor instanceof Table) {
                TextButton found = findButton((Table) actor, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    void testRealTimeResultsAndScrolling() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();
        AtomicReference<NetworkScanner.ScanProgressListener> capturedListener = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    NetworkScanner mockScanner = new NetworkScanner() {
                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            capturedListener.set(listener);
                            return new CompletableFuture<>(); // Never completes automatically
                        }
                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String b, int s, int e, List<Integer> p) { return null; }
                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String b, int s, int e, List<Integer> p, boolean u) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p, boolean u) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p, boolean u, ScanProgressListener l) { return null; }
                        @Override public void setTimeout(int timeoutMillis) {}
                        @Override public int getTimeout() { return 500; }
                        @Override public void setIncludeSelfIp(boolean include) {}
                        @Override public boolean isIncludeSelfIp() { return false; }
                        @Override public void stopScan() {}
                        @Override public void close() {}
                    };

                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override public List<NetworkInterfaceInfo> collect() { return Collections.emptyList(); }
                        @Override public synchronized CompletableFuture<List<NetworkInterfaceInfo>> collectAsync() { return CompletableFuture.completedFuture(Collections.emptyList()); }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    dialog.startIpField.setText("192.168.1.1");
                    dialog.endIpField.setText("192.168.1.10");
                    dialogRef.set(dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Setup timed out");
        PrinterDiscoveryDialog dialog = dialogRef.get();

        // Start scan
        Gdx.app.postRunnable(() -> dialog.startScan());
        Thread.sleep(500);

        assertNotNull(capturedListener.get(), "Listener should be captured");

        // Simulate discovery of first device
        DiscoveredDevice device1 = new DiscoveredDevice("192.168.1.5");
        Gdx.app.postRunnable(() -> capturedListener.get().onDeviceDiscovered(device1));
        Thread.sleep(200);

        // Check if device1 is in resultsTable
        assertTrue(hasDevice(dialog.resultsTable, "192.168.1.5"), "First device should be displayed immediately");

        // Simulate discovery of second device
        DiscoveredDevice device2 = new DiscoveredDevice("192.168.1.7");
        Gdx.app.postRunnable(() -> capturedListener.get().onDeviceDiscovered(device2));
        Thread.sleep(200);

        assertTrue(hasDevice(dialog.resultsTable, "192.168.1.5"), "First device should still be there");
        assertTrue(hasDevice(dialog.resultsTable, "192.168.1.7"), "Second device should be appended");
        
        // Check if resultsTable is inside a ScrollPane
        boolean foundScrollPane = false;
        for (Actor actor : dialog.getChildren()) {
            if (actor instanceof Table) {
                foundScrollPane = hasScrollPaneInTable((Table) actor, dialog.resultsTable);
                if (foundScrollPane) break;
            }
        }
        assertTrue(foundScrollPane, "Results should be inside a ScrollPane");

        Gdx.app.exit();
    }

    private boolean hasDevice(Table resultsTable, String ip) {
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof Table && ip.equals(actor.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasScrollPaneInTable(Table table, Table targetActor) {
        for (Actor actor : table.getChildren()) {
            if (actor instanceof ScrollPane) {
                ScrollPane sp = (ScrollPane) actor;
                if (sp.getActor() == targetActor) return true;
            } else if (actor instanceof Table) {
                if (hasScrollPaneInTable((Table) actor, targetActor)) return true;
            }
        }
        return false;
    }

    @Test
    void testDialogReopenVisibilityAndScannerLifecycle() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicBoolean scannerClosed = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    Stage stage = new Stage(new ScreenViewport());
                    stageRef.set(stage);

                    NetworkScanner mockScanner = new NetworkScanner() {
                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
                            return scanHost(host, ports, useBannerGrabbing, null);
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public void setTimeout(int timeoutMillis) {}

                        @Override
                        public int getTimeout() { return 500; }

                        @Override
                        public void setIncludeSelfIp(boolean include) {}

                        @Override
                        public boolean isIncludeSelfIp() { return false; }

                        @Override
                        public void stopScan() {
                        }

                        @Override
                        public void close() {
                            scannerClosed.set(true);
                        }
                    };

                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override
                        public List<NetworkInterfaceInfo> collect() {
                            return Collections.emptyList();
                        }

                        @Override
                        public synchronized CompletableFuture<List<NetworkInterfaceInfo>> collectAsync() {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    stage.addActor(dialog);
                    dialogRef.set(dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test setup timed out");
        PrinterDiscoveryDialog dialog = dialogRef.get();
        assertNotNull(dialog);
        assertTrue(dialog.isVisible());

        // Close via button
        CountDownLatch closeLatch = new CountDownLatch(1);
        Gdx.app.postRunnable(() -> {
            TextButton closeButton = findButton(dialog, "Close");
            if (closeButton != null) {
                closeButton.toggle();
            }
            closeLatch.countDown();
        });
        assertTrue(closeLatch.await(2, TimeUnit.SECONDS), "Close action timed out");
        
        // Should be hidden but NOT removed and NOT closed
        assertNotNull(dialog.getStage(), "Dialog should still be on stage after closing");
        assertFalse(dialog.isVisible(), "Dialog should be hidden after closing");
        assertFalse(scannerClosed.get(), "Scanner should NOT be closed after hiding dialog");

        // Toggle visibility back on (simulate DesktopUI)
        CountDownLatch reopenLatch = new CountDownLatch(1);
        Gdx.app.postRunnable(() -> {
            dialog.setVisible(!dialog.isVisible());
            reopenLatch.countDown();
        });
        assertTrue(reopenLatch.await(2, TimeUnit.SECONDS), "Reopen action timed out");
        
        assertTrue(dialog.isVisible(), "Dialog should be visible again");
        assertNotNull(dialog.getStage(), "Dialog should still be on stage");
        assertFalse(scannerClosed.get(), "Scanner should still be functional");

        // Now remove it truly (simulate disposal)
        CountDownLatch removeLatch = new CountDownLatch(1);
        Gdx.app.postRunnable(() -> {
            dialog.remove();
            removeLatch.countDown();
        });
        assertTrue(removeLatch.await(2, TimeUnit.SECONDS), "Remove action timed out");
        
        assertNull(dialog.getStage(), "Dialog should be removed from stage");
        assertTrue(scannerClosed.get(), "Scanner should be closed after true removal from stage");

        Gdx.app.exit();
    }
    @Test
    void testMdnsDetailsButton() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();
        AtomicReference<NetworkScanner.ScanProgressListener> capturedListener = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();

                    NetworkScanner mockScanner = new NetworkScanner() {
                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            capturedListener.set(listener);
                            return new CompletableFuture<>();
                        }
                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String b, int s, int e, List<Integer> p) { return null; }
                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String b, int s, int e, List<Integer> p, boolean u) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p, boolean u) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p, boolean u, ScanProgressListener l) { return null; }
                        @Override public void setTimeout(int timeoutMillis) {}
                        @Override public int getTimeout() { return 500; }
                        @Override public void setIncludeSelfIp(boolean include) {}
                        @Override public boolean isIncludeSelfIp() { return false; }
                        @Override public void stopScan() {}
                        @Override public void close() {}
                    };

                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override public List<NetworkInterfaceInfo> collect() { return Collections.emptyList(); }
                        @Override public synchronized CompletableFuture<List<NetworkInterfaceInfo>> collectAsync() { return CompletableFuture.completedFuture(Collections.emptyList()); }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    dialog.startIpField.setText("192.168.1.1");
                    dialog.endIpField.setText("192.168.1.10");
                    dialogRef.set(dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Setup timed out");
        PrinterDiscoveryDialog dialog = dialogRef.get();

        // Start scan
        Gdx.app.postRunnable(() -> dialog.startScan());
        Thread.sleep(500);

        // Simulate discovery of a device WITH mDNS
        DiscoveredDevice deviceWithMdns = new DiscoveredDevice("192.168.1.5");
        MdnsServiceInfo service = new MdnsServiceInfo("TestPrinter", "_http._tcp.local.", "192.168.1.5", 80, "printer.local", Collections.singletonMap("v", "1"));
        deviceWithMdns.addMdnsService(service);
        Gdx.app.postRunnable(() -> capturedListener.get().onDeviceDiscovered(deviceWithMdns));
        Thread.sleep(200);

        // Simulate discovery of a device WITHOUT mDNS
        DiscoveredDevice deviceWithoutMdns = new DiscoveredDevice("192.168.1.7");
        Gdx.app.postRunnable(() -> capturedListener.get().onDeviceDiscovered(deviceWithoutMdns));
        Thread.sleep(200);

        // Find the device tables
        Table tableWithMdns = null;
        Table tableWithoutMdns = null;
        for (Actor actor : dialog.resultsTable.getChildren()) {
            if (actor instanceof Table) {
                if ("192.168.1.5".equals(actor.getName())) tableWithMdns = (Table) actor;
                if ("192.168.1.7".equals(actor.getName())) tableWithoutMdns = (Table) actor;
            }
        }

        assertNotNull(tableWithMdns, "Table with mDNS should exist");
        assertNotNull(tableWithoutMdns, "Table without mDNS should exist");

        // Find the "?" buttons
        TextButton buttonWithMdns = findButton(tableWithMdns, "?");
        TextButton buttonWithoutMdns = findButton(tableWithoutMdns, "?");

        assertNotNull(buttonWithMdns, "Button '?' should exist for mDNS device");
        assertNotNull(buttonWithoutMdns, "Button '?' should exist for non-mDNS device");

        assertFalse(buttonWithMdns.isDisabled(), "Button should be ENABLED for mDNS device");
        assertTrue(buttonWithoutMdns.isDisabled(), "Button should be DISABLED for non-mDNS device");

        Gdx.app.exit();
    }

    @Test
    void testMdnsDetailsScrolling() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<NetworkScanner.ScanProgressListener> capturedListener = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    Stage stage = new Stage(new ScreenViewport());
                    stageRef.set(stage);

                    NetworkScanner mockScanner = new NetworkScanner() {
                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            capturedListener.set(listener);
                            return new CompletableFuture<>();
                        }
                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String b, int s, int e, List<Integer> p) { return null; }
                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String b, int s, int e, List<Integer> p, boolean u) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p, boolean u) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String h, List<Integer> p, boolean u, ScanProgressListener l) { return null; }
                        @Override public void setTimeout(int timeoutMillis) {}
                        @Override public int getTimeout() { return 500; }
                        @Override public void setIncludeSelfIp(boolean include) {}
                        @Override public boolean isIncludeSelfIp() { return false; }
                        @Override public void stopScan() {}
                        @Override public void close() {}
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, new NetworkInformationCollector());
                    stage.addActor(dialog);
                    dialogRef.set(dialog);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Setup timed out");
        PrinterDiscoveryDialog dialog = dialogRef.get();

        Gdx.app.postRunnable(() -> {
            dialog.startIpField.setText("192.168.1.1");
            dialog.endIpField.setText("192.168.1.254");
            dialog.startScan();
        });
        Thread.sleep(500);

        DiscoveredDevice device = new DiscoveredDevice("192.168.1.50");
        device.addMdnsService(new MdnsServiceInfo("TestPrinter", "_http._tcp.local.", "192.168.1.50", 80, "printer.local", Map.of("v", "1")));
        
        Gdx.app.postRunnable(() -> {
            assertNotNull(capturedListener.get(), "Listener should be captured");
            capturedListener.get().onDeviceDiscovered(device);
            
            // Find and click hint button
            Table deviceTable = null;
            for (Actor actor : dialog.resultsTable.getChildren()) {
                if (actor instanceof Table && "192.168.1.50".equals(actor.getName())) {
                    deviceTable = (Table) actor;
                    break;
                }
            }
            assertNotNull(deviceTable, "Device table should be present");
            TextButton hintButton = findButton(deviceTable, "?");
            assertNotNull(hintButton, "Hint button should be present");
            hintButton.fire(new ChangeListener.ChangeEvent());
            
            // Look for mDNS details dialog and ScrollPane within it
            Dialog mdnsDialog = null;
            for (Actor actor : stageRef.get().getActors()) {
                if (actor instanceof Dialog && "mDNS Details".equals(((Dialog) actor).getTitleLabel().getText().toString())) {
                    mdnsDialog = (Dialog) actor;
                    break;
                }
            }
            assertNotNull(mdnsDialog, "mDNS Details dialog should be open");
            
            boolean foundScrollPane = false;
            for (Actor actor : mdnsDialog.getContentTable().getChildren()) {
                if (actor instanceof ScrollPane) {
                    foundScrollPane = true;
                    ScrollPane sp = (ScrollPane) actor;
                    assertFalse(sp.getFadeScrollBars(), "Scrollbars should not fade for clarity");
                    break;
                }
            }
            assertTrue(foundScrollPane, "mDNS Details content should be inside a ScrollPane");
        });

        Thread.sleep(500);
        Gdx.app.exit();
    }

    @Test
    void testScanCancellationRetention() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean resultsRetained = new AtomicBoolean(false);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    Skin skin = GdxTestUtils.createTestSkin();
                    
                    CompletableFuture<List<DiscoveredDevice>> scanFuture = new CompletableFuture<>();
                    
                    NetworkScanner mockScanner = new NetworkScanner() {
                        @Override
                        public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) {
                            if (listener != null) {
                                DiscoveredDevice device = new DiscoveredDevice("192.168.1.10");
                                device.addService(new PortScanResult(80, true));
                                listener.onDeviceDiscovered(device);
                            }
                            return scanFuture;
                        }

                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports) { return scanFuture; }
                        @Override public CompletableFuture<List<DiscoveredDevice>> scanRange(String baseIp, int startHost, int endHost, List<Integer> ports, boolean useBannerGrabbing) { return scanFuture; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) { return null; }
                        @Override public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing, ScanProgressListener listener) { return null; }
                        @Override public void setTimeout(int timeoutMillis) {}
                        @Override public int getTimeout() { return 500; }
                        @Override public void setIncludeSelfIp(boolean include) {}
                        @Override public boolean isIncludeSelfIp() { return false; }
                        @Override public void stopScan() { scanFuture.cancel(true); }
                        @Override public void close() {}
                    };
                    
                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override public List<NetworkInterfaceInfo> collect() { return Collections.emptyList(); }
                        @Override public CompletableFuture<List<NetworkInterfaceInfo>> collectAsync() { return CompletableFuture.completedFuture(Collections.emptyList()); }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    dialog.startIpField.setText("192.168.1.1");
                    dialog.endIpField.setText("192.168.1.1");

                    // Start scan
                    dialog.startScan();
                    
                    // The device discovery is posted via Gdx.app.postRunnable
                    Gdx.app.postRunnable(() -> {
                        // Verify device is added to UI
                        boolean deviceFoundBeforeCancel = false;
                        for (Actor actor : dialog.resultsTable.getChildren()) {
                            if (actor instanceof Table && "192.168.1.10".equals(actor.getName())) {
                                deviceFoundBeforeCancel = true;
                                break;
                            }
                        }
                        
                        if (!deviceFoundBeforeCancel) {
                            latch.countDown();
                            return;
                        }

                        // Stop scan (triggers cancellation)
                        dialog.stopScan();
                        
                        // The completion happens on Gdx thread via postRunnable
                        Gdx.app.postRunnable(() -> {
                            boolean deviceFoundAfterCancel = false;
                            for (Actor actor : dialog.resultsTable.getChildren()) {
                                if (actor instanceof Table && "192.168.1.10".equals(actor.getName())) {
                                    deviceFoundAfterCancel = true;
                                    break;
                                }
                            }
                            resultsRetained.set(deviceFoundAfterCancel);
                            
                            // Now start a second scan and verify results are cleared
                            dialog.startScan();
                            boolean resultsClearedOnSecondScan = true;
                            for (Actor actor : dialog.resultsTable.getChildren()) {
                                if (actor instanceof Table && "192.168.1.10".equals(actor.getName())) {
                                    resultsClearedOnSecondScan = false;
                                    break;
                                }
                            }
                            
                            if (resultsClearedOnSecondScan) {
                                latch.countDown();
                            } else {
                                // Explicitly fail if results not cleared
                                resultsRetained.set(false);
                                latch.countDown();
                            }
                        });
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        }, config);
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        assertTrue(resultsRetained.get(), "Results should be retained after scan cancellation");
        Gdx.app.exit();
    }
}

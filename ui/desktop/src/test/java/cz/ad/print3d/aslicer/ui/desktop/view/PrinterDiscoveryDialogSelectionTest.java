package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector;
import cz.ad.print3d.aslicer.logic.net.scanner.NetworkScanner;
import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for multi-selection functionality in {@link PrinterDiscoveryDialog}.
 */
public class PrinterDiscoveryDialogSelectionTest {

    @Test
    void testMultiSelection() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PrinterDiscoveryDialog> dialogRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

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
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports) {
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
                        }

                        @Override
                        public CompletableFuture<DiscoveredDevice> scanHost(String host, List<Integer> ports, boolean useBannerGrabbing) {
                            return CompletableFuture.completedFuture(new DiscoveredDevice(host));
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
                        public void setMdnsTimeout(int timeoutMillis) {}

                        @Override
                        public int getMdnsTimeout() { return 500; }

                        @Override
                        public void setSsdpTimeout(int timeoutMillis) {}

                        @Override
                        public int getSsdpTimeout() { return 500; }

                        @Override
                        public void setIncludeSelfIp(boolean include) {}

                        @Override
                        public boolean isIncludeSelfIp() { return false; }

                        @Override
                        public void stopScan() {}

                        @Override
                        public void close() {}
                    };

                    NetworkInformationCollector mockCollector = new NetworkInformationCollector() {
                        @Override
                        public CompletableFuture<List<cz.ad.print3d.aslicer.logic.net.info.NetworkInterfaceInfo>> collectAsync() {
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        }
                    };

                    PrinterDiscoveryDialog dialog = new PrinterDiscoveryDialog(skin, mockScanner, mockCollector);
                    dialogRef.set(dialog);

                    // Add some mock devices
                    DiscoveredDevice device1 = new DiscoveredDevice("192.168.1.10");
                    DiscoveredDevice device2 = new DiscoveredDevice("192.168.1.11");
                    dialog.updateResults(Arrays.asList(device1, device2));

                    // Verify both devices are present
                    List<DiscoveredDevice> allDevices = dialog.getDiscoveredDevices();
                    assertEquals(2, allDevices.size());
                    assertFalse(allDevices.get(0).isSelected());
                    assertFalse(allDevices.get(1).isSelected());

                    // Find and click the first checkbox
                    CheckBox cb1 = findCheckBoxForDevice(dialog, "192.168.1.10");
                    assertNotNull(cb1);
                    cb1.setChecked(true);
                    // Manually trigger change listener if setChecked doesn't (it should in Scene2D)
                    cb1.fire(new ChangeListener.ChangeEvent());

                    // Verify selection
                    assertTrue(device1.isSelected() || allDevices.stream().filter(d -> d.getIpAddress().equals("192.168.1.10")).findFirst().get().isSelected());
                    List<DiscoveredDevice> selected = dialog.getSelectedDevices();
                    assertEquals(1, selected.size());
                    assertEquals("192.168.1.10", selected.get(0).getIpAddress());

                    // Select second device too
                    CheckBox cb2 = findCheckBoxForDevice(dialog, "192.168.1.11");
                    assertNotNull(cb2);
                    cb2.setChecked(true);
                    cb2.fire(new ChangeListener.ChangeEvent());

                    selected = dialog.getSelectedDevices();
                    assertEquals(2, selected.size());

                    // Deselect first
                    cb1.setChecked(false);
                    cb1.fire(new ChangeListener.ChangeEvent());
                    selected = dialog.getSelectedDevices();
                    assertEquals(1, selected.size());
                    assertEquals("192.168.1.11", selected.get(0).getIpAddress());

                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }
    }

    private CheckBox findCheckBoxForDevice(PrinterDiscoveryDialog dialog, String ip) {
        Table resultsTable = dialog.resultsTable;
        for (Actor actor : resultsTable.getChildren()) {
            if (actor instanceof Table && ip.equals(actor.getName())) {
                Table deviceTable = (Table) actor;
                for (Actor inner : deviceTable.getChildren()) {
                    if (inner instanceof CheckBox) {
                        return (CheckBox) inner;
                    }
                }
            }
        }
        return null;
    }
}

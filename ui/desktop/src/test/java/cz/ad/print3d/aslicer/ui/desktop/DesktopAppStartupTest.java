package cz.ad.print3d.aslicer.ui.desktop;
import cz.ad.print3d.aslicer.ui.desktop.config.*;
import cz.ad.print3d.aslicer.ui.desktop.persistence.*;
import cz.ad.print3d.aslicer.ui.desktop.view.*;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DesktopApp startup behavior.
 */
public class DesktopAppStartupTest {
    @TempDir
    Path tempDir;

    private Path originalConfigPath;
    private Path originalWorkspacePath;

    @BeforeEach
    void setUp() {
        originalConfigPath = AppConfig.CONFIG_PATH;
        AppConfig.CONFIG_PATH = tempDir.resolve(".aslicer").resolve("startup.properties");
        originalWorkspacePath = ScenePersistence.WORKSPACE_PATH;
        ScenePersistence.WORKSPACE_PATH = tempDir.resolve(".aslicer").resolve("workspace.g3db");
    }

    @AfterEach
    void tearDown() {
        AppConfig.CONFIG_PATH = originalConfigPath;
        ScenePersistence.WORKSPACE_PATH = originalWorkspacePath;
    }

    @Test
    void testNoDefaultModelOnFirstStart() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Integer> modelCount = new AtomicReference<>(-1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    // Mock minimal GL20
                    Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                            com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                            new Class[]{com.badlogic.gdx.graphics.GL20.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture") 
                                    || method.getName().equals("glCreateShader") || method.getName().equals("glCreateProgram")) return 1;
                                if (method.getName().equals("glGetIntegerv")) {
                                    java.nio.IntBuffer params = (java.nio.IntBuffer) args[1];
                                    params.put(0, 16);
                                    return null;
                                }
                                if (method.getName().equals("glGetShaderiv") || method.getName().equals("glGetProgramiv")) {
                                    java.nio.IntBuffer params = (java.nio.IntBuffer) args[2];
                                    params.put(0, 1); // Success
                                    return null;
                                }
                                if (method.getReturnType().equals(int.class)) return 0;
                                if (method.getReturnType().equals(boolean.class)) return true;
                                return null;
                            }
                    );
                    Gdx.gl = Gdx.gl20;

                    DesktopApp app = new DesktopApp() {
                        @Override
                        public void setupUI() {
                            // No-op for headless tests
                        }
                    };
                    app.create();
                    modelCount.set(app.instances.size);
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (errorRef.get() != null) {
            fail(errorRef.get());
        }
        assertEquals(0, modelCount.get(), "No model should be loaded on first start");
    }

    @Test
    void testNoDefaultModelAfterClearing() throws InterruptedException {
        // 1. First run: load a model, clear it, and save config
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef1 = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                            com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                            new Class[]{com.badlogic.gdx.graphics.GL20.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture") 
                                    || method.getName().equals("glCreateShader") || method.getName().equals("glCreateProgram")) return 1;
                                if (method.getName().equals("glGetIntegerv")) {
                                    java.nio.IntBuffer params = (java.nio.IntBuffer) args[1];
                                    params.put(0, 16);
                                    return null;
                                }
                                if (method.getName().equals("glGetShaderiv") || method.getName().equals("glGetProgramiv")) {
                                    java.nio.IntBuffer params = (java.nio.IntBuffer) args[2];
                                    params.put(0, 1); // Success
                                    return null;
                                }
                                if (method.getReturnType().equals(int.class)) return 0;
                                if (method.getReturnType().equals(boolean.class)) return true;
                                return null;
                            }
                    );
                    Gdx.gl = Gdx.gl20;

                    DesktopApp app = new DesktopApp() {
                        @Override
                        public void setupUI() {
                            // No-op for headless tests
                        }
                    };
                    app.create();
                    
                    // Manually load and then clear
                    java.nio.file.Path fileAPath = java.nio.file.Paths.get("..", "..", "logic", "model", "src", "test", "resources", "stl", "test-binary.stl").toAbsolutePath().normalize();
                    app.loadModel(fileAPath.toString());
                    assertFalse(app.instances.isEmpty());
                    
                    // Clear all models (similar to AppToolbar.ToolbarListener.onClear)
                    app.instances.clear();
                    app.loadedModelPaths.clear();
                    
                    app.saveAllConfig();
                } catch (Throwable t) {
                    errorRef1.set(t);
                } finally {
                    latch1.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch1.await(10, TimeUnit.SECONDS));
        if (errorRef1.get() != null) fail(errorRef1.get());

        // 2. Second run: check that no model is loaded
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<Integer> modelCount2 = new AtomicReference<>(-1);
        AtomicReference<Throwable> errorRef2 = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                            com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                            new Class[]{com.badlogic.gdx.graphics.GL20.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture") 
                                    || method.getName().equals("glCreateShader") || method.getName().equals("glCreateProgram")) return 1;
                                if (method.getName().equals("glGetIntegerv")) {
                                    java.nio.IntBuffer params = (java.nio.IntBuffer) args[1];
                                    params.put(0, 16);
                                    return null;
                                }
                                if (method.getName().equals("glGetShaderiv") || method.getName().equals("glGetProgramiv")) {
                                    java.nio.IntBuffer params = (java.nio.IntBuffer) args[2];
                                    params.put(0, 1); // Success
                                    return null;
                                }
                                if (method.getReturnType().equals(int.class)) return 0;
                                if (method.getReturnType().equals(boolean.class)) return true;
                                return null;
                            }
                    );
                    Gdx.gl = Gdx.gl20;

                    DesktopApp app = new DesktopApp() {
                        @Override
                        public void setupUI() {
                            // No-op for headless tests
                        }
                    };
                    app.create();
                    modelCount2.set(app.instances.size);
                } catch (Throwable t) {
                    errorRef2.set(t);
                } finally {
                    latch2.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        if (errorRef2.get() != null) fail(errorRef2.get());
        assertEquals(0, modelCount2.get(), "No model should be loaded after clearing and restarting");
    }
}

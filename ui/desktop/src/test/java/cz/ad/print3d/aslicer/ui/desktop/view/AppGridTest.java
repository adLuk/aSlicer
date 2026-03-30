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
package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AppGrid class.
 */
public class AppGridTest {

    @Test
    void testAppGridCreation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    float step = 1.0f;
                    AppGrid grid = new AppGrid(step);
                    
                    assertEquals(step, grid.getStep(), 0.0001f);
                    assertNotNull(grid.getInstance(), "Grid instance should not be null");
                    assertNotNull(grid.getInstance().model, "Grid model should not be null");
                    
                    grid.dispose();
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

            private void mockGdxGL() {
                Gdx.gl20 = (GL20) java.lang.reflect.Proxy.newProxyInstance(
                        GL20.class.getClassLoader(),
                        new Class<?>[]{GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture")) return 1;
                            if (method.getReturnType().equals(int.class)) return 0;
                            if (method.getReturnType().equals(boolean.class)) return true;
                            return null;
                        }
                );
                Gdx.gl = Gdx.gl20;
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        if (errorRef.get() != null) {
            errorRef.get().printStackTrace();
            fail("Error during grid creation: " + errorRef.get().getMessage());
        }
    }
}

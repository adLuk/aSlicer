package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

import java.lang.reflect.Proxy;
import java.nio.IntBuffer;

/**
 * Utility class for LibGDX testing, providing common mocking functionality.
 */
public class GdxTestUtils {

    /**
     * Configures a robust mock for GDX GL20 environment.
     * This mock handles buffer generation, shader/program creation, and basic queries
     * like glGetIntegerv which are required by ModelBatch and other GDX components.
     */
    public static void mockGdxGL() {
        Gdx.gl20 = (GL20) Proxy.newProxyInstance(
                GL20.class.getClassLoader(),
                new Class[]{GL20.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("glGenBuffer") || name.equals("glGenTexture") ||
                            name.equals("glCreateShader") || name.equals("glCreateProgram")) {
                        return 1;
                    }
                    if (name.equals("glGetShaderiv") || name.equals("glGetProgramiv")) {
                        IntBuffer params = (IntBuffer) args[2];
                        params.put(0, 1);
                        return null;
                    }
                    if (name.equals("glGetActiveAttrib")) {
                        IntBuffer size = (IntBuffer) args[2];
                        IntBuffer type = (IntBuffer) args[3];
                        size.put(0, 1);
                        type.put(0, GL20.GL_FLOAT_VEC4);
                        int index = (int) args[1];
                        if (index == 0) return "a_position";
                        if (index == 1) return "a_color";
                        if (index == 2) return "a_texCoord0";
                        return "attr" + index;
                    }
                    if (name.equals("glGetActiveUniform")) {
                        IntBuffer size = (IntBuffer) args[2];
                        IntBuffer type = (IntBuffer) args[3];
                        size.put(0, 1);
                        type.put(0, GL20.GL_FLOAT_MAT4);
                        int index = (int) args[1];
                        if (index == 0) return "u_projTrans";
                        return "uni" + index;
                    }
                    if (name.equals("glGetIntegerv")) {
                        IntBuffer params = (IntBuffer) args[1];
                        params.put(0, 16);
                        return null;
                    }
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(boolean.class)) return true;
                    return null;
                }
        );
        Gdx.gl = Gdx.gl20;
    }
}

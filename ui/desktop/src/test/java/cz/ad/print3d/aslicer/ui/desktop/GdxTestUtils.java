package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

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

    /**
     * Creates a minimal UI skin for testing purposes.
     *
     * @return the created Skin object
     */
    public static Skin createTestSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.selection = skin.newDrawable("white", Color.GRAY);
        listStyle.fontColorSelected = Color.BLACK;
        listStyle.fontColorUnselected = Color.WHITE;
        skin.add("default", listStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        skin.add("default", scrollPaneStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        skin.add("default", windowStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        ImageButton.ImageButtonStyle imageButtonStyle = new ImageButton.ImageButtonStyle();
        skin.add("default", imageButtonStyle);

        return skin;
    }
}

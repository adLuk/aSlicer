package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Central class for handling localization in the desktop application.
 * It uses LibGDX's {@link I18NBundle} to load and provide localized strings,
 * falling back to {@link ResourceBundle} if LibGDX is not yet initialized.
 */
public final class I18N {
    private static I18NBundle gdxBundle;
    private static ResourceBundle javaBundle;

    /**
     * Initializes the localization system with the default locale.
     */
    public static void init() {
        init(Locale.getDefault());
    }

    /**
     * Initializes the localization system with the specified locale.
     *
     * @param locale the locale to use for loading the bundle
     */
    public static void init(Locale locale) {
        try {
            javaBundle = ResourceBundle.getBundle("i18n.messages", locale);
        } catch (Exception e) {
            // Log error or handle missing bundle
        }

        if (Gdx.files != null) {
            try {
                gdxBundle = I18NBundle.createBundle(Gdx.files.internal("i18n/messages"), locale, "UTF-8");
            } catch (Exception e) {
                // Log error or handle missing bundle
            }
        }
    }

    /**
     * Gets the localized string for the specified key.
     *
     * @param key the key for the localized string
     * @return the localized string, or the key itself if not found
     */
    public static String get(String key) {
        if (gdxBundle != null) {
            return gdxBundle.get(key);
        }
        if (javaBundle != null && javaBundle.containsKey(key)) {
            return javaBundle.getString(key);
        }
        return key;
    }

    /**
     * Gets the localized string for the specified key and formats it with the given arguments.
     *
     * @param key  the key for the localized string
     * @param args the arguments to use for formatting
     * @return the localized and formatted string, or the key itself if not found
     */
    public static String format(String key, Object... args) {
        if (gdxBundle != null) {
            return gdxBundle.format(key, args);
        }
        if (javaBundle != null && javaBundle.containsKey(key)) {
            return MessageFormat.format(javaBundle.getString(key), args);
        }
        return key;
    }

    private I18N() {
        // Private constructor to prevent instantiation
    }
}

// client/ui/theme/FontManager.java
package client.ui.theme;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final Map<String, Font> fontCache = new HashMap<>();
    private static final String DEFAULT_FONT_PATH = "resources/fonts/DungGeunMo.ttf";

    public static Font getFont(float size) {
        return getCustomFont(DEFAULT_FONT_PATH, size);
    }

    public static Font getCustomFont(String path, float size) {
        String key = path + size;
        return fontCache.computeIfAbsent(key, k -> {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, new File(path))
                        .deriveFont(size);
            } catch (Exception e) {
                System.err.println("폰트 로딩 실패: " + e.getMessage());
                return new Font("Dialog", Font.PLAIN, (int)size);
            }
        });
    }
}

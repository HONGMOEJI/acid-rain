package client.ui.theme;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final Map<String, Font> fontCache = new HashMap<>();
    private static final String DEFAULT_FONT_RESOURCE = "/fonts/DungGeunMo.ttf";

    public static Font getFont(float size) {
        return getCustomFont(DEFAULT_FONT_RESOURCE, size);
    }

    public static Font getCustomFont(String resourcePath, float size) {
        String key = resourcePath + size;
        return fontCache.computeIfAbsent(key, k -> {
            try (InputStream is = FontManager.class.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.err.println("폰트 리소스를 찾을 수 없습니다: " + resourcePath);
                    return new Font("Dialog", Font.PLAIN, (int)size);
                }
                return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
            } catch (Exception e) {
                System.err.println("폰트 로딩 실패: " + e.getMessage());
                return new Font("Dialog", Font.PLAIN, (int) size);
            }
        });
    }
}

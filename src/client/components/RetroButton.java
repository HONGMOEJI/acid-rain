package client.components;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class RetroButton extends JButton {
    private static final Color NORMAL_COLOR = new Color(71, 185, 251);
    private static final Color HOVER_COLOR = new Color(91, 205, 255);
    private static final Color PRESSED_COLOR = new Color(51, 165, 231);
    private static final Font RETRO_FONT = createRetroFont();

    public RetroButton(String text) {
        super(text);
        setupStyle();
    }

    private static Font createRetroFont() {
        try {
            File fontFile = new File("resources/fonts/DungGeunMo.ttf");
            if (!fontFile.exists()) {
                System.err.println("폰트 파일을 찾을 수 없습니다: " + fontFile.getAbsolutePath());
                return new Font("Dialog", Font.PLAIN, 16);
            }
            return Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(16f);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Dialog", Font.PLAIN, 16);
        }
    }

    private void setupStyle() {
        setFont(RETRO_FONT);
        setForeground(Color.WHITE);
        setBackground(NORMAL_COLOR);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(true);

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setBackground(HOVER_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setBackground(NORMAL_COLOR);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                setBackground(PRESSED_COLOR);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                setBackground(HOVER_COLOR);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // 버튼 배경
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // 텍스트 그리기
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

        g2d.setColor(getForeground());
        g2d.setFont(getFont());
        g2d.drawString(getText(), x, y);
    }
}
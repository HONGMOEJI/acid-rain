package client;

import client.components.RoomListDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.File;

public class MainMenu extends JPanel {
    private static final Color BACKGROUND_COLOR = new Color(28, 31, 43);
    private static final Color WAVE_COLOR = new Color(71, 185, 251);
    private static final Color MENU_BAR_COLOR = new Color(0, 0, 102);
    private static final Color MENU_HOVER_COLOR = new Color(71, 185, 251);
    private static final Font TITLE_FONT = createTitleFont();

    private final String username;
    private int connectedUsers = 0;
    private final Timer waveTimer;
    private double waveOffset = 0;
    private JPanel contentPanel;
    private final GameClient client;
    private Color currentBackgroundColor;
    private JPanel topPanel;
    private JPanel bottomPanel;

    public MainMenu(GameClient client) {
        this.client = client;
        this.username = client.getUsername();
        this.currentBackgroundColor = BACKGROUND_COLOR;
        setLayout(new BorderLayout());

        createTopMenuBar();
        createContentPanel();
        createBottomBar();

        waveTimer = new Timer(50, e -> {
            waveOffset += 0.1;
            contentPanel.repaint();
        });
        waveTimer.start();

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                    startGame();
                }
            }
        });
    }

    private static Font createTitleFont() {
        try {
            File fontFile = new File("resources/fonts/DungGeunMo.ttf");
            if (!fontFile.exists()) {
                System.err.println("폰트 파일을 찾을 수 없습니다: " + fontFile.getAbsolutePath());
                return new Font("Dialog", Font.BOLD, 36);
            }
            return Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(36f);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Dialog", Font.BOLD, 36);
        }
    }

    private void createTopMenuBar() {
        topPanel = new JPanel(new BorderLayout());
        topPanel.setPreferredSize(new Dimension(getWidth(), 40));
        topPanel.setBackground(calculateMenuBarColor(currentBackgroundColor));

        JPanel leftMenu = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftMenu.setOpaque(false);
        JLabel settingsLabel = createMenuLabel("배경색 설정");
        JLabel myPageLabel = createMenuLabel("마이페이지");
        leftMenu.add(settingsLabel);
        leftMenu.add(myPageLabel);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setOpaque(false);
        JLabel welcomeLabel = createMenuLabel(username + "님이 입장하셨습니다.");
        centerPanel.add(welcomeLabel);

        JPanel rightMenu = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightMenu.setOpaque(false);
        JLabel rankingLabel = createMenuLabel("랭킹");
        JLabel exitLabel = createMenuLabel("종료");
        rightMenu.add(rankingLabel);
        rightMenu.add(exitLabel);

        topPanel.add(leftMenu, BorderLayout.WEST);
        topPanel.add(centerPanel, BorderLayout.CENTER);
        topPanel.add(rightMenu, BorderLayout.EAST);

        settingsLabel.addMouseListener(createMouseListener(() -> showBackgroundColorDialog()));
        myPageLabel.addMouseListener(createMouseListener(() -> showMyPage()));
        rankingLabel.addMouseListener(createMouseListener(() -> showRanking()));
        exitLabel.addMouseListener(createMouseListener(() -> System.exit(0)));

        add(topPanel, BorderLayout.NORTH);
    }

    private void createContentPanel() {
        contentPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(currentBackgroundColor);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                drawCenterMessages(g2d);
                drawWaves(g2d);
            }
        };
        contentPanel.setBackground(currentBackgroundColor);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void createBottomBar() {
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setPreferredSize(new Dimension(getWidth(), 40));
        bottomPanel.setBackground(calculateMenuBarColor(currentBackgroundColor));

        JLabel startGuideLabel = createMenuLabel("게임을 시작하려면 Ctrl + S를 눌러주세요");
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        leftPanel.add(startGuideLabel);

        JLabel usersLabel = createMenuLabel("현재 접속자 수: " + connectedUsers);
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.add(usersLabel);

        bottomPanel.add(leftPanel, BorderLayout.WEST);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private Color calculateMenuBarColor(Color backgroundColor) {
        float[] hsb = Color.RGBtoHSB(
                backgroundColor.getRed(),
                backgroundColor.getGreen(),
                backgroundColor.getBlue(),
                null
        );

        // 어두운 배경색일 경우 더 어둡게, 밝은 배경색일 경우 더 밝게
        float brightness = hsb[2];
        if (brightness < 0.5f) {
            brightness = Math.max(0f, brightness - 0.2f);
        } else {
            brightness = Math.min(1f, brightness + 0.2f);
        }

        return Color.getHSBColor(hsb[0], hsb[1], brightness);
    }

    private void updateMenuBarColors() {
        Color menuBarColor = calculateMenuBarColor(currentBackgroundColor);
        topPanel.setBackground(menuBarColor);
        bottomPanel.setBackground(menuBarColor);
    }

    private JLabel createMenuLabel(String text) {
        JLabel label = new JLabel(text);
        try {
            Font menuFont = Font.createFont(Font.TRUETYPE_FONT,
                    new File("resources/fonts/DungGeunMo.ttf")).deriveFont(14f);
            label.setFont(menuFont);
        } catch (Exception e) {
            e.printStackTrace();
            label.setFont(new Font("Dialog", Font.PLAIN, 14));
        }
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    private MouseAdapter createMouseListener(Runnable action) {
        return new MouseAdapter() {
            private Color originalColor;

            @Override
            public void mouseEntered(MouseEvent e) {
                JLabel label = (JLabel)e.getComponent();
                originalColor = label.getForeground();
                label.setForeground(MENU_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel)e.getComponent();
                label.setForeground(isLightColor(currentBackgroundColor) ? Color.BLACK : Color.WHITE);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        };
    }

    private void updateTextColors() {
        Color textColor = isLightColor(currentBackgroundColor) ? Color.BLACK : Color.WHITE;

        for (Component comp : getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                updatePanelTextColors(panel, textColor);
            }
        }
        contentPanel.repaint();
    }

    private void updatePanelTextColors(JPanel panel, Color textColor) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(textColor);
            } else if (comp instanceof JPanel) {
                JPanel nestedPanel = (JPanel) comp;
                for (Component nestedComp : nestedPanel.getComponents()) {
                    if (nestedComp instanceof JLabel) {
                        JLabel label = (JLabel) nestedComp;
                        if (!label.getForeground().equals(MENU_HOVER_COLOR)) {
                            label.setForeground(textColor);
                        }
                    }
                }
            }
        }
    }

    private boolean isLightColor(Color color) {
        double brightness = (color.getRed() * 299 +
                color.getGreen() * 587 +
                color.getBlue() * 114) / 1000.0;
        return brightness > 128;
    }

    private void drawCenterMessages(Graphics2D g2d) {
        try {
            Font messageFont = Font.createFont(Font.TRUETYPE_FONT,
                    new File("resources/fonts/DungGeunMo.ttf")).deriveFont(16f);
            g2d.setFont(messageFont);

            Color textColor = isLightColor(currentBackgroundColor) ? Color.BLACK : Color.WHITE;
            g2d.setColor(textColor);

            String[] messages = {
                    "타이핑 실력을 향상시켜보세요!",
                    "내리는 단어를 정확하게 입력하면 점수를 획득합니다.",
                    "특별한 단어를 입력하면 추가 점수를 획득할 수 있습니다.",
                    "",
                    "준비되셨나요?",
                    "Ctrl + S를 눌러 게임을 시작하세요!",
            };

            int y = 100;
            for (String message : messages) {
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(message)) / 2;
                g2d.drawString(message, x, y);
                y += 40;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawWaves(Graphics2D g2d) {
        int height = getHeight();
        int width = getWidth();
        int waveStartY = height - 150;

        for (int i = 0; i < 3; i++) {
            float alpha = 0.3f - (i * 0.1f);
            Color waveColor = new Color(
                    WAVE_COLOR.getRed()/255f,
                    WAVE_COLOR.getGreen()/255f,
                    WAVE_COLOR.getBlue()/255f,
                    alpha
            );
            g2d.setColor(waveColor);

            Path2D path = new Path2D.Double();
            path.moveTo(0, height);

            for (int x = 0; x <= width; x++) {
                double amplitude = 30.0;
                double frequency = 40.0;
                double y = Math.sin((x + waveOffset + (i * 30)) / frequency) * amplitude;
                path.lineTo(x, waveStartY - y - (i * 30));
            }

            path.lineTo(width, height);
            path.closePath();
            g2d.fill(path);
        }
    }

    private void showBackgroundColorDialog() {
        Color newColor = JColorChooser.showDialog(
                this,
                "배경색 선택",
                currentBackgroundColor
        );
        if (newColor != null) {
            currentBackgroundColor = newColor;
            setBackground(currentBackgroundColor);
            contentPanel.setBackground(currentBackgroundColor);
            updateTextColors();
            updateMenuBarColors();
            revalidate();
            repaint();
        }
    }

    private void showMyPage() {
        JOptionPane.showMessageDialog(this, "마이페이지 기능은 준비 중입니다.");
    }

    private void showRanking() {
        JOptionPane.showMessageDialog(this, "랭킹 기능은 준비 중입니다.");
    }

    private void startGame() {
        SwingUtilities.invokeLater(() -> {
            JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            RoomListDialog roomListDialog = new RoomListDialog(currentFrame, client);
            roomListDialog.setVisible(true);
        });
    }

    public void updateConnectedUsers(int count) {
        this.connectedUsers = count;
        SwingUtilities.invokeLater(() -> {
            Component[] components = bottomPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    Component[] subComps = ((JPanel)comp).getComponents();
                    for (Component subComp : subComps) {
                        if (subComp instanceof JLabel &&
                                ((JLabel)subComp).getText().startsWith("현재 접속자 수")) {
                            ((JLabel)subComp).setText("현재 접속자 수: " + count);
                            break;
                        }
                    }
                }
            }
        });
    }
}
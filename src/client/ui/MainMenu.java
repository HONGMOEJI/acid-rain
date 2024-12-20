/*
 * client.ui.MainMenu.java
 * 메인 메뉴 화면을 구성하는 클래스
 */

package client.ui;

import client.app.GameClient;
import client.event.GameEvent;
import client.event.GameEventListener;
import client.ui.dialog.LeaderboardDialog;
import client.ui.dialog.RoomListDialog;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import client.ui.theme.StyleManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;

public class MainMenu extends JPanel implements GameEventListener {
    private final GameClient client;
    private final Timer waveAnimationTimer;
    private double waveOffset = 0;
    private int connectedUsers = 0;

    private MainContentPanel contentPanel;
    private JPanel topMenuBar;
    private JPanel statusBar;
    private JLabel connectedUsersLabel;

    public MainMenu(GameClient client) {
        this.client = client;
        this.waveAnimationTimer = new Timer(50, e -> {
            waveOffset += 0.1;
            contentPanel.repaint();
        });

        initializeUI();
        setupKeyboardShortcuts();
        waveAnimationTimer.start();

        // 메인 메뉴가 생성될 때, 서버에 접속자 수 요청
        requestUserCountUpdate();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.BACKGROUND);

        createTopMenuBar();
        createMainContent();
        createStatusBar();
    }

    private void requestUserCountUpdate() {
        client.sendMessage(GameEvent.CMD_USERS_REQUEST);
    }

    private void createTopMenuBar() {
        topMenuBar = new JPanel(new BorderLayout());
        topMenuBar.setBackground(ColorScheme.SECONDARY);

        JPanel leftMenu = createMenuSection(FlowLayout.LEFT,
                createMenuLabel("배경색 설정", this::showBackgroundColorDialog),
                createMenuLabel("마이페이지", this::showMyPage)
        );

        JPanel centerMenu = createMenuSection(FlowLayout.CENTER,
                createMenuLabel(client.getUsername() + "님이 입장하셨습니다.", null)
        );

        JPanel rightMenu = createMenuSection(FlowLayout.RIGHT,
                createMenuLabel("랭킹", this::showRanking),
                createMenuLabel("종료", () -> System.exit(0))
        );

        topMenuBar.add(leftMenu, BorderLayout.WEST);
        topMenuBar.add(centerMenu, BorderLayout.CENTER);
        topMenuBar.add(rightMenu, BorderLayout.EAST);

        add(topMenuBar, BorderLayout.NORTH);
    }

    private JPanel createMenuSection(int alignment, JLabel... labels) {
        JPanel panel = new JPanel(new FlowLayout(alignment, 15, 10));
        panel.setOpaque(false);
        for (JLabel label : labels) {
            panel.add(label);
        }
        return panel;
    }

    private JLabel createMenuLabel(String text, Runnable action) {
        JLabel label = new JLabel(text);
        StyleManager.applyLabelStyle(label);

        if (action != null) {
            label.setCursor(new Cursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    label.setForeground(ColorScheme.PRIMARY);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    label.setForeground(ColorScheme.TEXT);
                }
            });
        }

        return label;
    }

    private void createMainContent() {
        contentPanel = new MainContentPanel();
        add(contentPanel, BorderLayout.CENTER);
    }

    private void createStatusBar() {
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(ColorScheme.SECONDARY);

        connectedUsersLabel = new JLabel("현재 접속자 수: " + connectedUsers);
        StyleManager.applyLabelStyle(connectedUsersLabel);

        JLabel startGuide = new JLabel("게임을 시작하려면 Ctrl + S를 눌러주세요");
        StyleManager.applyLabelStyle(startGuide);

        statusBar.add(startGuide, BorderLayout.WEST);
        statusBar.add(connectedUsersLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        KeyStroke startKey = KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK);
        inputMap.put(startKey, "StartGame");
        actionMap.put("StartGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });
    }

    private void startGame() {
        setVisible(false);  // 메인 메뉴 숨기기
        JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        RoomListDialog dialog = new RoomListDialog(currentFrame, client);
        dialog.setVisible(true);
    }

    private void showBackgroundColorDialog() {
        Color newColor = JColorChooser.showDialog(this, "배경색 선택",
                getBackground());
        if (newColor != null) {
            setBackground(newColor);
            contentPanel.setBackground(newColor);
            repaint();
        }
    }

    private void showMyPage() {
        JOptionPane.showMessageDialog(this, "마이페이지 기능은 준비 중입니다.");
    }

    private void showRanking() {
        JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        LeaderboardDialog leaderboardDialog = new LeaderboardDialog(currentFrame, client);

        // 현재 이벤트 리스너 (this) 임시 저장
        GameEventListener currentListener = this;

        // 리더보드 다이얼로그 표시
        leaderboardDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // 다이얼로그가 닫힐 때 메인 메뉴의 이벤트 리스너로 복원
                client.setEventListener(currentListener);
            }
        });

        leaderboardDialog.setVisible(true);
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        if (eventType.equals(GameEvent.USERS_UPDATED) && data.length > 0) {
            int newConnectedUsers = (int) data[0];
            SwingUtilities.invokeLater(() -> {
                connectedUsers = newConnectedUsers;
                connectedUsersLabel.setText("현재 접속자 수: " + connectedUsers);
            });
        }
    }

    private class MainContentPanel extends JPanel {
        public MainContentPanel() {
            setBackground(ColorScheme.BACKGROUND);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 배경
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 메인 텍스트
            drawMainContent(g2d);

            // 웨이브 효과
            drawWaves(g2d);
        }

        private void drawMainContent(Graphics2D g2d) {
            g2d.setFont(FontManager.getFont(16f));
            g2d.setColor(ColorScheme.TEXT);

            String[] messages = {
                    "타이핑 실력을 향상시켜보세요!",
                    "내리는 단어를 정확하게 입력하면 점수를 획득합니다.",
                    "특별한 단어를 입력하면 추가 점수를 획득할 수 있습니다.",
                    "",
                    "준비되셨나요?",
                    "Ctrl + S를 눌러 게임을 시작하세요!"
            };

            FontMetrics fm = g2d.getFontMetrics();
            int y = 100;

            for (String message : messages) {
                int x = (getWidth() - fm.stringWidth(message)) / 2;
                g2d.drawString(message, x, y);
                y += 40;
            }
        }

        private void drawWaves(Graphics2D g2d) {
            int height = getHeight();
            int width = getWidth();
            int waveStartY = height - 150;

            for (int i = 0; i < 3; i++) {
                float alpha = 0.3f - (i * 0.1f);
                g2d.setColor(new Color(
                        ColorScheme.PRIMARY.getRed(),
                        ColorScheme.PRIMARY.getGreen(),
                        ColorScheme.PRIMARY.getBlue(),
                        (int)(alpha * 255)
                ));

                Path2D path = new Path2D.Double();
                path.moveTo(0, height);

                for (int x = 0; x <= width; x++) {
                    double y = Math.sin((x + waveOffset + (i * 30)) / 40.0) * 30.0;
                    path.lineTo(x, waveStartY - y - (i * 30));
                }

                path.lineTo(width, height);
                path.closePath();
                g2d.fill(path);
            }
        }
    }
}

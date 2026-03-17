/*
 * client.ui.game.GameScreen.java
 * 게임 화면을 정의하는 클래스, 단어를 입력하고 점수를 획득하는 게임 화면
 */

package client.ui.game;

import client.app.GameClient;
import client.event.GameEvent.*;
import client.event.GameEventListener;
import client.ui.MainMenu;
import client.ui.components.GameTextField;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.Word;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GameScreen extends JFrame implements GameEventListener {
    private static final Logger logger = Logger.getLogger(GameScreen.class.getName());
    private static final double INITIAL_PH = 7.0;

    private final GameClient client;
    private final String roomId;
    private final JFrame mainFrame;
    private final String myName;
    private final String[] players;

    private final Map<String, Integer> scoreByPlayer = new LinkedHashMap<>();
    private final Map<String, Double> phByPlayer = new LinkedHashMap<>();
    private final List<Word> activeWords = new ArrayList<>();

    private JPanel gamePanel;
    private JPanel scoreboardPanel;
    private GameTextField inputField;
    private JLabel scoreLabel;
    private JLabel phLabel;
    private JLabel roomInfoLabel;
    private JProgressBar phMeter;
    private Timer screenRefreshTimer;

    private boolean isBlinded = false;
    private long blindEndTime = 0;
    private volatile boolean isClosing = false;
    private boolean roomLeaveSent = false;

    public GameScreen(GameClient client, String roomId, String myName, String[] players, JFrame mainFrame) {
        this.client = client;
        this.roomId = roomId;
        this.myName = myName;
        this.players = players != null && players.length > 0 ? players : new String[]{myName};
        this.mainFrame = mainFrame;

        initializePlayerState();
        client.setEventListener(this);
        initializeFrame();
        setupUI();
        setupInput();
        setupTimers();
    }

    private void initializePlayerState() {
        for (String player : players) {
            scoreByPlayer.put(player, 0);
            phByPlayer.put(player, INITIAL_PH);
        }
        scoreByPlayer.putIfAbsent(myName, 0);
        phByPlayer.putIfAbsent(myName, INITIAL_PH);
    }

    private void initializeFrame() {
        setTitle("Typing Game - " + roomId);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleGameEnd();
            }
        });
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        add(createInfoPanel(), BorderLayout.NORTH);
        add(createGamePanel(), BorderLayout.CENTER);
        add(createScoreboardPanel(), BorderLayout.EAST);
        add(createInputPanel(), BorderLayout.SOUTH);
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.SECONDARY);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        phLabel = new JLabel("pH: 7.0");
        phLabel.setFont(FontManager.getFont(16f));
        phLabel.setForeground(ColorScheme.TEXT);

        phMeter = new JProgressBar(0, 70) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(getForeground());
                int width = (int) (getWidth() * ((double) getValue() / getMaximum()));
                g2d.fillRect(0, 0, width, getHeight());
            }
        };
        phMeter.setValue(70);
        phMeter.setPreferredSize(new Dimension(150, 20));
        phMeter.setForeground(ColorScheme.PH_NORMAL);
        phMeter.setBackground(ColorScheme.BACKGROUND);

        leftPanel.add(phLabel);
        leftPanel.add(phMeter);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setOpaque(false);
        scoreLabel = new JLabel("내 점수: 0");
        scoreLabel.setFont(FontManager.getFont(20f));
        scoreLabel.setForeground(ColorScheme.TEXT);
        centerPanel.add(scoreLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        roomInfoLabel = new JLabel("참가자: " + players.length + "명");
        roomInfoLabel.setFont(FontManager.getFont(16f));
        roomInfoLabel.setForeground(ColorScheme.TEXT);
        rightPanel.add(roomInfoLabel);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createGamePanel() {
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame(g);
            }
        };
        gamePanel.setBackground(ColorScheme.BACKGROUND);
        return gamePanel;
    }

    private JPanel createScoreboardPanel() {
        scoreboardPanel = new JPanel();
        scoreboardPanel.setPreferredSize(new Dimension(240, 0));
        scoreboardPanel.setLayout(new BoxLayout(scoreboardPanel, BoxLayout.Y_AXIS));
        scoreboardPanel.setBackground(ColorScheme.SECONDARY);
        scoreboardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 2, 0, 0, ColorScheme.PRIMARY),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        refreshScoreboardPanel();
        return scoreboardPanel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(ColorScheme.SECONDARY);
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));

        inputField = new GameTextField();
        inputField.setFont(FontManager.getFont(16f));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        JButton exitButton = new JButton("게임 종료 (ESC)");
        styleButton(exitButton);
        exitButton.addActionListener(e -> handleGameEnd());

        buttonPanel.add(exitButton);
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private void styleButton(JButton button) {
        button.setFont(FontManager.getFont(14f));
        button.setForeground(ColorScheme.TEXT);
        button.setBackground(ColorScheme.PRIMARY);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setFocusPainted(false);
    }

    private void setupInput() {
        inputField.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (!input.isEmpty()) {
                client.sendGameAction(roomId, ClientCommand.WORD_INPUT, input);
                inputField.setText("");
            }
        });

        getRootPane().registerKeyboardAction(
                e -> handleGameEnd(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void setupTimers() {
        screenRefreshTimer = new Timer(1000 / 60, e -> refreshScreen());
        screenRefreshTimer.start();
    }

    private void drawGame(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(FontManager.getFont(16f));

        synchronized (activeWords) {
            for (Word word : activeWords) {
                if (word.hasSpecialEffect()) {
                    g2d.setFont(FontManager.getEmojiFont(16f));
                    if (word.getEffect() == Word.SpecialEffect.SCORE_BOOST) {
                        g2d.setColor(ColorScheme.ITEM_SCORE_BOOST);
                        g2d.drawString("⚡", word.getX() - 25, word.getY());
                        g2d.setFont(FontManager.getFont(16f));
                        g2d.setColor(ColorScheme.ITEM_SCORE_BOOST);
                    } else {
                        g2d.setColor(ColorScheme.ITEM_BLIND);
                        g2d.drawString("⭐", word.getX() - 25, word.getY());
                        g2d.setFont(FontManager.getFont(16f));
                        g2d.setColor(ColorScheme.ITEM_BLIND);
                    }
                } else {
                    g2d.setColor(Color.WHITE);
                }
                g2d.drawString(word.getText(), word.getX(), word.getY());
            }
        }

        if (isBlinded && System.currentTimeMillis() < blindEndTime) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        } else if (isBlinded && System.currentTimeMillis() >= blindEndTime) {
            isBlinded = false;
        }
    }

    private void refreshScreen() {
        if (isClosing) {
            return;
        }

        synchronized (activeWords) {
            for (Word word : new ArrayList<>(activeWords)) {
                word.setY(word.getY() + 2);
                if (word.getY() > gamePanel.getHeight()) {
                    activeWords.remove(word);
                    client.sendGameAction(roomId, ClientEvent.WORD_MISSED, word.getText());
                }
            }
        }

        updateGameInfo();
        gamePanel.repaint();
    }

    private void updateGameInfo() {
        int myScore = scoreByPlayer.getOrDefault(myName, 0);
        double myPH = phByPlayer.getOrDefault(myName, INITIAL_PH);

        scoreLabel.setText(String.format("내 점수: %d", myScore));
        phLabel.setText(String.format("pH: %.1f", myPH));

        if (myPH < 5.0) {
            phMeter.setForeground(ColorScheme.PH_DANGER);
        } else if (myPH < 6.0) {
            phMeter.setForeground(ColorScheme.PH_WARNING);
        } else {
            phMeter.setForeground(ColorScheme.PH_NORMAL);
        }

        phMeter.setValue((int) (myPH * 10));
        refreshScoreboardPanel();
    }

    private void refreshScoreboardPanel() {
        if (scoreboardPanel == null) {
            return;
        }

        scoreboardPanel.removeAll();

        JLabel title = new JLabel("실시간 점수판");
        title.setFont(FontManager.getFont(18f));
        title.setForeground(ColorScheme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreboardPanel.add(title);
        scoreboardPanel.add(Box.createVerticalStrut(12));

        for (String player : players) {
            scoreboardPanel.add(createPlayerSummary(player));
            scoreboardPanel.add(Box.createVerticalStrut(8));
        }

        scoreboardPanel.revalidate();
        scoreboardPanel.repaint();
    }

    private JPanel createPlayerSummary(String player) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(ColorScheme.BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        String displayName = player.equals(myName) ? player + " (나)" : player;
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(FontManager.getFont(15f));
        nameLabel.setForeground(ColorScheme.TEXT);

        JLabel valueLabel = new JLabel(String.format("점수 %d | pH %.1f",
                scoreByPlayer.getOrDefault(player, 0),
                phByPlayer.getOrDefault(player, INITIAL_PH)));
        valueLabel.setFont(FontManager.getFont(13f));
        valueLabel.setForeground(ColorScheme.TEXT.brighter());

        panel.add(nameLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void handleGameEnd() {
        if (isClosing) {
            return;
        }

        int option = JOptionPane.showConfirmDialog(this,
                "정말로 게임을 종료하시겠습니까?\n나가면 현재 게임은 종료 처리됩니다.",
                "게임 종료",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            isClosing = true;
            stopTimers();
            client.sendGameAction(roomId, ClientCommand.PLAYER_LEAVE_GAME, myName);
            leaveRoomIfNeeded();
            showMainMenu();
        }
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        if (isClosing) {
            return;
        }

        switch (eventType) {
            case ClientEvent.WORD_SPAWNED -> handleWordSpawned(data);
            case ClientEvent.WORD_MATCHED -> handleWordMatched(data);
            case ClientEvent.WORD_MISSED -> handleWordMissed(data);
            case ClientEvent.PH_UPDATE -> handlePhUpdate(data);
            case ClientEvent.BLIND_EFFECT -> handleBlindEffect(data);
            case ClientEvent.GAME_OVER -> handleGameOver(data);
            case ClientEvent.ROOM_CLOSED -> handleRoomClosed(data);
            default -> {
            }
        }

        gamePanel.repaint();
    }

    private void handleWordSpawned(Object... data) {
        synchronized (activeWords) {
            Word word = new Word((String) data[0], (int) data[1], 0);
            if (data.length > 2) {
                word.setSpecialEffect(true);
                word.setEffect((Word.SpecialEffect) data[2]);
            }
            activeWords.add(word);
        }
    }

    private void handleWordMatched(Object... data) {
        String wordText = (String) data[0];
        String playerName = (String) data[1];
        int newScore = (int) data[2];

        synchronized (activeWords) {
            activeWords.removeIf(w -> w.getText().equals(wordText));
        }
        scoreByPlayer.put(playerName, newScore);
        updateGameInfo();
    }

    private void handleWordMissed(Object... data) {
        String missedWord = (String) data[0];
        String playerName = (String) data[1];
        double newPH = (double) data[2];

        synchronized (activeWords) {
            activeWords.removeIf(w -> w.getText().equals(missedWord));
        }
        phByPlayer.put(playerName, newPH);
        updateGameInfo();
    }

    private void handlePhUpdate(Object... data) {
        String playerName = (String) data[0];
        double newPH = (double) data[1];
        phByPlayer.put(playerName, newPH);
        updateGameInfo();
    }

    private void handleBlindEffect(Object... data) {
        String targetPlayer = (String) data[0];
        int durationMs = (int) data[1];
        if (targetPlayer.equals(myName)) {
            isBlinded = true;
            blindEndTime = System.currentTimeMillis() + durationMs;
        }
    }

    private void handleGameOver(Object... data) {
        if (data.length < 3) {
            return;
        }

        isClosing = true;
        stopTimers();

        String winner = (String) data[0];
        String scoreSummary = (String) data[1];
        boolean isForfeit = (boolean) data[2];
        applyScoreSummary(scoreSummary);

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    buildResultMessage(winner, isForfeit),
                    "게임 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            leaveRoomIfNeeded();
            showMainMenu();
        });
    }

    private void handleRoomClosed(Object... data) {
        isClosing = true;
        stopTimers();

        String reason = data.length >= 2 ? (String) data[1] : "방이 종료되었습니다.";
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "방이 닫혔습니다: " + reason,
                    "게임 종료",
                    JOptionPane.INFORMATION_MESSAGE);
            showMainMenu();
        });
    }

    private void applyScoreSummary(String scoreSummary) {
        if (scoreSummary == null || scoreSummary.isEmpty()) {
            return;
        }

        for (String entry : scoreSummary.split(";")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            try {
                scoreByPlayer.put(parts[0], Integer.parseInt(parts[1]));
            } catch (NumberFormatException e) {
                logger.warning("점수 요약 파싱 실패: " + entry);
            }
        }
    }

    private String buildResultMessage(String winner, boolean isForfeit) {
        StringBuilder builder = new StringBuilder();
        builder.append(winner.equals(myName) ? "승리!" : "패배...");

        if (isForfeit) {
            builder.append("\n사유: 플레이어 이탈");
        }

        builder.append("\n\n최종 점수");
        for (Map.Entry<String, Integer> entry : scoreByPlayer.entrySet()) {
            builder.append("\n- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue());
        }
        return builder.toString();
    }

    private void leaveRoomIfNeeded() {
        if (roomLeaveSent) {
            return;
        }

        roomLeaveSent = true;
        client.sendMessage(ClientCommand.LEAVE_ROOM + "|" + roomId);
    }

    private void showMainMenu() {
        MainMenu mainMenu = new MainMenu(client);
        client.setEventListener(mainMenu);

        if (mainFrame != null) {
            mainFrame.getContentPane().removeAll();
            mainFrame.add(mainMenu);
            mainFrame.setSize(800, 600);
            mainFrame.setLocationRelativeTo(null);
            mainFrame.revalidate();
            mainFrame.repaint();
            mainFrame.setVisible(true);
        }

        super.dispose();
    }

    private void stopTimers() {
        if (screenRefreshTimer != null) {
            screenRefreshTimer.stop();
        }
    }

    @Override
    public void dispose() {
        if (!isClosing) {
            handleGameEnd();
        } else {
            stopTimers();
            super.dispose();
        }
    }
}

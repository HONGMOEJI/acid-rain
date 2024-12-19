package client.ui.game;

import client.app.GameClient;
import client.event.GameEventListener;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import client.ui.components.GameTextField;
import game.model.Word;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GameScreen extends JFrame implements GameEventListener {
    private static final Logger logger = Logger.getLogger(GameScreen.class.getName());
    private final GameClient client;
    private final String roomId;
    private final JFrame mainFrame;

    private JPanel gamePanel;
    private GameTextField inputField;
    private JLabel scoreLabel;
    private JLabel phLabel;
    private JProgressBar phMeter;
    private JLabel opponentScoreLabel;
    private Timer screenRefreshTimer;

    private List<Word> activeWords = new ArrayList<>();
    private boolean isBlinded = false;
    private long blindEndTime = 0;
    private int myScore = 0;
    private int opponentScore = 0;
    private double myPH = 7.0;
    private final String myName;
    private final String opponentName;
    private volatile boolean isClosing = false;

    public GameScreen(GameClient client, String roomId, String myName, String opponentName) {
        this.client = client;
        this.roomId = roomId;
        this.myName = myName;
        this.opponentName = opponentName;
        this.mainFrame = (JFrame) SwingUtilities.getWindowAncestor(this);

        client.setEventListener(this);
        initializeFrame();
        setupUI();
        setupInput();
        setupTimers();
        setVisible(true);
    }

    private void initializeFrame() {
        setTitle("Typing Game - " + myName + " vs " + opponentName);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 700);
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

        phMeter = new JProgressBar(0, 70);
        phMeter.setValue(70);
        phMeter.setPreferredSize(new Dimension(150, 20));
        phMeter.setForeground(ColorScheme.PRIMARY);
        phMeter.setBackground(ColorScheme.BACKGROUND);

        leftPanel.add(phLabel);
        leftPanel.add(phMeter);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setOpaque(false);
        scoreLabel = new JLabel("점수: 0");
        scoreLabel.setFont(FontManager.getFont(20f));
        scoreLabel.setForeground(ColorScheme.TEXT);
        centerPanel.add(scoreLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        opponentScoreLabel = new JLabel("상대방 점수: 0");
        opponentScoreLabel.setFont(FontManager.getFont(16f));
        opponentScoreLabel.setForeground(ColorScheme.TEXT);
        rightPanel.add(opponentScoreLabel);

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
                client.sendGameAction(roomId, "WORD_INPUT", input);
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
        screenRefreshTimer = new Timer(1000/60, e -> refreshScreen());
        screenRefreshTimer.start();
    }

    private void drawGame(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (isBlinded && System.currentTimeMillis() < blindEndTime) {
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            return;
        } else if (isBlinded && System.currentTimeMillis() >= blindEndTime) {
            isBlinded = false;
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getFont(16f));
        for (Word word : activeWords) {
            g2d.drawString(word.getText(), word.getX(), word.getY());
        }
    }

    private void refreshScreen() {
        if (!isClosing) {
            for (Word word : new ArrayList<>(activeWords)) {
                word.setY(word.getY() + 2);
                if (word.getY() > gamePanel.getHeight()) {
                    activeWords.remove(word);
                    break;
                }
            }
            updateGameInfo();
            gamePanel.repaint();
        }
    }

    private void updateGameInfo() {
        scoreLabel.setText(String.format("점수: %d", myScore));
        phLabel.setText(String.format("pH: %.1f", myPH));
        int phValue = (int)(myPH * 10);
        phMeter.setValue(phValue);

        if (myPH < 5.0) {
            phMeter.setForeground(Color.RED);
        } else if (myPH < 6.0) {
            phMeter.setForeground(Color.ORANGE);
        } else {
            phMeter.setForeground(ColorScheme.PRIMARY);
        }

        opponentScoreLabel.setText(String.format("상대방 점수: %d", opponentScore));
    }

    private void handleGameEnd() {
        if (isClosing) return;

        int option = JOptionPane.showConfirmDialog(this,
                "정말로 게임을 종료하시겠습니까?",
                "게임 종료",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            isClosing = true;

            if (screenRefreshTimer != null) {
                screenRefreshTimer.stop();
            }

            client.sendMessage("LEAVE_ROOM|" + roomId);
            client.setEventListener(null);

            if (mainFrame != null) {
                mainFrame.setVisible(true);
            }

            dispose();
        }
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        if (isClosing) return;

        switch (eventType) {
            case "WORD_SPAWNED":
                activeWords.add(new Word((String)data[0], (int)data[1], 0));
                break;

            case "WORD_MATCHED":
                String wordText = (String) data[0];
                String playerName = (String) data[1];
                int newScore = (int) data[2];
                if (playerName.equals(myName)) {
                    myScore = newScore;
                } else {
                    opponentScore = newScore;
                }
                activeWords.removeIf(w -> w.getText().equals(wordText));
                break;

            case "WORD_MISSED":
                String missedWord = (String) data[0];
                String playerNameMissed = (String) data[1];
                if (playerNameMissed.equals(myName)) {
                    myPH = (double) data[2];
                }
                activeWords.removeIf(w -> w.getText().equals(missedWord));
                break;

            case "BLIND_EFFECT":
                String targetPlayer = (String) data[0];
                int durationMs = (int) data[1];
                if (targetPlayer.equals(myName)) {
                    isBlinded = true;
                    blindEndTime = System.currentTimeMillis() + durationMs;
                }
                break;

            case "GAME_OVER":
                handleGameOver((String)data[0], (int)data[1], (int)data[2]);
                break;

            case "ROOM_CLOSED":
                handleRoomClosed((String)data[1]);
                break;
        }
    }

    private void handleGameOver(String winner, int finalMyScore, int finalOppScore) {
        if (isClosing) return;

        isClosing = true;
        if (screenRefreshTimer != null) {
            screenRefreshTimer.stop();
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    (winner.equals(myName) ? "승리!" : "패배...") +
                            "\n내 점수: " + finalMyScore +
                            "\n상대 점수: " + finalOppScore,
                    "게임 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            client.setEventListener(null);
            if (mainFrame != null) {
                mainFrame.setVisible(true);
            }
            dispose();
        });
    }

    private void handleRoomClosed(String reason) {
        if (isClosing) return;

        isClosing = true;
        if (screenRefreshTimer != null) {
            screenRefreshTimer.stop();
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "방이 닫혔습니다: " + reason,
                    "게임 종료",
                    JOptionPane.INFORMATION_MESSAGE);

            client.setEventListener(null);
            if (mainFrame != null) {
                mainFrame.setVisible(true);
            }
            dispose();
        });
    }

    @Override
    public void dispose() {
        if (!isClosing) {
            handleGameEnd();
        } else {
            super.dispose();
        }
    }
}
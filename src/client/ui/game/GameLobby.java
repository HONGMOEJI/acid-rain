// client/ui/game/GameLobby.java
package client.ui.game;

import client.app.GameClient;
import client.event.GameEvent;
import client.event.GameEventListener;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import client.ui.theme.StyleManager;
import client.ui.components.GameTextField;
import client.ui.components.RetroButton;
import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.GameRoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameLobby extends JFrame implements GameEventListener {
    private final GameRoom room;
    private final GameClient client;
    private final JFrame mainMenu;
    private JTextArea chatArea;
    private GameTextField chatInput;
    private JLabel statusLabel;
    private JComboBox<GameMode> gameModeCombo;
    private JComboBox<DifficultyLevel> difficultyCombo;
    private final boolean isHost;

    public GameLobby(GameRoom room, GameClient client, JFrame mainMenu) {
        this.room = room;
        this.client = client;
        this.mainMenu = mainMenu;
        this.isHost = client.getUsername().equals(room.getHostName());

        client.setEventListener(this);
        initializeFrame();
        setupUI();
        setupKeyboardShortcuts();
        setupWindowListeners();

        mainMenu.setVisible(false);
    }

    private void initializeFrame() {
        setTitle("게임 대기실 - " + room.getRoomName());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // 메인 패널 설정
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(ColorScheme.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);
    }

    private void setupUI() {
        // 상단 상태 패널
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.NORTH);

        // 중앙 분할 패널 (설정 + 채팅)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createSettingsPanel(),
                createChatPanel());
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(250);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);

        statusLabel = new JLabel(getStatusText());
        statusLabel.setForeground(ColorScheme.TEXT);
        statusLabel.setFont(FontManager.getFont(16f));
        panel.add(statusLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorScheme.BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 게임 모드 설정
        gbc.gridx = 0; gbc.gridy = 0;
        addSettingsLabel("게임 모드:", panel, gbc);

        gameModeCombo = new JComboBox<>(GameMode.values());
        gameModeCombo.setSelectedItem(room.getGameMode());
        gameModeCombo.setEnabled(isHost);
        StyleManager.applyComboBoxStyle(gameModeCombo);
        gbc.gridy = 1;
        panel.add(gameModeCombo, gbc);

        // 난이도 설정
        gbc.gridy = 2;
        addSettingsLabel("난이도:", panel, gbc);

        difficultyCombo = new JComboBox<>(DifficultyLevel.values());
        difficultyCombo.setSelectedItem(room.getDifficulty());
        difficultyCombo.setEnabled(isHost);
        StyleManager.applyComboBoxStyle(difficultyCombo);
        gbc.gridy = 3;
        panel.add(difficultyCombo, gbc);

        if (isHost) {
            setupHostEventListeners();
        }

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(ColorScheme.BACKGROUND);

        // 채팅 영역
        chatArea = new JTextArea();
        StyleManager.applyChatAreaStyle(chatArea);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));
        panel.add(scrollPane, BorderLayout.CENTER);

        // 채팅 입력
        chatInput = new GameTextField();
        chatInput.addActionListener(e -> sendChat());
        panel.add(chatInput, BorderLayout.SOUTH);

        return panel;
    }

    private void setupHostEventListeners() {
        gameModeCombo.addActionListener(e ->
                client.sendMessage("UPDATE_SETTINGS|" + room.getRoomId() +
                        "|MODE|" + gameModeCombo.getSelectedItem())
        );

        difficultyCombo.addActionListener(e ->
                client.sendMessage("UPDATE_SETTINGS|" + room.getRoomId() +
                        "|DIFFICULTY|" + difficultyCombo.getSelectedItem())
        );
    }

    private void setupKeyboardShortcuts() {
        // 채팅 토글 (Ctrl + C)
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
                "toggleChat"
        );
        getRootPane().getActionMap().put("toggleChat",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toggleChat();
                    }
                }
        );

        // 게임 시작 (Ctrl + S) - 방장만
        if (isHost) {
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                    "startGame"
            );
            getRootPane().getActionMap().put("startGame",
                    new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            startGame();
                        }
                    }
            );
        }
    }

    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                leaveRoom();
            }
        });
    }

    private void addSettingsLabel(String text, JPanel panel, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.TEXT);
        label.setFont(FontManager.getFont(14f));
        panel.add(label, gbc);
    }

    private void toggleChat() {
        chatInput.setEnabled(!chatInput.isEnabled());
        addChatMessage("시스템", chatInput.isEnabled() ?
                "채팅이 활성화되었습니다." : "채팅이 비활성화되었습니다.");
        if (chatInput.isEnabled()) {
            chatInput.requestFocus();
        }
    }

    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            client.sendMessage("CHAT|" + room.getRoomId() + "|" + message);
            chatInput.setText("");
        }
    }

    private void startGame() {
        if (room.canStart()) {
            client.sendMessage("START_GAME|" + room.getRoomId());
        } else {
            JOptionPane.showMessageDialog(this,
                    "게임을 시작하려면 모든 플레이어가 참가해야 합니다.",
                    "게임 시작 불가",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void leaveRoom() {
        client.sendMessage("LEAVE_ROOM|" + room.getRoomId());
        dispose();
    }

    private String getStatusText() {
        if (isHost && room.getCurrentPlayers() == room.getMaxPlayers()) {
            return "Ctrl + S를 눌러 게임을 시작하세요!";
        }
        return String.format("플레이어 대기 중... (%d/%d)",
                room.getCurrentPlayers(), room.getMaxPlayers());
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        SwingUtilities.invokeLater(() -> {
            switch (eventType) {
                case GameEvent.PLAYER_UPDATED:
                    handlePlayerUpdate(data);
                    break;
                case GameEvent.CHAT_RECEIVED:
                    handleChatReceived(data);
                    break;
                case GameEvent.SETTINGS_UPDATED:
                    handleSettingsUpdate(data);
                    break;
                case GameEvent.GAME_STARTED:
                    handleGameStart();
                    break;
            }
        });
    }

    private void handlePlayerUpdate(Object... data) {
        if (data.length >= 2) {
            room.setCurrentPlayers((int) data[1]);
            statusLabel.setText(getStatusText());
        }
    }

    private void handleChatReceived(Object... data) {
        if (data.length >= 2) {
            addChatMessage((String) data[0], (String) data[1]);
        }
    }

    private void handleSettingsUpdate(Object... data) {
        if (data.length >= 2) {
            gameModeCombo.setSelectedItem(data[0]);
            difficultyCombo.setSelectedItem(data[1]);
            room.setGameMode((GameMode) data[0]);
            room.setDifficulty((DifficultyLevel) data[1]);
        }
    }

    private void handleGameStart() {
        JOptionPane.showMessageDialog(this, "게임이 시작됩니다!");
        dispose();
    }

    private void addChatMessage(String username, String message) {
        chatArea.append(String.format("%s: %s%n", username, message));
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    @Override
    public void dispose() {
        mainMenu.setVisible(true);
        super.dispose();
    }
}
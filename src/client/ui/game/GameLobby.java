/*
 * client.ui.game.GameLobby.java
 * 게임 로비 화면을 정의하는 클래스, 게임 시작 전 플레이어들이 모이는 곳, 채팅 가능
 */

package client.ui.game;

import client.app.GameClient;
import client.event.GameEvent;
import client.event.GameEvent.*;
import client.event.GameEvent.ClientEvent;
import client.event.GameEventListener;
import client.ui.MainMenu;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import client.ui.components.GameTextField;
import client.ui.components.RetroButton;
import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.GameRoom;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class GameLobby extends JFrame implements GameEventListener {
    private static final Logger logger = Logger.getLogger(GameLobby.class.getName());

    private final GameRoom room;
    private final GameClient client;
    private final JFrame mainFrame;
    private JTextArea chatArea;
    private GameTextField chatInput;
    private JLabel statusLabel;
    private JComboBox<GameMode> gameModeCombo;
    private JComboBox<DifficultyLevel> difficultyCombo;
    private JPanel playerListPanel;
    private RetroButton startButton;
    private JPanel buttonPanel;
    private volatile boolean isClosing = false;

    public GameLobby(GameRoom room, GameClient client, JFrame mainFrame) {
        this.room = room;
        this.client = client;
        this.mainFrame = mainFrame;

        // UI 초기화 및 단축키 설정
        initializeFrame();
        setupUI();
        setupKeyboardShortcuts();
        setupWindowListeners();
        updateTitle();

        // 초기 플레이어 목록 표시
        if (room.getPlayers() != null && room.getPlayers().length > 0) {
            updatePlayerList(room.getPlayers());
        }

        // 이벤트 리스너 설정
        client.setEventListener(this);

        // 서버에 플레이어 목록 요청
        // 서버에 플레이어 목록 요청
        Thread initThread = new Thread(() -> {
            try {
                // 서버 연결 안정화를 위한 대기
                Thread.sleep(200);
                client.sendPlayerListRequest(room.getRoomId());
                logger.info("플레이어 목록 요청 전송: " + room.getRoomId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("플레이어 목록 요청 중 인터럽트 발생");
            } catch (Exception e) {
                logger.severe("플레이어 목록 요청 실패: " + e.getMessage());
            }
        });
        initThread.start();

        setVisible(true);
        logger.info("게임 로비 초기화 완료: " + room.getRoomName());
    }

    private void initializeFrame() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(ColorScheme.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);
    }

    private void setupUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);

        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);

        JPanel rightPanel = createRightPanel();
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
        buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ColorScheme.BACKGROUND);
        panel.add(createSettingsPanel(), BorderLayout.NORTH);
        playerListPanel = createPlayerListPanel();
        panel.add(playerListPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ColorScheme.BACKGROUND);

        statusLabel = new JLabel(getStatusText(), SwingConstants.CENTER);
        statusLabel.setFont(FontManager.getFont(16f));
        statusLabel.setForeground(ColorScheme.TEXT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(statusLabel, BorderLayout.NORTH);

        panel.add(createChatPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorScheme.SECONDARY);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY),
                "게임 설정",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                FontManager.getFont(14f),
                ColorScheme.TEXT
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        addSettingsLabel("프로그래밍 언어:", panel, gbc);

        gbc.gridy = 1;
        gameModeCombo = new JComboBox<>(GameMode.values());
        gameModeCombo.setSelectedItem(room.getGameMode());
        gameModeCombo.setEnabled(isHost());
        stylizeComboBox(gameModeCombo);
        panel.add(gameModeCombo, gbc);

        gbc.gridy = 2;
        addSettingsLabel("난이도:", panel, gbc);

        gbc.gridy = 3;
        difficultyCombo = new JComboBox<>(DifficultyLevel.values());
        difficultyCombo.setSelectedItem(room.getDifficulty());
        difficultyCombo.setEnabled(isHost());
        stylizeComboBox(difficultyCombo);
        panel.add(difficultyCombo, gbc);

        if (isHost()) {
            setupHostEventListeners();
        }

        return panel;
    }

    // 게임 설정 업데이트 요청
    private void setupHostEventListeners() {
        gameModeCombo.addActionListener(e -> {
            if (!isClosing && e.getSource() == gameModeCombo) {
                GameMode selectedMode = (GameMode) gameModeCombo.getSelectedItem();
                client.sendUpdateSettingsRequest(room.getRoomId(), "MODE", selectedMode.getDisplayName());
            }
        });

        difficultyCombo.addActionListener(e -> {
            if (!isClosing && e.getSource() == difficultyCombo) {
                DifficultyLevel selectedDifficulty = (DifficultyLevel) difficultyCombo.getSelectedItem();
                client.sendUpdateSettingsRequest(room.getRoomId(), "DIFFICULTY", selectedDifficulty.getDisplayName());
            }
        });
    }

    private JPanel createPlayerListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.SECONDARY);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY),
                "참가자 목록",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                FontManager.getFont(14f),
                ColorScheme.TEXT
        ));
        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(ColorScheme.BACKGROUND);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(ColorScheme.SECONDARY);
        chatArea.setForeground(ColorScheme.TEXT);
        chatArea.setFont(FontManager.getFont(14f));
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 스크롤바 스타일 설정
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ColorScheme.PRIMARY;
                this.trackColor = ColorScheme.SECONDARY;
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(ColorScheme.BACKGROUND);

        chatInput = new GameTextField();
        chatInput.addActionListener(e -> sendChat());

        RetroButton sendButton = new RetroButton("전송");
        sendButton.addActionListener(e -> sendChat());

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBackground(ColorScheme.BACKGROUND);

        if (isHost()) {
            startButton = new RetroButton("게임 시작 (F5)");
            startButton.addActionListener(e -> startGame());
            startButton.setEnabled(room.canStart());
            panel.add(startButton);
        }

        RetroButton leaveButton = new RetroButton(isHost() ? "방 닫기 (ESC)" : "나가기 (ESC)");
        leaveButton.addActionListener(e -> handleLeaveRoom());
        panel.add(leaveButton);

        return panel;
    }

    private void setupKeyboardShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // ESC - 나가기
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "leave");
        actionMap.put("leave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLeaveRoom();
            }
        });

        // F5 - 게임 시작 (방장만)
        if (isHost()) {
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "start");
            actionMap.put("start", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    startGame();
                }
            });
        }
    }

    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLeaveRoom();
            }
        });
    }

    /**
     * 플레이어 목록을 UI에 업데이트하는 메서드
     * 모든 플레이어를 표시하며 방장은 왕관 이모지(👑)로 구분
     *
     * @param players 업데이트할 플레이어 배열
     * @throws IllegalStateException 이 메서드가 이벤트 디스패치 스레드가 아닌 스레드에서 호출될 경우
     */
    private void updatePlayerList(String[] players) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updatePlayerList(players));
            return;
        }

        if (players == null || players.length == 0) {
            logger.warning("플레이어 목록이 비어있습니다.");
            return;
        }

        logger.info("플레이어 목록 업데이트: " + Arrays.toString(players));

        playerListPanel.removeAll();
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(ColorScheme.SECONDARY);

        for (String player : players) {
            JPanel playerPanel = new JPanel(new BorderLayout());
            playerPanel.setBackground(ColorScheme.SECONDARY);
            playerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            JLabel playerLabel = new JLabel();
            playerLabel.setFont(FontManager.getFont(14f));
            playerLabel.setForeground(ColorScheme.TEXT);

            if (player.equals(room.getHostName())) {
                playerLabel.setText(String.format("<html>%s 👑</html>", player));
            } else {
                playerLabel.setText(player);
            }

            playerPanel.add(playerLabel);
            listPanel.add(playerPanel);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        playerListPanel.add(scrollPane, BorderLayout.CENTER);

        playerListPanel.revalidate();
        playerListPanel.repaint();

        updateStatus();
    }

    private void updateStatus() {
        statusLabel.setText(getStatusText());
        if (startButton != null) {
            startButton.setEnabled(room.canStart());
        }
    }

    private void updateTitle() {
        SwingUtilities.invokeLater(() -> {
            setTitle(String.format("[%s] %s - %s",
                    room.getGameMode().getDisplayName(),
                    room.getRoomName(),
                    isHost() ? "방장" : "참가자"));
        });
    }

    /**
     * 새로운 방장 지정 시 UI를 업데이트하는 메서드
     *
     * @param isNewHost 새 방장 여부
     * - true: 게임 시작 버튼 추가, 설정 변경 가능
     * - false: 게임 시작 버튼 제거, 설정 변경 불가
     */
    private void updateUIForHostStatus(boolean isNewHost) {
        SwingUtilities.invokeLater(() -> {
            gameModeCombo.setEnabled(isNewHost);
            difficultyCombo.setEnabled(isNewHost);

            buttonPanel.removeAll();

            if (isNewHost) {
                startButton = new RetroButton("게임 시작 (F5)");
                startButton.addActionListener(e -> startGame());
                startButton.setEnabled(room.canStart());
                buttonPanel.add(startButton);

                RetroButton leaveButton = new RetroButton("방 닫기 (ESC)");
                leaveButton.addActionListener(e -> handleLeaveRoom());
                buttonPanel.add(leaveButton);
            } else {
                RetroButton leaveButton = new RetroButton("나가기 (ESC)");
                leaveButton.addActionListener(e -> handleLeaveRoom());
                buttonPanel.add(leaveButton);
            }

            buttonPanel.revalidate();
            buttonPanel.repaint();
            updateTitle();
        });
    }

    private void sendChat() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            client.sendRoomChatMessage(room.getRoomId(), message);
            chatInput.setText("");
            chatInput.requestFocus();
        }
    }

    /**
     * 게임 시작 요청을 처리하는 메서드
     * 시작 조건을 검증하고 사용자 확인을 거친 후 게임 시작
     *
     * - 최소 2명의 플레이어가 필요
     * - 방장만 시작 가능
     * - 시작 전 확인 대화상자 표시
     */
    private void startGame() {
        if (!room.canStart()) {
            JOptionPane.showMessageDialog(this,
                    "최소 두 명의 플레이어가 필요합니다.",
                    "게임 시작 불가",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int option = JOptionPane.showConfirmDialog(this,
                "게임을 시작하시겠습니까?",
                "게임 시작",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            client.sendGameStartRequest(room.getRoomId());
        }
    }

    /**
     * 방 나가기/닫기 처리를 담당하는 메서드
     * 방장과 일반 참가자를 구분하여 다른 메시지 표시
     *
     * - 방장: "방을 닫으시겠습니까?"
     * - 참가자: "방에서 나가시겠습니까?"
     * - 확인 시 서버에 나가기 요청 전송
     * - MainMenu로 화면 전환
     */
    private void handleLeaveRoom() {
        if (isClosing) return;

        String message = isHost() ? "방을 닫으시겠습니까?" : "방에서 나가시겠습니까?";
        int option = JOptionPane.showConfirmDialog(this,
                message,
                "확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            isClosing = true;

            // 방 나가기 처리
            client.sendLeaveRoomRequest(room.getRoomId());

            // MainMenu로 전환
            transitionToMainMenu();
        }
    }

    private void handleGameStart(Object... data) {
        String[] players = room.getPlayers();
        if (data.length >= 2 && data[1] instanceof String[]) {
            players = (String[]) data[1];
            room.setPlayers(players);
        }

        GameScreen gameScreen = new GameScreen(client, room.getRoomId(), client.getUsername(), players, mainFrame);
        isClosing = true;
        super.dispose();
        gameScreen.setVisible(true);
    }

    private boolean isHost() {
        return client.getUsername().equals(room.getHostName());
    }

    private String getStatusText() {
        if (isHost() && room.canStart()) {
            return "F5를 눌러 게임을 시작하세요!";
        }
        return String.format("대기 중... (%d/%d)",
                room.getCurrentPlayers(), room.getMaxPlayers());
    }

    private void addSettingsLabel(String text, JPanel panel, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.getFont(14f));
        label.setForeground(ColorScheme.TEXT);
        panel.add(label, gbc);
    }

    private void stylizeComboBox(JComboBox<?> comboBox) {
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                        index, isSelected, cellHasFocus);
                label.setFont(FontManager.getFont(14f));
                label.setForeground(ColorScheme.TEXT);
                label.setBackground(isSelected ? ColorScheme.PRIMARY : ColorScheme.SECONDARY);
                label.setOpaque(true);
                return label;
            }
        });
        comboBox.setBackground(ColorScheme.SECONDARY);
        comboBox.setForeground(ColorScheme.TEXT);
        comboBox.setFont(FontManager.getFont(14f));
        comboBox.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));
    }

    /**
     * 게임 로비에서 발생하는 각종 이벤트를 처리하는 메서드
     *
     * @param eventType 발생한 이벤트의 타입 (예: 플레이어 업데이트, 채팅 수신 등)
     * @param data 이벤트와 관련된 데이터를 담은 가변 인자 배열
     *            - 플레이어 업데이트: roomId, playerCount, players[]
     *            - 채팅 수신: username, message
     *            - 설정 업데이트: roomId, gameMode, difficulty
     */
    @Override
    public void onGameEvent(String eventType, Object... data) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onGameEvent(eventType, data));
            return;
        }

        try {
            switch (eventType) {
                case ClientEvent.PLAYER_UPDATED -> handlePlayerUpdate(data);
                case ClientEvent.CHAT_RECEIVED -> handleChatReceived(data);
                case ClientEvent.SETTINGS_UPDATED -> handleSettingsUpdate(data);
                case ClientEvent.GAME_STARTED -> handleGameStart(data);
                case ClientEvent.HOST_LEFT -> handleHostLeft(data);
                case ClientEvent.NEW_HOST -> handleNewHost(data);
                case ClientEvent.ROOM_CLOSED -> handleRoomClosed(data);
                case ClientEvent.ERROR_OCCURRED -> handleError(data);
            }
        } catch (Exception e) {
            logger.severe("이벤트 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerUpdate(Object... data) {
        if (data.length >= 3) {
            String roomId = (String) data[0];
            int playerCount = (int) data[1];

            if (!roomId.equals(room.getRoomId())) {
                return;
            }

            room.setCurrentPlayers(playerCount);

            if (data.length >= 3 && data[2] instanceof String[]) {
                String[] players = (String[]) data[2];
                room.setPlayers(players);
                updatePlayerList(players);
            }
        }
    }

    private void handleChatReceived(Object... data) {
        if (data.length >= 2) {
            String username = (String) data[0];
            String message = (String) data[1];
            chatArea.append(String.format("[%s] %s%n", username, message));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    private void handleSettingsUpdate(Object... data) {
        if (data.length >= 3) {
            String roomId = (String) data[0];
            GameMode gameMode = GameMode.valueOf((String) data[1]);
            DifficultyLevel difficulty = DifficultyLevel.valueOf((String) data[2]);

            if (!isHost()) {
                gameModeCombo.setSelectedItem(gameMode);
                difficultyCombo.setSelectedItem(difficulty);
            }

            room.setGameMode(gameMode);
            room.setDifficulty(difficulty);
            updateTitle();
        }
    }

    private void handleHostLeft(Object... data) {
        if (data.length >= 2) {
            String roomId = (String) data[0];
            String message = (String) data[1];

            if (!isHost()) {
                JOptionPane.showMessageDialog(this,
                        message,
                        "알림",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void handleNewHost(Object... data) {
        if (data.length >= 2) {
            String roomId = (String) data[0];
            String newHostName = (String) data[1];

            boolean wasHost = isHost();
            room.setHostName(newHostName);
            boolean isNewHost = client.getUsername().equals(newHostName);

            if (isNewHost && !wasHost) {
                updateUIForHostStatus(true);
                JOptionPane.showMessageDialog(this,
                        "당신이 새로운 방장이 되었습니다.",
                        "알림",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            updateTitle();
            updatePlayerList(room.getPlayers());
        }
    }

    private void handleRoomClosed(Object... data) {
        if (!isClosing) {
            isClosing = true;

            // 메시지 표시
            if (data.length >= 2) {
                String roomId = (String) data[0];
                String reason = (String) data[1];

                JOptionPane.showMessageDialog(this,
                        reason,
                        "방 종료",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            // MainMenu로 전환
            transitionToMainMenu();
        }
    }

    private void transitionToMainMenu() {
        SwingUtilities.invokeLater(() -> {
            // MainMenu 생성 및 설정
            MainMenu mainMenu = new MainMenu(client);
            client.setEventListener(mainMenu);

            // mainFrame 업데이트
            if (mainFrame != null) {
                mainFrame.getContentPane().removeAll();
                mainFrame.add(mainMenu);
                mainFrame.setSize(800, 600);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.revalidate();
                mainFrame.repaint();
                mainFrame.setVisible(true);
            }

            // GameLobby 창 닫기
            dispose();
        });
    }

    private void handleError(Object... data) {
        if (data.length >= 1) {
            String errorMessage = (String) data[0];
            JOptionPane.showMessageDialog(this,
                    errorMessage,
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        if (!isClosing) {
            handleLeaveRoom();
        } else {
            client.setEventListener(null);
            super.dispose();
        }
    }
}

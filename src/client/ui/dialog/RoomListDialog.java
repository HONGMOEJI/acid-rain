package client.ui.dialog;

import client.app.GameClient;
import client.event.GameEvent;
import client.event.GameEventListener;
import client.ui.components.RetroButton;
import client.ui.game.GameLobby;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.GameRoom;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RoomListDialog extends BaseDialog implements GameEventListener {
    private final DefaultListModel<RoomListItem> roomListModel;
    private final JList<RoomListItem> roomList;
    private final GameClient client;
    private final JFrame mainFrame;
    private List<GameRoom> rooms = new ArrayList<>();
    private Timer refreshTimer;
    private JLabel statusLabel;
    private boolean isClosing = false;

    public RoomListDialog(JFrame mainFrame, GameClient client) {
        super(mainFrame, "게임 방 목록");
        this.mainFrame = mainFrame;
        this.client = client;

        mainFrame.setVisible(false);
        this.client.setEventListener(this);

        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);

        setupDialog();
        setupUI();
        setupRefreshTimer();
        setupWindowListener();

        client.sendMessage("ROOM_LIST");
    }

    private void setupDialog() {
        setSize(1000, 700);
        setLocationRelativeTo(getOwner());
        setResizable(true);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }

    private void setupUI() {
        mainPanel.setLayout(new BorderLayout(0, 20));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(ColorScheme.BACKGROUND);
        topPanel.add(createHeaderPanel(), BorderLayout.CENTER);

        statusLabel = new JLabel("방 목록을 불러오는 중...");
        statusLabel.setFont(FontManager.getFont(14f));
        statusLabel.setForeground(ColorScheme.TEXT);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(createListPanel(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);

        JLabel titleLabel = new JLabel("게임 방 목록");
        titleLabel.setFont(FontManager.getFont(28f));
        titleLabel.setForeground(ColorScheme.TEXT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);

        roomList.setBackground(ColorScheme.SECONDARY);
        roomList.setForeground(ColorScheme.TEXT);
        roomList.setSelectionBackground(ColorScheme.PRIMARY);
        roomList.setSelectionForeground(ColorScheme.TEXT);
        roomList.setFont(FontManager.getFont(16f));
        roomList.setCellRenderer(new RoomListCellRenderer());
        roomList.setFixedCellHeight(60);

        roomList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY, 2));
        scrollPane.setBackground(ColorScheme.BACKGROUND);
        scrollPane.getViewport().setBackground(ColorScheme.SECONDARY);

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ColorScheme.PRIMARY;
                this.trackColor = ColorScheme.SECONDARY;
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panel.setBackground(ColorScheme.BACKGROUND);

        RetroButton refreshButton = new RetroButton("새로고침 (F5)");
        RetroButton createButton = new RetroButton("방 만들기 (F2)");
        RetroButton joinButton = new RetroButton("입장 (Enter)");
        RetroButton backButton = new RetroButton("돌아가기 (ESC)");

        refreshButton.addActionListener(e -> refreshRoomList());
        createButton.addActionListener(e -> showCreateRoomDialog());
        joinButton.addActionListener(e -> joinSelectedRoom());
        backButton.addActionListener(e -> handleClose());

        setupKeyboardShortcuts(refreshButton, createButton, joinButton, backButton);

        panel.add(refreshButton);
        panel.add(createButton);
        panel.add(joinButton);
        panel.add(backButton);

        return panel;
    }

    private void setupKeyboardShortcuts(JButton refreshButton, JButton createButton,
                                        JButton joinButton, JButton backButton) {
        getRootPane().registerKeyboardAction(
                e -> refreshButton.doClick(),
                KeyStroke.getKeyStroke("F5"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        getRootPane().registerKeyboardAction(
                e -> createButton.doClick(),
                KeyStroke.getKeyStroke("F2"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        getRootPane().registerKeyboardAction(
                e -> joinButton.doClick(),
                KeyStroke.getKeyStroke("ENTER"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        getRootPane().registerKeyboardAction(
                e -> backButton.doClick(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void setupRefreshTimer() {
        refreshTimer = new Timer(30000, e -> refreshRoomList());
        refreshTimer.start();
    }

    private void setupWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
    }

    public void refreshRoomList() {
        statusLabel.setText("방 목록을 새로고치는 중...");
        client.sendMessage("ROOM_LIST");
    }

    private void showCreateRoomDialog() {
        CreateRoomDialog dialog = new CreateRoomDialog(this);
        dialog.setVisible(true);

        if (dialog.isRoomCreated()) {
            client.sendCreateRoomRequest(dialog.getCreatedRoom());
        }
    }

    private void joinSelectedRoom() {
        RoomListItem selectedItem = roomList.getSelectedValue();
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this,
                    "입장할 방을 선택해주세요.",
                    "알림",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        GameRoom selectedRoom = findRoomById(selectedItem.getRoomId());
        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this,
                    "선택한 방을 찾을 수 없습니다. 방 목록을 새로고침해주세요.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            refreshRoomList();
            return;
        }

        if (selectedRoom.isFull()) {
            JOptionPane.showMessageDialog(this,
                    "방이 가득 찼습니다.",
                    "입장 불가",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedRoom.isPasswordRequired()) {
            showPasswordDialog(selectedRoom);
        } else {
            client.sendJoinRoomRequest(selectedRoom.getRoomId(), null);
        }
    }

    private void showPasswordDialog(GameRoom room) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(250, 80));

        JLabel label = new JLabel("비밀번호를 입력하세요:");
        label.setFont(FontManager.getFont(14f));
        panel.add(label, BorderLayout.NORTH);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(FontManager.getFont(14f));
        panel.add(passwordField, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(this,
                panel,
                "비밀번호 입력",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String password = new String(passwordField.getPassword());
            if (!password.isEmpty()) {
                client.sendJoinRoomRequest(room.getRoomId(), password);
            } else {
                JOptionPane.showMessageDialog(this,
                        "비밀번호를 입력해주세요.",
                        "알림",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleClose() {
        if (!isClosing) {
            isClosing = true;
            if (refreshTimer != null) {
                refreshTimer.stop();
            }
            client.setEventListener(null);
            mainFrame.setVisible(true);
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (!isClosing) {
            handleClose();
        } else {
            super.dispose();
        }
    }

    private GameRoom findRoomById(String roomId) {
        return rooms.stream()
                .filter(room -> room.getRoomId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        SwingUtilities.invokeLater(() -> {
            switch (eventType) {
                case GameEvent.ROOM_LIST_UPDATED:
                    handleRoomListUpdate(data);
                    break;
                case GameEvent.ROOM_JOINED:
                    handleRoomJoined(data);
                    break;
                case GameEvent.ROOM_CREATED:
                    handleRoomCreated(data);
                    break;
                case GameEvent.ERROR_OCCURRED:
                    handleError((String) data[0]);
                    break;
            }
        });
    }

    private void handleRoomListUpdate(Object... data) {
        roomListModel.clear();
        rooms.clear();

        if (data.length > 0) {
            String[] roomInfos;
            if (data[0] instanceof String[]) {
                roomInfos = (String[]) data[0];
            } else if (data[0] instanceof String) {
                roomInfos = new String[]{(String) data[0]};
            } else {
                return;
            }

            for (String roomInfo : roomInfos) {
                try {
                    GameRoom room = GameRoom.fromString(roomInfo);
                    if (room != null) {
                        rooms.add(room);
                        roomListModel.addElement(new RoomListItem(room));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        updateStatusLabel();
        roomList.revalidate();
        roomList.repaint();
    }

    private void updateStatusLabel() {
        String status = String.format("총 %d개의 방이 있습니다.", rooms.size());
        if (rooms.isEmpty()) {
            status = "현재 생성된 방이 없습니다. 새로운 방을 만들어보세요!";
        }
        statusLabel.setText(status);
    }

    private void handleRoomJoined(Object... data) {
        boolean success = (boolean) data[0];
        String message = (String) data[1];

        if (success && data.length >= 3) {
            try {
                String roomInfoStr = (String) data[2];
                GameRoom joinedRoom = GameRoom.fromString(roomInfoStr);

                if (joinedRoom != null) {
                    setVisible(false);  // 방 목록 숨기기
                    new GameLobby(joinedRoom, client, mainFrame);
                } else {
                    throw new Exception("방 정보 변환 실패");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "방 입장 처리 중 오류가 발생했습니다: " + e.getMessage(),
                        "입장 실패",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    message,
                    "입장 실패",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRoomCreated(Object... data) {
        boolean success = (boolean) data[0];
        String message = (String) data[1];

        if (!success) {
            JOptionPane.showMessageDialog(this,
                    message,
                    "방 생성 실패",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleError(String errorMessage) {
        JOptionPane.showMessageDialog(this,
                errorMessage,
                "오류",
                JOptionPane.ERROR_MESSAGE);
    }

    private static class RoomListItem {
        private final String roomId;
        private final String displayText;

        public RoomListItem(GameRoom room) {
            this.roomId = room.getRoomId();
            this.displayText = String.format("[%s] %s %s (%d/%d) - %s - %s",
                    room.getHostName(),
                    room.getRoomName(),
                    room.isPasswordRequired() ? "🔒" : "",
                    room.getCurrentPlayers(),
                    room.getMaxPlayers(),
                    room.getGameMode().getDisplayName(),
                    room.getDifficulty().getDisplayName());
        }

        public String getRoomId() {
            return roomId;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    private class RoomListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);

            label.setFont(FontManager.getFont(16f));
            label.setBorder(new EmptyBorder(10, 15, 10, 15));

            RoomListItem item = (RoomListItem) value;
            GameRoom room = findRoomById(item.getRoomId());
            if (room != null && room.isFull() && !isSelected) {
                label.setForeground(ColorScheme.TEXT.darker());
            }

            if (!isSelected) {
                label.setBackground(index % 2 == 0 ? ColorScheme.SECONDARY :
                        ColorScheme.SECONDARY.brighter());
            }

            return label;
        }
    }
}
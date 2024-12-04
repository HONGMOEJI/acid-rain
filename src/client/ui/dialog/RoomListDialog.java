// client/ui/dialog/RoomListDialog.java
package client.ui.dialog;

import client.app.GameClient;
import client.event.GameEvent;
import client.event.GameEventListener;
import client.ui.components.RetroButton;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.GameRoom;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class RoomListDialog extends BaseDialog implements GameEventListener {
    private final DefaultListModel<RoomListItem> roomListModel;
    private final JList<RoomListItem> roomList;
    private final GameClient client;
    private List<GameRoom> rooms = new ArrayList<>();

    public RoomListDialog(JFrame parent, GameClient client) {
        super(parent, "게임 방 목록");
        this.client = client;
        this.client.setEventListener(this);

        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);

        setupDialog();
        setupUI();
        client.sendMessage("ROOM_LIST");
    }

    private void setupDialog() {
        setSize(800, 600);
        setLocationRelativeTo(getOwner());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
    }

    private void setupUI() {
        mainPanel.setLayout(new BorderLayout(0, 20));

        // 헤더 패널
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 리스트 패널
        JPanel listPanel = createListPanel();
        mainPanel.add(listPanel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);

        JLabel titleLabel = new JLabel("게임 방 목록");
        titleLabel.setFont(FontManager.getFont(24f));
        titleLabel.setForeground(ColorScheme.TEXT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(titleLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);

        // 리스트 설정
        roomList.setBackground(ColorScheme.SECONDARY);
        roomList.setForeground(ColorScheme.TEXT);
        roomList.setSelectionBackground(ColorScheme.PRIMARY);
        roomList.setSelectionForeground(ColorScheme.TEXT);
        roomList.setFont(FontManager.getFont(16f));
        roomList.setCellRenderer(new RoomListCellRenderer());
        roomList.setFixedCellHeight(50);

        // 더블 클릭 이벤트
        roomList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    joinSelectedRoom();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));
        scrollPane.setBackground(ColorScheme.BACKGROUND);
        scrollPane.getViewport().setBackground(ColorScheme.SECONDARY);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setBackground(ColorScheme.BACKGROUND);

        RetroButton refreshButton = new RetroButton("새로고침");
        RetroButton createButton = new RetroButton("방 만들기");
        RetroButton joinButton = new RetroButton("입장");

        refreshButton.addActionListener(e -> client.sendMessage("ROOM_LIST"));
        createButton.addActionListener(e -> showCreateRoomDialog());
        joinButton.addActionListener(e -> joinSelectedRoom());

        panel.add(refreshButton);
        panel.add(createButton);
        panel.add(joinButton);

        return panel;
    }

    private GridBagConstraints createConstraints(int y, int gridheight) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = gridheight;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = y == 1 ? 1.0 : 0.0; // 리스트에만 수직 가중치 부여
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
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
            showError("입장할 방을 선택해주세요.");
            return;
        }

        GameRoom selectedRoom = findRoomById(selectedItem.getRoomId());
        if (selectedRoom == null) {
            showError("선택한 방을 찾을 수 없습니다.");
            return;
        }

        if (selectedRoom.isFull()) {
            showError("방이 가득 찼습니다.");
            return;
        }

        if (selectedRoom.isPasswordRequired()) {
            showPasswordDialog(selectedRoom);
        } else {
            client.sendJoinRoomRequest(selectedRoom.getRoomId(), null);
        }
    }

    private void showPasswordDialog(GameRoom room) {
        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(this,
                passwordField,
                "비밀번호를 입력하세요",
                JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String password = new String(passwordField.getPassword());
            if (!password.isEmpty()) {
                client.sendJoinRoomRequest(room.getRoomId(), password);
            } else {
                showError("비밀번호를 입력해주세요.");
            }
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
                case GameEvent.ERROR_OCCURRED:
                    showError((String) data[0]);
                    break;
            }
        });
    }

    private void handleRoomListUpdate(Object... data) {
        // 방 목록 업데이트 처리
        roomListModel.clear();
        if (data.length > 0 && data[0] instanceof List) {
            this.rooms = (List<GameRoom>) data[0];
            for (GameRoom room : rooms) {
                roomListModel.addElement(new RoomListItem(room));
            }
        }
    }

    private void handleRoomJoined(Object... data) {
        boolean success = (boolean) data[0];
        String message = (String) data[1];

        if (success && data.length >= 3) {
            GameRoom room = (GameRoom) data[2];
            dispose(); // 방 입장 성공시 다이얼로그 닫기
        } else {
            showError(message);
        }
    }

    private static class RoomListItem {
        private final String roomId;
        private final String displayText;

        public RoomListItem(GameRoom room) {
            this.roomId = room.getRoomId();
            this.displayText = String.format("%s %s (%d/%d) - %s - %s",
                    room.getRoomName(),
                    room.isPasswordRequired() ? "🔒" : "",
                    room.getCurrentPlayers(),
                    room.getMaxPlayers(),
                    room.getGameMode(),
                    room.getDifficulty());
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

            if (!isSelected) {
                label.setBackground(ColorScheme.SECONDARY);
                label.setForeground(ColorScheme.TEXT);
            }

            return label;
        }
    }
}
package client.components;

import client.GameClient;
import game.GameRoom;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class RoomListDialog extends JDialog {
    private static final Color BACKGROUND_COLOR = new Color(28, 31, 43);
    private static final Color TEXT_COLOR = Color.WHITE;

    private final DefaultListModel<GameRoom> roomListModel;
    private final JList<GameRoom> roomList;
    private final GameClient client;

    public RoomListDialog(JFrame parent, GameClient client) {
        super(parent, "게임 방 목록", true);
        this.client = client;
        this.client.setRoomListDialog(this);

        // 메인 패널 설정
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 방 목록 패널
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setCellRenderer(new RoomListCellRenderer());
        roomList.setBackground(new Color(45, 45, 60));
        roomList.setSelectionBackground(new Color(71, 185, 251));
        roomList.setSelectionForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        scrollPane.getViewport().setBackground(new Color(45, 45, 60));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(71, 185, 251)));

        // 상단 정보 패널
        JPanel infoPanel = createInfoPanel();

        // 버튼 패널
        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);

        // 초기 방 목록 요청
        refreshRoomList();
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(BACKGROUND_COLOR);

        JLabel titleLabel = new JLabel("현재 열린 방 목록");
        titleLabel.setFont(createFont().deriveFont(18f));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        infoPanel.add(titleLabel, BorderLayout.CENTER);
        return infoPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        RetroButton createRoomButton = new RetroButton("방 만들기");
        RetroButton joinRoomButton = new RetroButton("입장");
        RetroButton refreshButton = new RetroButton("새로고침");

        createRoomButton.addActionListener(e -> showCreateRoomDialog());
        joinRoomButton.addActionListener(e -> joinSelectedRoom());
        refreshButton.addActionListener(e -> refreshRoomList());

        buttonPanel.add(refreshButton);
        buttonPanel.add(createRoomButton);
        buttonPanel.add(joinRoomButton);

        return buttonPanel;
    }

    private Font createFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT,
                    new File("resources/fonts/DungGeunMo.ttf")).deriveFont(14f);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Dialog", Font.PLAIN, 14);
        }
    }

    private void showCreateRoomDialog() {
        CreateRoomDialog dialog = new CreateRoomDialog(this);
        dialog.setVisible(true);

        if (dialog.isRoomCreated()) {
            GameRoom newRoom = dialog.getCreatedRoom();
            // 서버에 방 생성 요청
            client.sendCreateRoomRequest(newRoom);
            refreshRoomList();
        }
    }

    private void joinSelectedRoom() {
        GameRoom selectedRoom = roomList.getSelectedValue();
        if (selectedRoom == null) {
            JOptionPane.showMessageDialog(this,
                    "입장할 방을 선택해주세요.",
                    "알림",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selectedRoom.isFull()) {
            JOptionPane.showMessageDialog(this,
                    "방이 가득 찼습니다.",
                    "입장 불가",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedRoom.isPasswordRequired()) {
            String password = JOptionPane.showInputDialog(this,
                    "비밀번호를 입력해주세요:",
                    "비밀방 입장",
                    JOptionPane.QUESTION_MESSAGE);

            if (password == null || !password.equals(selectedRoom.getPassword())) {
                JOptionPane.showMessageDialog(this,
                        "비밀번호가 일치하지 않습니다.",
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // 서버에 방 입장 요청
        client.sendJoinRoomRequest(selectedRoom.getRoomId());
        dispose();
    }

    private void refreshRoomList() {
        // 서버에 방 목록 요청
        client.sendRoomListRequest();
    }

    // 서버로부터 받은 방 목록으로 UI 업데이트
    public void updateRoomList(List<GameRoom> rooms) {
        SwingUtilities.invokeLater(() -> {
            roomListModel.clear(); // 기존 목록 초기화
            for (GameRoom room : rooms) {
                roomListModel.addElement(room); // 새로운 방 추가
            }
            System.out.println("Room list updated in UI with " + rooms.size() + " rooms.");
        });
    }


    // 방 입장 성공/실패 처리
    public void handleJoinRoomResponse(boolean success, String message) {
        if (!success) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        message,
                        "입장 실패",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    // 방 생성 성공/실패 처리
    public void handleCreateRoomResponse(boolean success, String message) {
        if (!success) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        message,
                        "방 생성 실패",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
        refreshRoomList();
    }
}
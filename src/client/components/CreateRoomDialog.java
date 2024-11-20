package client.components;

import game.DifficultyLevel;
import game.GameMode;
import game.GameRoom;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CreateRoomDialog extends JDialog {
    private boolean roomCreated = false;
    private GameRoom createdRoom;
    private static final Color BACKGROUND_COLOR = new Color(28, 31, 43);
    private static final Color TEXT_COLOR = Color.WHITE;

    public CreateRoomDialog(JDialog parent) {
        super(parent, "방 만들기", true);
        setupDialog();
    }

    private void setupDialog() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 컴포넌트 생성
        JTextField roomNameField = createStyledTextField();
        JPasswordField passwordField = createStyledPasswordField();
        JComboBox<GameMode> gameModeCombo = new JComboBox<>(GameMode.values());
        JComboBox<DifficultyLevel> difficultyCombo = new JComboBox<>(DifficultyLevel.values());

        styleCombos(gameModeCombo, difficultyCombo);

        // 컴포넌트 배치
        gbc.gridx = 0; gbc.gridy = 0;
        addLabel(mainPanel, "방 제목:", gbc);
        gbc.gridx = 1;
        mainPanel.add(roomNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        addLabel(mainPanel, "비밀번호:", gbc);
        gbc.gridx = 1;
        mainPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        addLabel(mainPanel, "게임모드:", gbc);
        gbc.gridx = 1;
        mainPanel.add(gameModeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        addLabel(mainPanel, "난이도:", gbc);
        gbc.gridx = 1;
        mainPanel.add(difficultyCombo, gbc);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        RetroButton createButton = new RetroButton("생성");
        RetroButton cancelButton = new RetroButton("취소");

        createButton.addActionListener(e -> {
            if (validateInput(roomNameField)) {
                createdRoom = new GameRoom(
                        roomNameField.getText(),
                        (GameMode) gameModeCombo.getSelectedItem(),
                        (DifficultyLevel) difficultyCombo.getSelectedItem()
                );
                createdRoom.setPassword(new String(passwordField.getPassword()));
                roomCreated = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField(20);
        field.setBackground(new Color(45, 45, 60));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setFont(createFont());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 185, 251)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField(20);
        field.setBackground(new Color(45, 45, 60));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setFont(createFont());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 185, 251)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private void styleCombos(JComboBox<?>... combos) {
        for (JComboBox<?> combo : combos) {
            combo.setBackground(new Color(45, 45, 60));
            combo.setForeground(TEXT_COLOR);
            combo.setFont(createFont());
            ((JComponent) combo.getRenderer()).setBackground(new Color(45, 45, 60));
            combo.setBorder(BorderFactory.createLineBorder(new Color(71, 185, 251)));
        }
    }

    private void addLabel(JPanel panel, String text, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(createFont());
        panel.add(label, gbc);
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

    private boolean validateInput(JTextField roomNameField) {
        String roomName = roomNameField.getText().trim();
        if (roomName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "방 제목을 입력해주세요.",
                    "입력 오류",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public boolean isRoomCreated() {
        return roomCreated;
    }

    public GameRoom getCreatedRoom() {
        return createdRoom;
    }
}
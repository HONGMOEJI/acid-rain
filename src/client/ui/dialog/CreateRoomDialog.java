// client/ui/dialog/CreateRoomDialog.java
package client.ui.dialog;

import client.ui.components.GameTextField;
import client.ui.components.RetroButton;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.DifficultyLevel;
import game.model.GameMode;
import game.model.GameRoom;

import javax.swing.*;
import java.awt.*;

public class CreateRoomDialog extends BaseDialog {
    private boolean roomCreated = false;
    private GameRoom createdRoom;
    private GameTextField roomNameField;
    private JPasswordField passwordField;
    private JComboBox<GameMode> gameModeCombo;
    private JComboBox<DifficultyLevel> difficultyCombo;

    public CreateRoomDialog(Window owner) {
        super(owner, "방 만들기");
        setupUI();
        centerOnScreen();
    }

    private void setupUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 컴포넌트 생성
        roomNameField = new GameTextField(20);
        passwordField = createPasswordField();
        gameModeCombo = new JComboBox<>(GameMode.values());
        difficultyCombo = new JComboBox<>(DifficultyLevel.values());

        stylizeComboBoxes(gameModeCombo, difficultyCombo);

        // 컴포넌트 배치
        gbc.gridx = 0; gbc.gridy = 0;
        addLabel("방 제목:", gbc);

        gbc.gridx = 1;
        mainPanel.add(roomNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        addLabel("비밀번호:", gbc);

        gbc.gridx = 1;
        mainPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        addLabel("게임모드:", gbc);

        gbc.gridx = 1;
        mainPanel.add(gameModeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        addLabel("난이도:", gbc);

        gbc.gridx = 1;
        mainPanel.add(difficultyCombo, gbc);

        // 버튼 패널
        RetroButton createButton = new RetroButton("생성");
        RetroButton cancelButton = new RetroButton("취소");

        createButton.addActionListener(e -> handleCreateRoom());
        cancelButton.addActionListener(e -> dispose());

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        mainPanel.add(createButtonPanel(createButton, cancelButton), gbc);
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField(20);
        field.setBackground(ColorScheme.SECONDARY);
        field.setForeground(ColorScheme.TEXT);
        field.setCaretColor(ColorScheme.TEXT);
        field.setFont(FontManager.getFont(14f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private void stylizeComboBoxes(JComboBox<?>... combos) {
        for (JComboBox<?> combo : combos) {
            combo.setBackground(ColorScheme.SECONDARY);
            combo.setForeground(ColorScheme.TEXT);
            combo.setFont(FontManager.getFont(14f));

            // 콤보박스 렌더러 스타일링
            ((JComponent) combo.getRenderer()).setBackground(ColorScheme.SECONDARY);
            combo.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));
        }
    }

    private void handleCreateRoom() {
        if (validateInput()) {
            createdRoom = new GameRoom(
                    roomNameField.getText().trim(),
                    (GameMode) gameModeCombo.getSelectedItem(),
                    (DifficultyLevel) difficultyCombo.getSelectedItem()
            );

            String password = new String(passwordField.getPassword());
            if (!password.isEmpty()) {
                createdRoom.setPassword(password);
            }

            roomCreated = true;
            dispose();
        }
    }

    private boolean validateInput() {
        String roomName = roomNameField.getText().trim();
        if (roomName.isEmpty()) {
            showError("방 제목을 입력해주세요.");
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
// client/LoginDialog.java
package client.components;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class LoginDialog extends JDialog {
    private JTextField nicknameField;
    private JTextField serverAddressField;
    private JTextField portField;
    private boolean accepted = false;
    private static final Color BACKGROUND_COLOR = new Color(28, 31, 43);
    private static final Color TEXT_COLOR = Color.WHITE;

    public LoginDialog(JFrame parent) {
        super(parent, "로그인", true);
        setupUI();
    }

    private void setupUI() {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT,
                    new File("resources/fonts/DungGeunMo.ttf")).deriveFont(14f);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(BACKGROUND_COLOR);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            // 라벨과 텍스트필드 생성
            nicknameField = createStyledTextField(font);
            serverAddressField = createStyledTextField(font);
            portField = createStyledTextField(font);

            // 기본값 설정
            serverAddressField.setText("localhost");
            portField.setText("12345");

            // 컴포넌트 배치
            gbc.gridx = 0; gbc.gridy = 0;
            addLabel("닉네임:", panel, gbc, font);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(nicknameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            addLabel("서버 주소:", panel, gbc, font);

            gbc.gridx = 1;
            panel.add(serverAddressField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            addLabel("포트:", panel, gbc, font);

            gbc.gridx = 1;
            panel.add(portField, gbc);

            // 버튼 패널
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setBackground(BACKGROUND_COLOR);

            RetroButton connectButton = new RetroButton("접속");
            RetroButton cancelButton = new RetroButton("취소");

            connectButton.addActionListener(e -> {
                if (validateInput()) {
                    accepted = true;
                    dispose();
                }
            });
            cancelButton.addActionListener(e -> {
                accepted = false;
                dispose();
            });

            buttonPanel.add(connectButton);
            buttonPanel.add(cancelButton);

            gbc.gridx = 0; gbc.gridy = 3;
            gbc.gridwidth = 2;
            panel.add(buttonPanel, gbc);

            add(panel);
            pack();
            setLocationRelativeTo(getParent());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addLabel(String text, JPanel panel, GridBagConstraints gbc, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(TEXT_COLOR);
        panel.add(label, gbc);
    }

    private JTextField createStyledTextField(Font font) {
        JTextField field = new JTextField(15);
        field.setFont(font);
        field.setBackground(new Color(45, 45, 60));
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 185, 251)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private boolean validateInput() {
        if (nicknameField.getText().trim().isEmpty()) {
            showError("닉네임을 입력해주세요.");
            return false;
        }
        if (serverAddressField.getText().trim().isEmpty()) {
            showError("서버 주소를 입력해주세요.");
            return false;
        }
        try {
            int port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                showError("포트 번호는 1-65535 사이의 값이어야 합니다.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("올바른 포트 번호를 입력해주세요.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    public String getNickname() {
        return nicknameField.getText().trim();
    }

    public String getServerAddress() {
        return serverAddressField.getText().trim();
    }

    public int getPort() {
        return Integer.parseInt(portField.getText().trim());
    }

    public boolean isAccepted() {
        return accepted;
    }
}
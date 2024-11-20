package client;

import client.components.LoginDialog;

import javax.swing.*;

public class ClientMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("산성비 게임");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            LoginDialog loginDialog = new LoginDialog(frame);
            loginDialog.setVisible(true);

            if (loginDialog.isAccepted()) {
                try {
                    GameClient client = new GameClient(
                            loginDialog.getServerAddress(),
                            loginDialog.getPort(),
                            loginDialog.getNickname()
                    );

                    // 먼저 MainMenu 생성
                    MainMenu mainMenu = new MainMenu(client);
                    // 그 다음 GameClient에 MainMenu 설정
                    client.setMainMenu(mainMenu);

                    frame.add(mainMenu);
                    frame.setVisible(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(frame,
                            "서버 연결 실패: " + e.getMessage(),
                            "오류",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            } else {
                System.exit(0);
            }
        });
    }
}
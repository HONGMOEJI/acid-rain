// client/app/ClientMain.java
package client.app;

import client.ui.dialog.LoginDialog;
import client.ui.MainMenu;
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

            // 로그인 다이얼로그 표시
            LoginDialog loginDialog = new LoginDialog(frame);
            loginDialog.setVisible(true);

            if (loginDialog.isAccepted()) {
                try {
                    // GameClient 생성 및 초기화
                    GameClient client = new GameClient(
                            loginDialog.getServerAddress(),
                            loginDialog.getPort(),
                            loginDialog.getNickname()
                    );

                    // MainMenu 생성 및 설정
                    MainMenu mainMenu = new MainMenu(client);
                    client.setEventListener(mainMenu);

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
/*
 * client.app.ClientMain.java
 * 게임 기본 진입점
 */
package client.app;

import client.ui.dialog.LoginDialog;
import client.ui.MainMenu;
import javax.swing.*;
import java.util.logging.Logger;

    public class ClientMain {
    private static final Logger logger = Logger.getLogger(ClientMain.class.getName());

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warning("시스템 룩앤필 설정 실패: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ACID RAIN");
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

                    MainMenu mainMenu = new MainMenu(client);
                    client.setEventListener(mainMenu);

                    client.connect();

                    frame.add(mainMenu);
                    frame.setVisible(true);
                } catch (Exception e) {
                    logger.severe("게임 클라이언트 초기화 실패: " + e.getMessage());
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
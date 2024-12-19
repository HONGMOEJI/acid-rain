package client.ui.game;

import client.app.GameClient;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.PlayerScore;
import game.model.GameMode;
import game.model.DifficultyLevel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GameResultScreen extends JDialog {
    private final GameClient client;
    private final PlayerScore myScore;
    private final PlayerScore opponentScore;
    private final String roomId;
    private final boolean isWinner;

    public GameResultScreen(JFrame parent, GameClient client, PlayerScore myScore,
                            PlayerScore opponentScore, String roomId) {
        super(parent, "게임 결과", true);
        this.client = client;
        this.myScore = myScore;
        this.opponentScore = opponentScore;
        this.roomId = roomId;
        this.isWinner = myScore.getScore() > opponentScore.getScore();

        setupDialog();
        setupUI();
    }

    private void setupDialog() {
        setSize(500, 400);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(ColorScheme.BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 결과 표시
        mainPanel.add(createResultPanel(), BorderLayout.CENTER);

        // 버튼 패널
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setOpaque(false);

        // 승리/패배 메시지
        JLabel resultLabel = new JLabel(isWinner ? "승리!" : "패배...", SwingConstants.CENTER);
        resultLabel.setFont(FontManager.getFont(32f));
        resultLabel.setForeground(isWinner ? Color.GREEN : Color.RED);

        // 내 점수
        JLabel myScoreLabel = new JLabel(String.format("내 점수: %d", myScore.getScore()),
                SwingConstants.CENTER);
        myScoreLabel.setFont(FontManager.getFont(24f));
        myScoreLabel.setForeground(ColorScheme.TEXT);

        // 상대방 점수
        JLabel opponentScoreLabel = new JLabel(
                String.format("상대방 점수: %d", opponentScore.getScore()),
                SwingConstants.CENTER);
        opponentScoreLabel.setFont(FontManager.getFont(24f));
        opponentScoreLabel.setForeground(ColorScheme.TEXT);

        // 게임 정보
        JLabel gameInfoLabel = new JLabel(
                String.format("%s - %s",
                        myScore.getGameMode().getDisplayName(),
                        myScore.getDifficulty().getDisplayName()),
                SwingConstants.CENTER);
        gameInfoLabel.setFont(FontManager.getFont(16f));
        gameInfoLabel.setForeground(ColorScheme.TEXT);

        panel.add(resultLabel);
        panel.add(myScoreLabel);
        panel.add(opponentScoreLabel);
        panel.add(gameInfoLabel);

        // 리더보드 진입 가능 여부 확인
        if (isLeaderboardWorthy()) {
            JLabel congratsLabel = new JLabel("새로운 기록 달성!", SwingConstants.CENTER);
            congratsLabel.setFont(FontManager.getFont(18f));
            congratsLabel.setForeground(Color.YELLOW);
            panel.add(congratsLabel);
        }

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panel.setOpaque(false);

        if (isLeaderboardWorthy()) {
            JButton leaderboardButton = createStyledButton("리더보드에 등록");
            leaderboardButton.addActionListener(e -> registerLeaderboard());
            panel.add(leaderboardButton);
        }

        JButton rematchButton = createStyledButton("다시 하기");
        rematchButton.addActionListener(e -> requestRematch());

        JButton exitButton = createStyledButton("나가기");
        exitButton.addActionListener(e -> exitToLobby());

        panel.add(rematchButton);
        panel.add(exitButton);

        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getFont(14f));
        button.setBackground(ColorScheme.PRIMARY);
        button.setForeground(ColorScheme.TEXT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    private boolean isLeaderboardWorthy() {
        // TODO: 리더보드 구현 시 실제 로직으로 대체
        return myScore.getScore() > 1000;
    }

    private void registerLeaderboard() {
        // TODO: 리더보드 등록 로직
        client.sendGameAction(roomId, "REGISTER_LEADERBOARD",
                String.valueOf(myScore.getScore()));
    }

    private void requestRematch() {
        client.sendGameAction(roomId, "REQUEST_REMATCH");
        dispose();
    }

    private void exitToLobby() {
        client.sendGameAction(roomId, "EXIT_TO_LOBBY");
        dispose();
    }
}
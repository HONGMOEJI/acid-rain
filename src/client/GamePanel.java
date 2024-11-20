// client/GamePanel.java
package client;

import game.Word;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private GameClient client;
    private List<Word> words;
    private JTextField inputField;
    private Timer gameTimer;
    private int score;

    public GamePanel(GameClient client) {
        this.client = client;
        this.words = new ArrayList<>();
        this.score = 0;

        setLayout(new BorderLayout());
        setupUI();

        gameTimer = new Timer(16, e -> {
            updateGame();
            repaint();
        });
        gameTimer.start();
    }

    private void setupUI() {
        inputField = new JTextField();
        inputField.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (!input.isEmpty()) {
                checkWord(input);
                inputField.setText("");
            }
        });
        add(inputField, BorderLayout.SOUTH);
    }

    private void updateGame() {
        for (Word word : new ArrayList<>(words)) {
            word.move();
            if (word.getY() > getHeight()) {
                words.remove(word);
                // 놓친 단어 처리
            }
        }
    }

    private void checkWord(String input) {
        for (Word word : new ArrayList<>(words)) {
            if (word.getText().equals(input)) {
                words.remove(word);
                score += 10;
                client.sendMessage("SCORE:" + score);
                break;
            }
        }
    }

    public void processMessage(String message) {
        if (message.startsWith("WORD:")) {
            // 새 단어 추가
            String word = message.substring(5);
            int x = (int) (Math.random() * (getWidth() - 100));
            words.add(new Word(word, x, 2));
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);

        // 단어 그리기
        for (Word word : words) {
            g.drawString(word.getText(), word.getX(), word.getY());
        }

        // 점수 표시
        g.drawString("Score: " + score, 10, 20);
    }
}
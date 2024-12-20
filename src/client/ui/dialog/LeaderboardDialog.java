package client.ui.dialog;

import client.app.GameClient;
import client.event.GameEventListener;
import client.ui.theme.ColorScheme;
import client.ui.theme.FontManager;
import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.LeaderboardEntry;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardDialog extends BaseDialog implements GameEventListener {
    private final GameClient client;
    private final JTabbedPane tabbedPane;
    private final JTable globalTable;
    private final JTable myRecordsTable;
    private final DefaultTableModel globalModel;
    private final DefaultTableModel myRecordsModel;
    private final JComboBox<GameModeWrapper> modeFilter;
    private final JComboBox<DifficultyWrapper> difficultyFilter;
    private final DateTimeFormatter dateFormatter;

    // 게임모드와 난이도를 위한 래퍼 클래스들
    private static class GameModeWrapper {
        private final GameMode mode;

        public GameModeWrapper(GameMode mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            return mode.getDisplayName();
        }

        public GameMode getMode() {
            return mode;
        }
    }

    private static class DifficultyWrapper {
        private final DifficultyLevel difficulty;

        public DifficultyWrapper(DifficultyLevel difficulty) {
            this.difficulty = difficulty;
        }

        @Override
        public String toString() {
            return difficulty.getDisplayName();
        }

        public DifficultyLevel getDifficulty() {
            return difficulty;
        }
    }

    public LeaderboardDialog(Window owner, GameClient client) {
        super(owner, "리더보드");
        this.client = client;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        setSize(800, 600);
        setResizable(true);
        setMinimumSize(new Dimension(600, 400));

        // 모델 초기화
        String[] columns = {"순위", "닉네임", "점수", "게임 모드", "난이도", "달성 일시"};
        globalModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myRecordsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // 콤보박스 초기화
        GameModeWrapper[] modes = new GameModeWrapper[GameMode.values().length];
        for (int i = 0; i < GameMode.values().length; i++) {
            modes[i] = new GameModeWrapper(GameMode.values()[i]);
        }
        modeFilter = new JComboBox<>(modes);

        DifficultyWrapper[] difficulties = new DifficultyWrapper[DifficultyLevel.values().length];
        for (int i = 0; i < DifficultyLevel.values().length; i++) {
            difficulties[i] = new DifficultyWrapper(DifficultyLevel.values()[i]);
        }
        difficultyFilter = new JComboBox<>(difficulties);

        // 컴포넌트 초기화
        tabbedPane = new JTabbedPane();
        globalTable = createTable(globalModel);
        myRecordsTable = createTable(myRecordsModel);

        setupUI();
        client.setEventListener(this);
        loadLeaderboard();
    }

    private void setupUI() {
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 필터 패널
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterPanel.setBackground(ColorScheme.BACKGROUND);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.PRIMARY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // 게임 모드 필터
        JLabel modeLabel = new JLabel("게임 모드:");
        modeLabel.setForeground(ColorScheme.TEXT);
        modeLabel.setFont(FontManager.getFont(14f));
        filterPanel.add(modeLabel);

        styleComboBox(modeFilter);
        filterPanel.add(modeFilter);

        // 난이도 필터
        JLabel difficultyLabel = new JLabel("난이도:");
        difficultyLabel.setForeground(ColorScheme.TEXT);
        difficultyLabel.setFont(FontManager.getFont(14f));
        filterPanel.add(difficultyLabel);

        styleComboBox(difficultyFilter);
        filterPanel.add(difficultyFilter);

        // 새로고침 버튼
        JButton refreshButton = new JButton("새로고침");
        styleButton(refreshButton);
        refreshButton.addActionListener(e -> loadLeaderboard());
        filterPanel.add(Box.createHorizontalStrut(20));  // 간격 추가
        filterPanel.add(refreshButton);

        mainPanel.add(filterPanel, BorderLayout.NORTH);

        // 테이블 패널
        tabbedPane.addTab("전체 순위", createTablePanel(globalTable));
        tabbedPane.addTab("내 기록", createTablePanel(myRecordsTable));
        styleTabbedPane(tabbedPane);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // 필터 변경 리스너
        modeFilter.addActionListener(e -> loadLeaderboard());
        difficultyFilter.addActionListener(e -> loadLeaderboard());
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                if (comp instanceof JLabel) {
                    JLabel label = (JLabel) comp;
                    if (column == 2) { // 점수 칼럼
                        label.setHorizontalAlignment(SwingConstants.RIGHT);
                    } else {
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }
                return comp;
            }
        };

        table.setBackground(ColorScheme.SECONDARY);
        table.setForeground(ColorScheme.TEXT);
        table.setFont(FontManager.getFont(14f));
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(ColorScheme.PRIMARY.darker());
        table.setSelectionBackground(ColorScheme.PRIMARY.brighter());
        table.setSelectionForeground(ColorScheme.TEXT);
        table.setFocusable(false);

        // 헤더 스타일링
        JTableHeader header = table.getTableHeader();
        header.setBackground(ColorScheme.PRIMARY);
        header.setForeground(ColorScheme.TEXT);
        header.setFont(FontManager.getFont(14f).deriveFont(Font.BOLD));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ColorScheme.PRIMARY.darker()));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        // 열 너비 설정
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);   // 순위
        columnModel.getColumn(1).setPreferredWidth(120);  // 닉네임
        columnModel.getColumn(2).setPreferredWidth(100);  // 점수
        columnModel.getColumn(3).setPreferredWidth(100);  // 게임 모드
        columnModel.getColumn(4).setPreferredWidth(80);   // 난이도
        columnModel.getColumn(5).setPreferredWidth(150);  // 달성 일시

        return table;
    }

    private JPanel createTablePanel(JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(ColorScheme.BACKGROUND);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 스크롤바 스타일링
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = ColorScheme.PRIMARY;
                this.trackColor = ColorScheme.SECONDARY;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });

        panel.add(scrollPane);
        return panel;
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(ColorScheme.SECONDARY);
        comboBox.setForeground(ColorScheme.TEXT);
        comboBox.setFont(FontManager.getFont(14f));
        comboBox.setPreferredSize(new Dimension(120, 30));
        comboBox.setBorder(BorderFactory.createLineBorder(ColorScheme.PRIMARY));

        // 렌더러 설정
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        comboBox.setRenderer(renderer);
    }

    private void styleButton(JButton button) {
        button.setBackground(ColorScheme.PRIMARY);
        button.setForeground(ColorScheme.TEXT);
        button.setFont(FontManager.getFont(14f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.PRIMARY.darker()),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));

        // 호버 효과
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.PRIMARY.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ColorScheme.PRIMARY);
            }
        });
    }

    private void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setBackground(ColorScheme.BACKGROUND);
        tabbedPane.setForeground(ColorScheme.TEXT);
        tabbedPane.setFont(FontManager.getFont(14f));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void loadLeaderboard() {
        clearTables();
        GameMode mode = ((GameModeWrapper)modeFilter.getSelectedItem()).getMode();
        DifficultyLevel difficulty = ((DifficultyWrapper)difficultyFilter.getSelectedItem()).getDifficulty();

        // 전체 순위 요청
        client.sendGameAction("LEADERBOARD", "GET_TOP", mode.name(), difficulty.name());

        // 내 기록 요청
        client.sendGameAction("LEADERBOARD", "GET_MY_RECORDS", mode.name(), difficulty.name());
    }

    private void clearTables() {
        globalModel.setRowCount(0);
        myRecordsModel.setRowCount(0);
    }

    @Override
    public void onGameEvent(String eventType, Object... data) {
        switch (eventType) {
            case "TOP_SCORES" -> handleTopScores(data);
            case "USER_RECORDS" -> handleUserRecords(data);
        }
    }

    private void handleTopScores(Object... data) {
        SwingUtilities.invokeLater(() -> {
            try {
                globalModel.setRowCount(0);
                System.out.println("Processing top scores...");
                List<LeaderboardEntry> entries = parseLeaderboardEntries(data);
                System.out.println("Parsed " + entries.size() + " entries");

                int rank = 1;
                for (LeaderboardEntry entry : entries) {
                    System.out.println("Adding entry to table: Rank " + rank + " - " + entry.toString());
                    globalModel.addRow(new Object[]{
                            rank++,
                            entry.getUsername(),
                            String.format("%,d", entry.getScore()),
                            entry.getGameMode().getDisplayName(),
                            entry.getDifficulty().getDisplayName(),
                            entry.getTimestamp().format(dateFormatter)
                    });
                }
                System.out.println("Finished adding entries to table");
            } catch (Exception e) {
                System.err.println("Error in handleTopScores: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleUserRecords(Object... data) {
        SwingUtilities.invokeLater(() -> {
            myRecordsModel.setRowCount(0);
            List<LeaderboardEntry> entries = parseLeaderboardEntries(data);

            int rank = 1;
            for (LeaderboardEntry entry : entries) {
                myRecordsModel.addRow(new Object[]{
                        rank++,
                        entry.getUsername(),
                        String.format("%,d", entry.getScore()),
                        entry.getGameMode().getDisplayName(),
                        entry.getDifficulty().getDisplayName(),
                        entry.getTimestamp().format(dateFormatter)
                });
            }
        });
    }

    private List<LeaderboardEntry> parseLeaderboardEntries(Object... data) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (data.length > 0 && data[0] instanceof String) {
            String rawData = (String) data[0];
            System.out.println("=== Begin Raw Leaderboard Data ===");
            System.out.println(rawData);
            System.out.println("=== End Raw Leaderboard Data ===");

            if (!rawData.isEmpty()) {
                String[] lines = rawData.split("\n");
                System.out.println("Found " + lines.length + " lines");

                for (String line : lines) {
                    try {
                        if (!line.trim().isEmpty()) {
                            System.out.println("Parsing line: " + line.trim());
                            LeaderboardEntry entry = LeaderboardEntry.fromString(line.trim());
                            System.out.println("Successfully parsed: " + entry.toString());
                            entries.add(entry);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing entry: " + line);
                        System.err.println("Error details: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.err.println("Received empty data");
            }
        } else {
            System.err.println("Invalid data type: " +
                    (data.length > 0 ? data[0].getClass().getName() : "null"));
        }

        System.out.println("Total parsed entries: " + entries.size());
        return entries;
    }

    @Override
    public void dispose() {
        client.setEventListener(null);
        super.dispose();
    }
}
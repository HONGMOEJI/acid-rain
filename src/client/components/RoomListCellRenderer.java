package client.components;

import game.GameRoom;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class RoomListCellRenderer extends DefaultListCellRenderer {
    private static final Color SELECTED_BACKGROUND = new Color(71, 185, 251);
    private static final Color NORMAL_BACKGROUND = new Color(45, 45, 60);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Font CUSTOM_FONT = createFont();

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof GameRoom) {
            GameRoom room = (GameRoom) value;
            setText(formatRoomInfo(room));
        }

        setFont(CUSTOM_FONT);
        setForeground(TEXT_COLOR);
        setBackground(isSelected ? SELECTED_BACKGROUND : NORMAL_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        return this;
    }

    private String formatRoomInfo(GameRoom room) {
        return String.format("%s [%d/%d] %s %s %s",
                room.getRoomName(),
                room.getCurrentPlayers(),
                room.getMaxPlayers(),
                room.getGameMode(),
                room.getDifficulty(),
                room.isPasswordRequired() ? "🔒" : "");
    }

    private static Font createFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT,
                    new File("resources/fonts/DungGeunMo.ttf")).deriveFont(14f);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Dialog", Font.PLAIN, 14);
        }
    }
}
package server.game;

import game.model.GameMode;
import game.model.Word;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ServerWordManager {
    private static final String WORDS_DIRECTORY = "resources/words/";
    private final Map<GameMode, List<String>> wordsByMode = new HashMap<>();
    private final Random random = new Random();
    private final GameMode mode;

    public ServerWordManager(GameMode mode) {
        this.mode = mode;
        loadWordsForMode(mode);
    }

    private void loadWordsForMode(GameMode mode) {
        String filename = WORDS_DIRECTORY + "words_" + mode.name().toLowerCase() + ".txt";
        Path filePath = Paths.get(filename);

        try {
            Files.createDirectories(Paths.get(WORDS_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Failed to create words directory: " + e.getMessage());
        }

        if (!Files.exists(filePath)) {
            createDefaultWordFile(mode, filePath);
        }

        try {
            List<String> words = Files.readAllLines(filePath);
            words.removeIf(String::isEmpty);
            wordsByMode.put(mode, words);
        } catch (IOException e) {
            System.err.println("Error reading word file for " + mode + ": " + e.getMessage());
            wordsByMode.put(mode, Collections.emptyList());
        }
    }

    private void createDefaultWordFile(GameMode mode, Path filePath) {
        List<String> defaultWords;
        switch (mode) {
            case JAVA -> defaultWords = List.of("public", "class", "extends", "implements", "void", "int", "boolean", "String", "final", "static", "private", "protected", "abstract", "try", "catch", "throw", "import", "return", "for", "while");
            case PYTHON -> defaultWords = List.of("def", "class", "import", "from", "as", "if", "elif", "else", "while", "for", "in", "try", "except", "finally", "with", "print");
            case KOTLIN -> defaultWords = List.of("fun", "val", "var", "class", "object", "interface", "override", "private", "public", "protected", "data", "return", "when");
            case C -> defaultWords = List.of("int", "char", "float", "double", "void", "long", "short", "signed", "unsigned", "struct", "union", "if", "else", "for", "while", "return");
            default -> defaultWords = List.of("sample", "word", "test");
        }
        try {
            Files.write(filePath, defaultWords);
        } catch (IOException e) {
            System.err.println("Failed to create default word file for " + mode + ": " + e.getMessage());
        }
    }

    public Word getRandomWord() {
        List<String> words = wordsByMode.getOrDefault(mode, Collections.emptyList());
        if (words.isEmpty()) {
            return new Word("default", 100, 0);
        }
        String text = words.get(random.nextInt(words.size()));
        int xPos = random.nextInt(600) + 100; // 100~700 range
        Word w = new Word(text, xPos, 0);
        // 20% 확률로 특수효과
        if (random.nextDouble() < 0.2) {
            w.setSpecialEffect(true);
            if (random.nextBoolean()) {
                w.setEffect(Word.SpecialEffect.SCORE_BOOST);
            } else {
                w.setEffect(Word.SpecialEffect.BLIND_OPPONENT);
            }
        }
        return w;
    }
}

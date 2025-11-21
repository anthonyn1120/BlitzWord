// Speedle - A Wordle-style word guessing game

package com.example.demo3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class BlitzWord {
    // Game Constants
    private static final int MAX_GUESSES = 6;
    private static final int WORD_LENGTH = 5;
    private static final int TIMER_DURATION = 120;
    private static final int LEADERBOARD_THRESHOLD = 60;

    // Color Constants (Wordle-style)
    private static final Color CORRECT_COLOR = new Color(108, 169, 101);
    private static final Color PRESENT_COLOR = new Color(200, 182, 83);
    private static final Color ABSENT_COLOR = new Color(120, 124, 127);
    private static final Color KEY_DEFAULT = new Color(211, 214, 218);

    // UI Components
    private JFrame frame;
    private JLabel[][] letterLabels;
    private JButton playAgainButton;
    private JLabel timerLabel;
    private Map<Character, JButton> keyboardButtons;

    // Game State
    private String targetWord;
    private int currentGuess;
    private String currentInput;
    private boolean isHardMode;
    private GameTimer gameTimer;
    private Leaderboard leaderboard;
    private boolean gameActive;

    public BlitzWord(boolean hardMode) {
        this.isHardMode = hardMode;
        this.targetWord = selectRandomWord();
        this.currentGuess = 0;
        this.currentInput = "";
        this.leaderboard = new Leaderboard();
        this.gameActive = true;
        this.keyboardButtons = new HashMap<>();

        // Print answer to terminal for testing
        System.out.println("The word is: " + targetWord);

        initializeUI();

        if (isHardMode) {
            gameTimer = new GameTimer(TIMER_DURATION, this::onTimerUpdate, this::onTimerExpired);
            gameTimer.start();
        }
    }

    private void initializeUI() {
        frame = new JFrame("Speedle");
        frame.setSize(1000, 1000);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        createTitleLabel();
        createGuessGrid();
        createKeyboard();
        createPlayAgainButton();

        if (isHardMode) {
            createTimerLabel();
        }

        frame.setVisible(true);
        frame.setFocusable(true);
    }

    private void createTitleLabel() {
        JLabel titleLabel = new JLabel("Speedle", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Neue Helvetica 75", Font.BOLD, 40));
        titleLabel.setBounds(400, 0, 200, 50);
        frame.add(titleLabel);
    }

    private void createTimerLabel() {
        timerLabel = new JLabel("Time: 2:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        timerLabel.setBounds(350, 50, 300, 30);
        frame.add(timerLabel);
    }

    private void createGuessGrid() {
        letterLabels = new JLabel[MAX_GUESSES][WORD_LENGTH];

        for (int row = 0; row < MAX_GUESSES; row++) {
            JPanel rowPanel = new JPanel();
            rowPanel.setLayout(new GridLayout(1, WORD_LENGTH, 5, 5));

            for (int col = 0; col < WORD_LENGTH; col++) {
                letterLabels[row][col] = createLetterLabel();
                rowPanel.add(letterLabels[row][col]);
            }

            rowPanel.setBounds(350, 100 + (row * 60), 300, 50);
            frame.add(rowPanel);
        }
    }

    private JLabel createLetterLabel() {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(new Font("Neue Helvetica 75", Font.BOLD, 40));
        label.setPreferredSize(new Dimension(50, 50));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }

    private void createKeyboard() {
        // Row 1
        JPanel row1 = new JPanel();
        String keys1 = "QWERTYUIOP";
        for (char c : keys1.toCharArray()) {
            JButton btn = createKeyButton(c);
            row1.add(btn);
            keyboardButtons.put(c, btn);
        }
        row1.setBounds(250, 650, 500, 50);
        frame.add(row1);

        // Row 2
        JPanel row2 = new JPanel();
        String keys2 = "ASDFGHJKL";
        for (char c : keys2.toCharArray()) {
            JButton btn = createKeyButton(c);
            row2.add(btn);
            keyboardButtons.put(c, btn);
        }
        row2.setBounds(270, 710, 460, 50);
        frame.add(row2);

        // Row 3
        JPanel row3 = new JPanel();
        JButton enterBtn = new JButton("ENTER");
        enterBtn.setFont(new Font("Arial", Font.BOLD, 10));
        enterBtn.setPreferredSize(new Dimension(65, 45));
        enterBtn.addActionListener(e -> submitGuess());
        row3.add(enterBtn);

        String keys3 = "ZXCVBNM";
        for (char c : keys3.toCharArray()) {
            JButton btn = createKeyButton(c);
            row3.add(btn);
            keyboardButtons.put(c, btn);
        }

        JButton backBtn = new JButton("←");
        backBtn.setFont(new Font("Arial", Font.BOLD, 20));
        backBtn.setPreferredSize(new Dimension(55, 45));
        backBtn.addActionListener(e -> backspace());
        row3.add(backBtn);

        row3.setBounds(240, 770, 520, 50);
        frame.add(row3);

        // Keyboard listener
        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                char c = Character.toUpperCase(e.getKeyChar());
                if (Character.isLetter(c)) {
                    typeLetter(c);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    submitGuess();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    backspace();
                }
            }
        });
    }

    private JButton createKeyButton(char letter) {
        JButton btn = new JButton(String.valueOf(letter));
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(45, 45));
        btn.setBackground(KEY_DEFAULT);
        btn.addActionListener(e -> typeLetter(letter));
        return btn;
    }

    private void typeLetter(char letter) {
        if (!gameActive || currentInput.length() >= WORD_LENGTH) {
            return;
        }
        currentInput += letter;
        updateCurrentRow();
    }

    private void backspace() {
        if (!gameActive || currentInput.length() == 0) {
            return;
        }
        currentInput = currentInput.substring(0, currentInput.length() - 1);
        updateCurrentRow();
    }

    private void updateCurrentRow() {
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (i < currentInput.length()) {
                letterLabels[currentGuess][i].setText(String.valueOf(currentInput.charAt(i)));
            } else {
                letterLabels[currentGuess][i].setText("");
            }
        }
    }

    private void submitGuess() {
        if (!gameActive || currentInput.length() != WORD_LENGTH) {
            shakeFrame();
            return;
        }

        String guess = currentInput.toUpperCase();

        // Simple animation - show colors one by one
        for (int i = 0; i < WORD_LENGTH; i++) {
            final int index = i;
            javax.swing.Timer timer = new javax.swing.Timer(i * 200, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    char letter = guess.charAt(index);
                    Color color = determineLetterColor(letter, index);

                    letterLabels[currentGuess][index].setOpaque(true);
                    letterLabels[currentGuess][index].setBackground(color);
                    letterLabels[currentGuess][index].setForeground(Color.WHITE);

                    // Update keyboard color
                    JButton keyBtn = keyboardButtons.get(letter);
                    if (keyBtn != null && !keyBtn.getBackground().equals(CORRECT_COLOR)) {
                        if (color.equals(CORRECT_COLOR) ||
                                (color.equals(PRESENT_COLOR) && !keyBtn.getBackground().equals(PRESENT_COLOR))) {
                            keyBtn.setBackground(color);
                            keyBtn.setForeground(Color.WHITE);
                        } else if (color.equals(ABSENT_COLOR) && keyBtn.getBackground().equals(KEY_DEFAULT)) {
                            keyBtn.setBackground(color);
                            keyBtn.setForeground(Color.WHITE);
                        }
                    }

                    ((javax.swing.Timer)e.getSource()).stop();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }

        // Check result after animation
        javax.swing.Timer checkTimer = new javax.swing.Timer(WORD_LENGTH * 200 + 100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (guess.equals(targetWord)) {
                    handleWin();
                } else {
                    currentGuess++;
                    currentInput = "";
                    if (currentGuess >= MAX_GUESSES) {
                        handleLoss();
                    }
                }
                ((javax.swing.Timer)e.getSource()).stop();
            }
        });
        checkTimer.setRepeats(false);
        checkTimer.start();
    }

    private void createPlayAgainButton() {
        playAgainButton = new JButton("Play Again");
        playAgainButton.setBounds(400, 850, 200, 50);
        playAgainButton.setVisible(false);
        playAgainButton.addActionListener(e -> resetGame());
        frame.add(playAgainButton);
    }

    private Color determineLetterColor(char letter, int position) {
        if (targetWord.charAt(position) == letter) {
            return CORRECT_COLOR;
        } else if (targetWord.indexOf(letter) != -1) {
            return PRESENT_COLOR;
        } else {
            return ABSENT_COLOR;
        }
    }

    private void handleWin() {
        endGame();

        if (isHardMode) {
            int timeElapsed = TIMER_DURATION - gameTimer.getTimeRemaining();

            if (timeElapsed < TIMER_DURATION) {
                String playerName = JOptionPane.showInputDialog(frame,
                        "You won! The word was: " + targetWord +
                                "\nTime: " + formatTime(timeElapsed) +
                                "\n\nEnter your name for the leaderboard:");

                if (playerName != null && !playerName.trim().isEmpty()) {
                    leaderboard.addScore(playerName.trim(), timeElapsed);
                    leaderboard.display(frame);
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, "You won! The word was: " + targetWord);
        }

        playAgainButton.setVisible(true);
    }

    private void handleLoss() {
        endGame();

        JOptionPane.showMessageDialog(frame, "Out of attempts! The word was: " + targetWord);

        playAgainButton.setVisible(true);
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    private void onTimerUpdate(int secondsRemaining) {
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        timerLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
    }

    private void onTimerExpired() {
        endGame();
        JOptionPane.showMessageDialog(frame, "Time's up! The word was: " + targetWord);
        playAgainButton.setVisible(true);
    }

    private void endGame() {
        gameActive = false;
        stopTimer();
    }

    private void stopTimer() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
    }

    private void shakeFrame() {
        final Point originalLocation = frame.getLocation();
        javax.swing.Timer shakeTimer = new javax.swing.Timer(50, new ActionListener() {
            int shakeCount = 0;
            public void actionPerformed(ActionEvent e) {
                if (shakeCount < 10) {
                    int offset = (shakeCount % 2 == 0) ? 10 : -10;
                    frame.setLocation(originalLocation.x + offset, originalLocation.y);
                    shakeCount++;
                } else {
                    frame.setLocation(originalLocation);
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            }
        });
        shakeTimer.start();
    }

    private void resetGame() {
        targetWord = selectRandomWord();
        currentGuess = 0;
        currentInput = "";
        gameActive = true;

        for (int i = 0; i < MAX_GUESSES; i++) {
            for (int j = 0; j < WORD_LENGTH; j++) {
                letterLabels[i][j].setText("");
                letterLabels[i][j].setBackground(Color.WHITE);
                letterLabels[i][j].setOpaque(false);
            }
        }

        for (JButton btn : keyboardButtons.values()) {
            btn.setBackground(KEY_DEFAULT);
            btn.setForeground(Color.BLACK);
        }

        playAgainButton.setVisible(false);

        if (isHardMode) {
            gameTimer = new GameTimer(TIMER_DURATION, this::onTimerUpdate, this::onTimerExpired);
            gameTimer.start();
        }

        frame.requestFocus();
    }

    private String selectRandomWord() {
        List<String> wordList = loadWordsFromFile();

        if (wordList.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Error: Could not load word list.\nPlease ensure 'words.txt' exists.");
            return "ERROR";
        }

        Random random = new Random();
        return wordList.get(random.nextInt(wordList.size())).toUpperCase();
    }

    private List<String> loadWordsFromFile() {
        List<String> words = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("words.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.length() == WORD_LENGTH) {
                    words.add(trimmedLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading words.txt: " + e.getMessage());
        }

        return words;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int modeChoice = JOptionPane.showOptionDialog(
                    null,
                    "Choose your game mode:",
                    "Speedle - Mode Selection",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Standard Mode", "Hard Mode (2:00 Timer)"},
                    "Standard Mode"
            );

            boolean hardMode = (modeChoice == 1);
            new BlitzWord(hardMode);
        });
    }
}

// Simple Timer class for Hard Mode
class GameTimer {
    private javax.swing.Timer swingTimer;
    private int secondsRemaining;
    private TimerUpdateListener updateListener;
    private TimerExpiredListener expiredListener;

    public interface TimerUpdateListener {
        void onUpdate(int secondsRemaining);
    }

    public interface TimerExpiredListener {
        void onExpired();
    }

    public GameTimer(int seconds, TimerUpdateListener updateListener, TimerExpiredListener expiredListener) {
        this.secondsRemaining = seconds;
        this.updateListener = updateListener;
        this.expiredListener = expiredListener;
    }

    public void start() {
        updateListener.onUpdate(secondsRemaining);

        swingTimer = new javax.swing.Timer(1000, e -> {
            secondsRemaining--;

            if (secondsRemaining >= 0) {
                updateListener.onUpdate(secondsRemaining);
            }

            if (secondsRemaining <= 0) {
                stop();
                expiredListener.onExpired();
            }
        });
        swingTimer.start();
    }

    public void stop() {
        if (swingTimer != null && swingTimer.isRunning()) {
            swingTimer.stop();
        }
    }

    public int getTimeRemaining() {
        return Math.max(0, secondsRemaining);
    }
}

// Simple Leaderboard class - stores top 3 scores
class Leaderboard {
    private static final int MAX_ENTRIES = 3;
    private static final String[] MEDALS = {"🥇", "🥈", "🥉"};

    private List<LeaderboardEntry> scores;

    public Leaderboard() {
        this.scores = new ArrayList<>();
    }

    public void addScore(String playerName, int timeInSeconds) {
        scores.add(new LeaderboardEntry(playerName, timeInSeconds));
        // Sort by time - lowest time first (fastest)
        scores.sort((a, b) -> Integer.compare(a.timeInSeconds, b.timeInSeconds));

        if (scores.size() > MAX_ENTRIES) {
            scores.subList(MAX_ENTRIES, scores.size()).clear();
        }
    }

    public void display(Component parent) {
        if (scores.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "🏆 No scores yet!");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("🏆 FASTEST TIMES 🏆\n\n");

        for (int i = 0; i < scores.size(); i++) {
            LeaderboardEntry entry = scores.get(i);
            int mins = entry.timeInSeconds / 60;
            int secs = entry.timeInSeconds % 60;
            text.append(String.format("%s %d. %s - %d:%02d\n",
                    MEDALS[i], i + 1, entry.playerName, mins, secs));
        }

        JOptionPane.showMessageDialog(parent, text.toString());
    }

    private static class LeaderboardEntry {
        String playerName;
        int timeInSeconds;

        LeaderboardEntry(String playerName, int timeInSeconds) {
            this.playerName = playerName;
            this.timeInSeconds = timeInSeconds;
        }
    }
}
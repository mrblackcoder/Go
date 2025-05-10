package client;

import common.Message;
import javax.swing.*;
import java.awt.*;
import client.ui.GoBoardPanel;
import client.ui.ScoringDialog;
import client.ui.TutorialFrame;
import client.ui.GameSettingsDialog;
import client.ui.GameReplayFrame;
import client.ui.SoundEffects;
import game.go.model.Point;
import game.go.model.Stone;
import game.go.util.GameRecorder;
import game.go.util.GameTimer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main application window for the Go game
 */
public class MainFrm extends JFrame {

    // UI components
    private final GoBoardPanel board = new GoBoardPanel();
    private final JTextField txtChatInput = new JTextField(20);
    private final JTextArea chatArea = new JTextArea(10, 30);
    private final JButton btnSendChat = new JButton("Send");
    private final JButton btnPass = new JButton("Pass");
    private final JButton btnResign = new JButton("Resign");
    private final JLabel lblStatus = new JLabel("Status: Connecting...", SwingConstants.LEFT);
    private final JLabel lblBlackTime = new JLabel("00:00", SwingConstants.CENTER);
    private final JLabel lblWhiteTime = new JLabel("00:00", SwingConstants.CENTER);
    private final JButton btnSettings = new JButton("Settings");
    private final JButton btnTutorial = new JButton("Tutorial");
    private final JButton btnLoadGame = new JButton("Load Game");
    private final JCheckBox chkSound = new JCheckBox("Sound", true);

    // Game state
    private boolean myTurn = false;
    private String role = "Unknown";
    private CClient client;
    private final String host;
    private final int port;
    private Point lastMove = null;

    // Timers
    private GameTimer blackTimer;
    private GameTimer whiteTimer;

    // Game recorder
    private GameRecorder gameRecorder;

    // Constants
    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 700;
    private static final Color BOARD_BG_COLOR = new Color(219, 176, 102);
    private static final Color PANEL_BG_COLOR = new Color(240, 230, 210);
    private static final Font STATUS_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font CHAT_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font TIMER_FONT = new Font("Monospaced", Font.BOLD, 16);
    private static final int DEFAULT_TIME_MINUTES = 30;

    /**
     * Creates the main frame
     */
    public MainFrm(String host, int port) {
        super("Go Game");
        this.host = host;
        this.port = port;

        setupUIComponents();
        setupListeners();
        setupKeyboardShortcuts();
        initializeTimers();
        connectToServer();

        // Window properties
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 500));
    }

    /**
     * Sets up UI components and layout
     */
    private void setupUIComponents() {
        // Main colors and styles
        setBackground(PANEL_BG_COLOR);
        board.setBackground(BOARD_BG_COLOR);

        // Main layout
        setLayout(new BorderLayout(10, 10));

        // Status bar (top)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(PANEL_BG_COLOR);

        lblStatus.setFont(STATUS_FONT);
        topPanel.add(lblStatus, BorderLayout.CENTER);

        // Timer panel
        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timerPanel.setBackground(PANEL_BG_COLOR);

        JLabel lblBlackTimerLabel = new JLabel("Black: ");
        lblBlackTimerLabel.setFont(TIMER_FONT);
        lblBlackTime.setFont(TIMER_FONT);
        lblBlackTime.setForeground(Color.BLACK);

        JLabel lblWhiteTimerLabel = new JLabel("White: ");
        lblWhiteTimerLabel.setFont(TIMER_FONT);
        lblWhiteTime.setFont(TIMER_FONT);
        lblWhiteTime.setForeground(Color.DARK_GRAY);

        timerPanel.add(lblBlackTimerLabel);
        timerPanel.add(lblBlackTime);
        timerPanel.add(Box.createHorizontalStrut(20));
        timerPanel.add(lblWhiteTimerLabel);
        timerPanel.add(lblWhiteTime);

        topPanel.add(timerPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Game board (center)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(PANEL_BG_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(board, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Chat panel (right)
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(PANEL_BG_COLOR);
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(CHAT_FONT);
        chatArea.setBackground(new Color(255, 255, 240));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(PANEL_BG_COLOR);
        txtChatInput.setFont(CHAT_FONT);
        btnSendChat.setFont(BUTTON_FONT);

        inputPanel.add(txtChatInput, BorderLayout.CENTER);
        inputPanel.add(btnSendChat, BorderLayout.EAST);

        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.EAST);

        // Control buttons (bottom)
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(PANEL_BG_COLOR);

        JPanel gameControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        gameControlPanel.setBackground(PANEL_BG_COLOR);

        btnPass.setFont(BUTTON_FONT);
        btnResign.setFont(BUTTON_FONT);
        btnSettings.setFont(BUTTON_FONT);
        btnTutorial.setFont(BUTTON_FONT);
        btnLoadGame.setFont(BUTTON_FONT);

        styleButton(btnPass, new Color(80, 120, 220));
        styleButton(btnResign, new Color(220, 80, 80));
        styleButton(btnSettings, new Color(100, 100, 100));
        styleButton(btnTutorial, new Color(50, 150, 50));
        styleButton(btnLoadGame, new Color(60, 145, 180));

        gameControlPanel.add(btnPass);
        gameControlPanel.add(btnResign);

        // Help and settings panel
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        settingsPanel.setBackground(PANEL_BG_COLOR);
        settingsPanel.add(chkSound);
        settingsPanel.add(btnTutorial);
        settingsPanel.add(btnLoadGame);
        settingsPanel.add(btnSettings);

        controlPanel.add(gameControlPanel, BorderLayout.CENTER);
        controlPanel.add(settingsPanel, BorderLayout.EAST);

        add(controlPanel, BorderLayout.SOUTH);

        // Enable hover effect on board
        board.addHoverEffect('B'); // Default as black, will be updated when role is set
    }

    /**
     * Initializes game timers
     */
    private void initializeTimers() {
        blackTimer = new GameTimer(DEFAULT_TIME_MINUTES, lblBlackTime);
        whiteTimer = new GameTimer(DEFAULT_TIME_MINUTES, lblWhiteTime);

        // Set timeout actions
        blackTimer.setTimeoutAction(() -> {
            if (!role.equalsIgnoreCase("BLACK")) {
                showMessageDialog("Black's time is up! You win!", "Time's Up", JOptionPane.INFORMATION_MESSAGE);
                if (client != null) {
                    client.send(new Message(Message.Type.RESIGN, ""));
                }
            }
        });

        whiteTimer.setTimeoutAction(() -> {
            if (!role.equalsIgnoreCase("WHITE")) {
                showMessageDialog("White's time is up! You win!", "Time's Up", JOptionPane.INFORMATION_MESSAGE);
                if (client != null) {
                    client.send(new Message(Message.Type.RESIGN, ""));
                }
            }
        });
    }

    /**
     * Styles a button with the specified color
     */
    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Sets up event listeners for UI components
     */
    private void setupListeners() {
        // Chat send button
        btnSendChat.addActionListener(e -> sendChatMessage());
        txtChatInput.addActionListener(e -> sendChatMessage());

        // Pass button
        btnPass.addActionListener(e -> {
            if (client != null) {
                if (!myTurn) {
                    showTurnWarning();
                    return;
                }
                client.send(new Message(Message.Type.PASS, ""));

                // Record the pass
                if (gameRecorder != null && gameInProgress) {
                    Stone playerStone = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
                    gameRecorder.recordPass(playerStone);
                    System.out.println("Pass recorded by " + playerStone);
                }

                // Stop timer
                if (role.equalsIgnoreCase("BLACK")) {
                    blackTimer.stop();
                    whiteTimer.start();
                } else {
                    whiteTimer.stop();
                    blackTimer.start();
                }

                myTurn = false;
                updateStatusVisuals();

                // Play sound
                SoundEffects.play("stone_place");

                // Clear last move highlight
                lastMove = null;
                board.setLastMove(null);
            }
        });

        // Modify the Resign button action in setupListeners()
        btnResign.addActionListener(e -> {
            if (client != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to resign?",
                        "Confirm Resignation",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    client.send(new Message(Message.Type.RESIGN, ""));

                    // Record the resignation
                    if (gameRecorder != null && gameInProgress) {
                        Stone playerStone = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
                        gameRecorder.recordResign(playerStone);
                        System.out.println("Resignation recorded by " + playerStone);
                        gameInProgress = false; // Mark game as ended
                    }

                    // Stop timers
                    blackTimer.stop();
                    whiteTimer.stop();

                    myTurn = false;
                    updateStatusVisuals();

                    // Play sound
                    SoundEffects.play("game_end");
                }
            }
        });
        // Tutorial button
        btnTutorial.addActionListener(e -> {
            TutorialFrame.showTutorial();
        });

        // Settings button
        btnSettings.addActionListener(e -> {
            showSettingsDialog();
        });

        // Load Game button
        btnLoadGame.addActionListener(e -> {
            loadGameRecord();
        });

        // Sound checkbox
        chkSound.addActionListener(e -> {
            SoundEffects.enableSound(chkSound.isSelected());
        });

        // Board click handler
        board.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (client == null || !myTurn) {
                    if (client != null && !myTurn) {
                        showTurnWarning();
                    }
                    return;
                }

                Point boardCoord = board.getBoardCoordinates(e.getX(), e.getY());

                // Check if click is valid
                if (boardCoord != null) {
                    // Save last move and highlight it
                    lastMove = boardCoord;
                    board.setLastMove(lastMove);

                    // Send move to server
                    client.send(new Message(Message.Type.MOVE, boardCoord.x() + "," + boardCoord.y()));

                    // Record the move
                    if (gameRecorder != null && gameInProgress) {
                        Stone playerStone = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
                        gameRecorder.recordMove(boardCoord, playerStone);
                        System.out.println("Move recorded: " + playerStone + " at " + boardCoord.x() + "," + boardCoord.y());
                    }

                    // Update turn status
                    myTurn = false;
                    updateStatusVisuals();

                    // Switch timers
                    if (role.equalsIgnoreCase("BLACK")) {
                        blackTimer.stop();
                        whiteTimer.start();
                    } else {
                        whiteTimer.stop();
                        blackTimer.start();
                    }

                    // Play sound
                    SoundEffects.play("stone_place");
                }
            }
        });
    }
        /**
         * Sets up keyboard shortcuts
         */
    private void setupKeyboardShortcuts() {
        // Create key bindings map
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Add shortcut for pass (P key)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "pass");
        actionMap.put("pass", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client != null && myTurn) {
                    btnPass.doClick();
                } else if (client != null && !myTurn) {
                    showTurnWarning();
                }
            }
        });

        // Add shortcut for chat focus (C key)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "chat");
        actionMap.put("chat", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtChatInput.requestFocusInWindow();
            }
        });

        // Add shortcut to show rules (F1 key)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "rules");
        actionMap.put("rules", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRules();
            }
        });

        // Add shortcut for resign (R key)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "resign");
        actionMap.put("resign", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (client != null && myTurn) {
                    btnResign.doClick();
                } else if (client != null && !myTurn) {
                    showTurnWarning();
                }
            }
        });

        // Add shortcut for tutorial (T key)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), "tutorial");
        actionMap.put("tutorial", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnTutorial.doClick();
            }
        });

        // Add shortcut for load game (L key)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "loadGame");
        actionMap.put("loadGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadGameRecord();
            }
        });
    }

    /**
     * Shows a settings dialog
     */
    private void showSettingsDialog() {
        GameSettingsDialog dialog = GameSettingsDialog.showDialog(this);

        if (dialog.isConfirmed()) {
            // Apply settings
            int timeControl = dialog.getTimeControl();

            // Reset timers
            blackTimer.reset(timeControl);
            whiteTimer.reset(timeControl);

            // Update sound setting
            SoundEffects.enableSound(dialog.isSoundEnabled());
            chkSound.setSelected(dialog.isSoundEnabled());

            showMessageDialog("Settings applied. Time control set to " + timeControl + " minutes.",
                    "Settings Updated", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Shows the Go game rules
     */
    private void showRules() {
        JTextArea rulesText = new JTextArea();
        rulesText.setEditable(false);
        rulesText.setLineWrap(true);
        rulesText.setWrapStyleWord(true);
        rulesText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        rulesText.setText(
                "Go Game Basic Rules:\n\n"
                + "1. The game is played by placing stones alternately. Black starts.\n\n"
                + "2. Once placed, stones cannot be moved, but they can be captured.\n\n"
                + "3. Capturing: When all liberties (adjacent empty points) of a stone or group of stones are filled by opponent stones, those stones are captured and removed from the board.\n\n"
                + "4. 'Ko' rule: A player cannot make a move that would recreate the board position after their previous move.\n\n"
                + "5. 'Suicide' rule: A player cannot place a stone where it would have no liberties, unless it captures opponent stones in the process.\n\n"
                + "6. The game ends when both players pass in succession.\n\n"
                + "7. Scoring: Territory points + captured stones. White typically receives a komi (compensation points) for going second."
        );

        JScrollPane scrollPane = new JScrollPane(rulesText);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Go Game Rules", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a "not your turn" warning
     */
    private void showTurnWarning() {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(this,
                "It's not your turn!",
                "Warning",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Sends a chat message
     */
    private void sendChatMessage() {
        if (client == null) {
            return;
        }

        String msg = txtChatInput.getText().trim();
        if (!msg.isEmpty()) {
            client.send(new Message(Message.Type.TO_CLIENT, msg));
            txtChatInput.setText("");
            txtChatInput.requestFocus();
        }
    }

    /**
     * Updates turn and status indicators
     */
    private void updateStatusVisuals() {
        SwingUtilities.invokeLater(() -> {
            String turnIndicator = myTurn ? "Your Turn" : "Waiting for Opponent";
            lblStatus.setText("Role: " + this.role + " | " + turnIndicator);
            setTitle("Go Game - " + this.role + " | " + turnIndicator);

            // Change color based on turn
            lblStatus.setForeground(myTurn ? new Color(0, 120, 0) : new Color(120, 0, 0));

            // Update button states
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);
        });
    }

    /**
     * Updates status with score and turn information
     */
    public void updateStatus(int me, int opp, String whoseTurn) {
        final String turnIndicator;

        if (this.role == null || this.role.equals("Unknown")
                || this.role.equals("Connecting...") || whoseTurn == null || whoseTurn.isEmpty()) {
            turnIndicator = "";
            this.myTurn = false;
        } else {
            this.myTurn = whoseTurn.equalsIgnoreCase(this.role);
            turnIndicator = myTurn ? "Your Turn" : "Waiting for Opponent";

            // Update timers based on turn
            if (myTurn) {
                if (role.equalsIgnoreCase("BLACK")) {
                    blackTimer.start();
                    whiteTimer.stop();
                } else {
                    whiteTimer.start();
                    blackTimer.stop();
                }
            }
        }

        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Role: " + this.role + " | Score: " + me + " (You) : " + opp + " (Opponent) | " + turnIndicator);
            setTitle("Go Game - " + this.role + " [" + me + ":" + opp + "] " + turnIndicator);
            lblStatus.setForeground(myTurn ? new Color(0, 120, 0) : new Color(120, 0, 0));

            // Update button states
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);
        });
    }

    /**
     * Sets the player's role
     */
    private boolean gameInProgress = false;

// Modify the setRole method to reset the game recorder
    public void setRole(String r) {
        this.role = r;
        this.myTurn = r.equalsIgnoreCase("BLACK");
        final String initialTurn = myTurn ? "Your Turn" : "Waiting for Opponent";

        // Initialize game recorder
        gameRecorder = new GameRecorder(19, "Player (" + r + ")", "Opponent");
        gameRecorder.enableRecording(true); // Explicitly enable recording
        gameInProgress = true; // Mark game as in progress

        // Set hover color based on role
        board.addHoverEffect(r.equalsIgnoreCase("BLACK") ? 'B' : 'W');

        // Start timers based on role
        if (myTurn) {
            blackTimer.start();
        } else {
            whiteTimer.start();
        }

        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Role: " + this.role + " | Score: 0:0 | " + initialTurn);
            setTitle("Go Game - " + this.role + " | " + initialTurn);
            lblStatus.setForeground(myTurn ? new Color(0, 120, 0) : new Color(120, 0, 0));

            // Update button states
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);

            // Notify user of role assignment
            showChat("Your Role: " + this.role + (myTurn ? " (You start the game)" : " (Opponent starts)"));

            // Play game start sound
            SoundEffects.play("game_start");
        });
    }

    /**
     * Displays a chat message
     */
    public void showChat(String payload) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(payload + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /**
     * Shows an error message
     */
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            SoundEffects.play("error");
        });
    }

    /**
     * Shows a connection error message
     */
    public void showConnectionError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Connection Error", JOptionPane.ERROR_MESSAGE);
            SoundEffects.play("error");
        });
    }

    /**
     * Shows a custom message dialog
     */
    private void showMessageDialog(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, title, messageType);
        });
    }

    /**
     * Shows the game over dialog
     */
    public void showGameOverDialog(String result, int myScore, int oppScore, String reason) {
        // Stop timers
        blackTimer.stop();
        whiteTimer.stop();

        // Mark game as ended
        gameInProgress = false;

        // Debug output - print recorded moves
        if (gameRecorder != null) {
            gameRecorder.printMoves();
        }

        // Play game end sound
        SoundEffects.play("game_end");

        SwingUtilities.invokeLater(() -> {
            // Create dialog content
            JPanel panel = new JPanel(new BorderLayout(10, 10));

            JLabel resultLabel = new JLabel(result, JLabel.CENTER);
            resultLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

            JLabel scoreLabel = new JLabel("Score: " + myScore + " (You) : " + oppScore + " (Opponent)", JLabel.CENTER);
            scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

            JLabel reasonLabel = new JLabel(reason != null && !reason.isEmpty() ? "Reason: " + reason : "", JLabel.CENTER);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(resultLabel, BorderLayout.NORTH);
            topPanel.add(scoreLabel, BorderLayout.CENTER);
            topPanel.add(reasonLabel, BorderLayout.SOUTH);

            panel.add(topPanel, BorderLayout.NORTH);

            // Add buttons for post-game actions
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

            JButton btnSaveGame = new JButton("Save Game Record");
            JButton btnNewGame = new JButton("New Game");
            JButton btnViewBoard = new JButton("View Final Position");

            // Show move count in the save button
            if (gameRecorder != null) {
                int moveCount = gameRecorder.getMoveCount();
                btnSaveGame.setText("Save Game Record (" + moveCount + " moves)");
            }

            btnSaveGame.addActionListener(e -> saveGameRecord());
            btnNewGame.addActionListener(e -> {
                // Reconnect to server for a new game
                if (client != null) {
                    client.close();
                }
                connectToServer();
            });
            btnViewBoard.addActionListener(e -> ScoringDialog.showScoringDialog(null));

            buttonPanel.add(btnSaveGame);
            buttonPanel.add(btnNewGame);
            buttonPanel.add(btnViewBoard);

            panel.add(buttonPanel, BorderLayout.SOUTH);

            // Show custom dialog
            JOptionPane.showOptionDialog(
                    this,
                    panel,
                    "Game Over",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    new Object[]{},
                    null
            );
        });
    }

    /**
     * Saves the game record to a file
     */
// Modify the saveGameRecord method to add better debugging
    private void saveGameRecord() {
        if (gameRecorder == null) {
            showMessageDialog("No game recorder available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int moveCount = gameRecorder.getMoveCount();
        if (moveCount == 0) {
            showMessageDialog("No moves to save (move count: 0).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Debug output
        System.out.println("Saving game with " + moveCount + " moves");
        gameRecorder.printMoves();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Game Record");

        // Set default file name with timestamp and move count
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String defaultFileName = "go_game_" + sdf.format(new Date()) + "_" + moveCount + "moves.sgf";
        fileChooser.setSelectedFile(new File(defaultFileName));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // Add .sgf extension if missing
            if (!file.getName().toLowerCase().endsWith(".sgf")) {
                file = new File(file.getAbsolutePath() + ".sgf");
            }

            try {
                gameRecorder.saveToSgf(file.getAbsolutePath());
                showMessageDialog("Game record saved successfully to " + file.getName()
                        + "\nMoves saved: " + moveCount,
                        "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                showMessageDialog("Failed to save game record: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads a game record from an SGF file and displays it
     */
    private void loadGameRecord() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Game Record");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".sgf");
            }

            @Override
            public String getDescription() {
                return "Smart Game Format (*.sgf)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                GameRecorder loadedGame = GameRecorder.loadFromSgf(file.getAbsolutePath());
                showGameReplayDialog(loadedGame);
            } catch (IOException | ParseException e) {
                showMessageDialog("Failed to load game record: " + e.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Shows a dialog to replay a loaded game
     */
    private void showGameReplayDialog(GameRecorder gameRecord) {
        if (gameRecord == null || gameRecord.getMoveCount() == 0) {
            showMessageDialog("No moves to replay in this game record.",
                    "Empty Game", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Create a custom dialog for replay options
        JDialog replayOptionsDialog = new JDialog(this, "Game Replay Options", true);
        replayOptionsDialog.setLayout(new BorderLayout());

        // Create panel for replay information
        JPanel infoPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add game information
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        infoPanel.add(new JLabel("Game: " + gameRecord.getBlackPlayer() + " (Black) vs "
                + gameRecord.getWhitePlayer() + " (White)"));
        infoPanel.add(new JLabel("Date: " + sdf.format(gameRecord.getGameDate())));
        infoPanel.add(new JLabel("Board Size: " + gameRecord.getBoardSize() + "×" + gameRecord.getBoardSize()));
        infoPanel.add(new JLabel("Komi: " + gameRecord.getKomi()));
        infoPanel.add(new JLabel("Moves: " + gameRecord.getMoveCount()));

        replayOptionsDialog.add(infoPanel, BorderLayout.CENTER);

        // Create panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton btnReplay = new JButton("Replay Game");
        JButton btnCancel = new JButton("Cancel");

        btnReplay.addActionListener(e -> {
            replayOptionsDialog.dispose();

            // Open game replay window
            GameReplayFrame replayFrame = new GameReplayFrame(
                    gameRecord.getBlackPlayer() + " vs " + gameRecord.getWhitePlayer(),
                    gameRecord.getMoves(),
                    gameRecord.getBoardSize());
            replayFrame.showReplay();
        });

        btnCancel.addActionListener(e -> {
            replayOptionsDialog.dispose();
        });

        buttonPanel.add(btnReplay);
        buttonPanel.add(btnCancel);

        replayOptionsDialog.add(buttonPanel, BorderLayout.SOUTH);

        // Set dialog properties
        replayOptionsDialog.pack();
        replayOptionsDialog.setLocationRelativeTo(this);
        replayOptionsDialog.setResizable(false);
        replayOptionsDialog.setVisible(true);
    }

    /**
     * Handles disconnection from the server
     */
    public void handleDisconnect() {
        SwingUtilities.invokeLater(() -> {
            // Stop timers
            blackTimer.stop();
            whiteTimer.stop();

            myTurn = false;
            updateStatusVisuals();
            showChat("Disconnected from server.");

            // Offer to reconnect
            int option = JOptionPane.showConfirmDialog(this,
                    "You have been disconnected from the server. Would you like to reconnect?",
                    "Disconnected",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                connectToServer();
            }
        });
    }

    /**
     * Connects to the server
     */
    private void connectToServer() {
        try {
            client = new CClient(host, port, this);
            client.start();
            showChat("Connecting to server: " + host + ":" + port);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to connect to server:\n" + ex.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Returns the game board panel
     */
    public GoBoardPanel getBoard() {
        return board;
    }

    /**
     * Main method - starts the application
     */
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get host and port from arguments or use defaults
        String host = args.length > 0 ? args[0] : "localhost";
        int port = 6000;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1] + ". Using default port (6000).");
            }
        }

        final String finalHost = host;
        final int finalPort = port;
        SwingUtilities.invokeLater(() -> {
            new MainFrm(finalHost, finalPort).setVisible(true);
        });
    }
}

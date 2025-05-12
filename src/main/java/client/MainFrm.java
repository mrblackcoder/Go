package client;

import common.Message;
import javax.swing.*;
import java.awt.*;
import client.ui.GoBoardPanel;
import client.ui.ScoringDialog;
import client.ui.TutorialFrame;
import client.ui.SoundEffects;
import game.go.model.GameGoModel;
import game.go.model.Point;
import game.go.model.Stone;
import game.go.util.GameRecorder;
import game.go.util.GameTimer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.border.TitledBorder;

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
    private final JButton btnTutorial = new JButton("Tutorial");
    private final JCheckBox chkSound = new JCheckBox("Sound", true);
    private final JLabel lblConnectionStatus = new JLabel("⚠️ Not Connected", SwingConstants.RIGHT);
    private final JPanel topPanel = new JPanel(new BorderLayout(5, 0));
    private final JButton btnNewGameMain = new JButton("New Game");
    private static final Logger LOGGER = Logger.getLogger(MainFrm.class.getName());
    private int cachedMyScore = 0;
    private int cachedOppScore = 0;
    private boolean selfResigned = false;   // Bu istemci resign’e bastı mı?
    private boolean gameOverShown = false;   // Diyalog bir kez gösterildi mi?

    private GameGoModel gameModel = new GameGoModel(19); // 19x19 standart Go tahtası
    // Zamanı daha belirgin göstermek için özel panel ve etiketler
    private JPanel timerPanel;
    private JLabel myTimeLabel;
    private JLabel opponentTimeLabel;
    private Preferences preferences = Preferences.userNodeForPackage(MainFrm.class);
    // Zaman uyarı bayrakları
    private boolean timeWarningPlayed = false;
    private boolean criticalWarningPlayed = false;
    private boolean intentionalDisconnect = false;

    // Game state
    private boolean myTurn = false;
    private String role = "Unknown";
    private CClient client;
    private final String host;
    private final int port;
    private Point lastMove = null;
    private boolean gameInProgress = false;
    private boolean newGameInProgress = false;

    // Timers
    private GameTimer blackTimer;
    private GameTimer whiteTimer;

    // Game recorder
    private GameRecorder gameRecorder;

    //Constants
    private int DEFAULT_WIDTH = 900;
    private int DEFAULT_HEIGHT = 700;
    private Color BOARD_BG_COLOR = new Color(219, 176, 102);
    private Color PANEL_BG_COLOR = new Color(240, 230, 210);
    private static final Font STATUS_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font CHAT_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font TIMER_FONT = new Font("Monospaced", Font.BOLD, 16);
    private static final int DEFAULT_TIME_MINUTES = 30;

    /**
     * Creates the main frame
     *
     * @param host Server host address
     * @param port Server port
     */
    public MainFrm(String host, int port) {
        super("Go Game");
        this.host = host;
        this.port = port;

        setupUIComponents();
        setupListeners();
        initializeTimers();
        connectToServer();

        // Window properties
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 500));
    }

    private void setupUIComponents() {
        // Define dark theme colors
        Color DARK_BG = Color.BLACK;
        Color DARK_SECONDARY = new Color(20, 20, 20);
        Color DARK_BORDER = new Color(50, 50, 50);
        Color DARK_HIGHLIGHT = new Color(40, 40, 40);
        Color TEXT_COLOR = new Color(220, 220, 220);

        // Main colors and styles
        setBackground(DARK_BG);
        getContentPane().setBackground(DARK_BG);
        PANEL_BG_COLOR = DARK_BG;
        BOARD_BG_COLOR = new Color(40, 40, 40); // Dark gray for board background
        board.setBackground(BOARD_BG_COLOR);

        // Main layout
        setLayout(new BorderLayout(10, 10));

        // Status bar (top)
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
        topPanel.setBackground(DARK_BG);
        lblStatus.setFont(STATUS_FONT);
        lblStatus.setForeground(TEXT_COLOR);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(DARK_BG);
        topPanel.add(lblStatus, BorderLayout.CENTER);

        // Timer panel (enhanced version)
        timerPanel = new JPanel(new BorderLayout());
        timerPanel.setBackground(DARK_BG);
        timerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                "Game Time",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                STATUS_FONT,
                TEXT_COLOR
        ));

        JPanel timeLabelsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        timeLabelsPanel.setBackground(DARK_BG);

        myTimeLabel = new JLabel("00:00", JLabel.CENTER);
        myTimeLabel.setFont(TIMER_FONT);
        myTimeLabel.setForeground(new Color(60, 180, 255)); // Bright blue
        myTimeLabel.setOpaque(true);
        myTimeLabel.setBackground(DARK_BG);

        opponentTimeLabel = new JLabel("00:00", JLabel.CENTER);
        opponentTimeLabel.setFont(TIMER_FONT);
        opponentTimeLabel.setForeground(new Color(255, 80, 80)); // Bright red
        opponentTimeLabel.setOpaque(true);
        opponentTimeLabel.setBackground(DARK_BG);

        JLabel myTimeCaption = new JLabel("Your Time:", JLabel.RIGHT);
        myTimeCaption.setForeground(TEXT_COLOR);
        myTimeCaption.setOpaque(true);
        myTimeCaption.setBackground(DARK_BG);

        JLabel oppTimeCaption = new JLabel("Opponent's Time:", JLabel.RIGHT);
        oppTimeCaption.setForeground(TEXT_COLOR);
        oppTimeCaption.setOpaque(true);
        oppTimeCaption.setBackground(DARK_BG);

        timeLabelsPanel.add(myTimeCaption);
        timeLabelsPanel.add(myTimeLabel);
        timeLabelsPanel.add(oppTimeCaption);
        timeLabelsPanel.add(opponentTimeLabel);
        timerPanel.add(timeLabelsPanel, BorderLayout.CENTER);

        // Original timer elements
        JPanel classicTimerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        classicTimerPanel.setBackground(DARK_BG);

        JLabel lblBlackTimerLabel = new JLabel("Black: ");
        lblBlackTimerLabel.setFont(TIMER_FONT);
        lblBlackTimerLabel.setForeground(TEXT_COLOR);
        lblBlackTimerLabel.setOpaque(true);
        lblBlackTimerLabel.setBackground(DARK_BG);

        lblBlackTime.setFont(TIMER_FONT);
        lblBlackTime.setForeground(new Color(200, 200, 200)); // Light gray for black stones
        lblBlackTime.setOpaque(true);
        lblBlackTime.setBackground(DARK_BG);

        JLabel lblWhiteTimerLabel = new JLabel("White: ");
        lblWhiteTimerLabel.setFont(TIMER_FONT);
        lblWhiteTimerLabel.setForeground(TEXT_COLOR);
        lblWhiteTimerLabel.setOpaque(true);
        lblWhiteTimerLabel.setBackground(DARK_BG);

        lblWhiteTime.setFont(TIMER_FONT);
        lblWhiteTime.setForeground(Color.WHITE); // White for white stones
        lblWhiteTime.setOpaque(true);
        lblWhiteTime.setBackground(DARK_BG);

        classicTimerPanel.add(lblBlackTimerLabel);
        classicTimerPanel.add(lblBlackTime);
        classicTimerPanel.add(Box.createHorizontalStrut(20));
        classicTimerPanel.add(lblWhiteTimerLabel);
        classicTimerPanel.add(lblWhiteTime);

        // Add the new timer panel to topPanel
        topPanel.add(timerPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Game board (center)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(DARK_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(board, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Connection status indicator
        lblConnectionStatus.setFont(STATUS_FONT);
        lblConnectionStatus.setForeground(new Color(255, 80, 80)); // Brighter red for visibility
        lblConnectionStatus.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        lblConnectionStatus.setOpaque(true);
        lblConnectionStatus.setBackground(DARK_BG);
        topPanel.add(lblConnectionStatus, BorderLayout.WEST);

        // Chat panel (right)
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(DARK_BG);
        chatPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                "Chat",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                CHAT_FONT,
                TEXT_COLOR
        ));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(CHAT_FONT);
        chatArea.setBackground(DARK_SECONDARY);
        chatArea.setForeground(TEXT_COLOR);
        chatArea.setCaretColor(Color.WHITE);

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        chatScroll.getViewport().setBackground(DARK_SECONDARY);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(DARK_BG);

        txtChatInput.setFont(CHAT_FONT);
        txtChatInput.setBackground(DARK_SECONDARY);
        txtChatInput.setForeground(Color.WHITE);
        txtChatInput.setCaretColor(Color.WHITE);

        btnSendChat.setFont(BUTTON_FONT);
        styleButtonDark(btnSendChat, new Color(60, 60, 60));

        inputPanel.add(txtChatInput, BorderLayout.CENTER);
        inputPanel.add(btnSendChat, BorderLayout.EAST);

        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.EAST);

        // Control buttons (bottom)
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(DARK_BG);

        JPanel gameControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        gameControlPanel.setBackground(DARK_BG);

        btnPass.setFont(BUTTON_FONT);
        btnResign.setFont(BUTTON_FONT);
        btnTutorial.setFont(BUTTON_FONT);
        btnNewGameMain.setFont(BUTTON_FONT);

        styleButtonDark(btnPass, new Color(50, 90, 180));
        styleButtonDark(btnResign, new Color(180, 50, 50));
        styleButtonDark(btnTutorial, new Color(40, 120, 40));
        styleButtonDark(btnNewGameMain, new Color(50, 150, 50));

        gameControlPanel.add(btnPass);
        gameControlPanel.add(btnResign);
        gameControlPanel.add(btnNewGameMain);

        // Help and settings panel
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        settingsPanel.setBackground(DARK_BG);

        chkSound.setForeground(TEXT_COLOR);
        chkSound.setBackground(DARK_BG);

        settingsPanel.add(chkSound);
        settingsPanel.add(btnTutorial);

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
                if (client != null && client.isConnected()) {
                    client.send(new Message(Message.Type.RESIGN, ""));
                }
            }
        });

        whiteTimer.setTimeoutAction(() -> {
            if (!role.equalsIgnoreCase("WHITE")) {
                showMessageDialog("White's time is up! You win!", "Time's Up", JOptionPane.INFORMATION_MESSAGE);
                if (client != null && client.isConnected()) {
                    client.send(new Message(Message.Type.RESIGN, ""));
                }
            }
        });

        // Uyarı aksiyonları ekle
        blackTimer.setWarningAction(() -> {
            if (role.equalsIgnoreCase("BLACK") && chkSound.isSelected()) {
                SoundEffects.play(SoundEffects.TIME_WARNING);
            }
        });

        blackTimer.setCriticalAction(() -> {
            if (role.equalsIgnoreCase("BLACK") && chkSound.isSelected()) {
                SoundEffects.play(SoundEffects.TIME_CRITICAL);
            }
        });

        whiteTimer.setWarningAction(() -> {
            if (role.equalsIgnoreCase("WHITE") && chkSound.isSelected()) {
                SoundEffects.play(SoundEffects.TIME_WARNING);
            }
        });

        whiteTimer.setCriticalAction(() -> {
            if (role.equalsIgnoreCase("WHITE") && chkSound.isSelected()) {
                SoundEffects.play(SoundEffects.TIME_CRITICAL);
            }
        });
    }

    /**
     * Styles a button with the specified color
     *
     * @param button Button to style
     * @param color Background color
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

        // ----- RESIGN düğmesi --------------------------------------------------
btnResign.addActionListener(e -> {
    if (client == null) return;

    int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to resign?",
            "Confirm Resignation",
            JOptionPane.YES_NO_OPTION);

    if (confirm != JOptionPane.YES_OPTION) return;

    /* Bayraklar: bu istemci resign’e bastı */
    selfResigned  = true;
    gameOverShown = false;   // Yeni diyaloğa izin ver

    /* 1) Sunucuya rol içeren resign mesajı gönder */
    client.send(new Message(Message.Type.RESIGN, role));   // "BLACK" / "WHITE"

    /* 2) Kayıt tut */
    if (gameRecorder != null && gameInProgress) {
        Stone me = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
        gameRecorder.recordResign(me);
        System.out.println("Resignation recorded by " + me);
    }

    /* 3) Oyun durumunu kapat */
    blackTimer.stop();
    whiteTimer.stop();
    gameInProgress = false;
    myTurn = false;
    updateStatusVisuals();

    /* 4) Hemen “You lost – you resigned.” diyaloğunu göster */
    showGameOverDialog(
            "",                 // resultIgnored
            cachedMyScore,      // kendi skor
            cachedOppScore,     // rakip skor
            "RESIGN:" + role);  // reason – renk içeriyor

    /* 5) Ses efekti */
    SoundEffects.play("game_end");
});

        // Tutorial button
        btnTutorial.addActionListener(e -> {
            TutorialFrame.showTutorial();
        });

        // Sound checkbox
        chkSound.addActionListener(e -> {
            SoundEffects.enableSound(chkSound.isSelected());
        });

        btnNewGameMain.addActionListener(e -> {
            // Aktif oyunda mıyız kontrol et
            if (gameInProgress) {
                // Kullanıcıya onay sor
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to start a new game?",
                        "Confirm New Game",
                        JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    // New Game durumunu başlat
                    newGameInProgress = true;

                    // Mevcut bağlantıyı kapat - bu handleNewGameReconnect'i tetikleyecek
                    if (client != null) {
                        client.close();
                    } else {
                        // Client yoksa direkt olarak yeni bağlantı metodunu çağır
                        handleNewGameReconnect();
                    }
                }
            } else {
                // Oyun aktif değilse, direkt new game işlemini başlat
                newGameInProgress = true;

                // Mevcut bağlantıyı kapat - bu handleNewGameReconnect'i tetikleyecek
                if (client != null) {
                    client.close();
                } else {
                    // Client yoksa direkt olarak yeni bağlantı metodunu çağır
                    handleNewGameReconnect();
                }
            }
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

                    // Hamleyi kayıt altına al - düzeltilmiş kod
                    if (gameRecorder != null && gameInProgress) {
                        Stone playerStone = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
                        registerMoveInRecorder(boardCoord, playerStone);
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
     * Shows a "not your turn" warning
     */
    private void showTurnWarning() {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(this,
                "It's not your turn!",
                "Warning",
                JOptionPane.WARNING_MESSAGE);
    }

    public boolean isNewGameInProgress() {
        return newGameInProgress;
    }

    /**
     * Connects to the server
     */
    private void connectToServer() {
        try {
            System.out.println("Attempting to connect to " + host + ":" + port + "...");
            showChat("Trying to connect to server at " + host + ":" + port + "...");

            // DNS çözümleme testi
            try {
                java.net.InetAddress address = java.net.InetAddress.getByName(host);
                System.out.println("Successfully resolved " + host + " to " + address.getHostAddress());
                showChat("Server address resolved: " + address.getHostAddress());
            } catch (java.net.UnknownHostException uhe) {
                System.err.println("Could not resolve host: " + host);
                showChat("Warning: Could not resolve host: " + host);
            }

            // Bağlantı öncesi UI durumunu güncelle
            lblStatus.setText("Status: Connecting to " + host + ":" + port + "...");

            long startTime = System.currentTimeMillis();
            client = new CClient(host, port, this);
            long endTime = System.currentTimeMillis();
            System.out.println("Connection attempt took " + (endTime - startTime) + "ms");

            client.start();

            showChat("Successfully connected to server: " + host + ":" + port);
            updateConnectionStatus(true);

            // Server'a yeni oyun için hazır olduğumuzu bildirelim
            sendReadyForNewGame();

        } catch (Exception ex) {
            ex.printStackTrace(); // Tam stack trace yazdır

            showChat("Failed to connect to server: " + ex.getMessage());
            System.err.println("Connection error details: " + ex);

            updateConnectionStatus(false);

            // Kullanıcıya sunucuya tekrar bağlanmayı denemek isteyip istemediğini sor
            int option = JOptionPane.showConfirmDialog(this,
                    "Failed to connect to server:\n" + ex.getMessage()
                    + "\n\nWould you like to retry connecting?",
                    "Connection Error",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                retryConnection();
            }
        }
    }

    /**
     * Styles a button with dark theme
     *
     * @param button Button to style
     * @param color Background color
     */
    private void styleButtonDark(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Retry connecting to server with delay
     */
    private void retryConnection() {
        new Thread(() -> {
            try {
                for (int i = 5; i > 0; i--) {
                    final int countdown = i;
                    SwingUtilities.invokeLater(() -> {
                        showChat("Retrying connection in " + countdown + " seconds...");
                        lblStatus.setText("Retrying connection in " + countdown + " seconds...");
                    });
                    Thread.sleep(1000);
                }

                SwingUtilities.invokeLater(() -> {
                    showChat("Retrying connection to " + host + ":" + port + "...");
                    connectToServer();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Updates the connection status display
     *
     * @param connected True if connected to server
     */
    private void updateConnectionStatus(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                lblConnectionStatus.setText("✓ Connected");
                lblConnectionStatus.setForeground(new Color(0, 150, 0));
            } else {
                lblConnectionStatus.setText("⚠️ Not Connected");
                lblConnectionStatus.setForeground(new Color(200, 0, 0));
            }
        });
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
     * Updates turn and status indicators with a dark theme
     */
    private void updateStatusVisuals() {
        SwingUtilities.invokeLater(() -> {
            String turnIndicator = myTurn ? "YOUR TURN" : "Waiting for Opponent";
            lblStatus.setText("Role: " + this.role + " | " + turnIndicator);
            setTitle("Go Game - " + this.role + " | " + turnIndicator);

            // Update status label based on turn
            lblStatus.setOpaque(true);
            lblStatus.setBackground(Color.BLACK);

            if (myTurn) {
                // Highlight everything when it's your turn
                lblStatus.setForeground(new Color(0, 255, 0)); // Bright green
                lblStatus.setFont(new Font(STATUS_FONT.getFamily(), Font.BOLD, STATUS_FONT.getSize() + 2));
                lblStatus.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 255, 0), 2),
                        BorderFactory.createEmptyBorder(3, 5, 3, 5)
                ));

                // Always add the green border to the board when it's your turn
                board.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 0), 3));

                // Flash effect for the board border to catch attention
                new Thread(() -> {
                    try {
                        for (int i = 0; i < 3; i++) {
                            SwingUtilities.invokeLater(()
                                    -> board.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 0), 3)));
                            Thread.sleep(200);
                            SwingUtilities.invokeLater(()
                                    -> board.setBorder(BorderFactory.createLineBorder(new Color(60, 255, 60), 3)));
                            Thread.sleep(200);
                        }
                        SwingUtilities.invokeLater(()
                                -> board.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 0), 3)));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                // Normal style when waiting for opponent
                lblStatus.setForeground(new Color(255, 80, 80)); // Bright red
                lblStatus.setFont(STATUS_FONT);
                lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

                // Remove board border
                board.setBorder(null);
            }

            // Update button states
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);

            // Update New Game button style based on game state
            if (gameInProgress) {
                btnNewGameMain.setText("End & New Game");
                btnNewGameMain.setToolTipText("End current game and start a new one");
            } else {
                btnNewGameMain.setText("New Game");
                btnNewGameMain.setToolTipText("Start a new game");
            }

            // Update timer panel with dark theme
            if (timerPanel != null) {
                timerPanel.setOpaque(true);
                timerPanel.setBackground(Color.BLACK);

                if (myTurn) {
                    timerPanel.setBorder(BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(new Color(0, 255, 0), 3),
                            "GAME TIME - YOUR TURN",
                            TitledBorder.CENTER,
                            TitledBorder.TOP,
                            new Font("SansSerif", Font.BOLD, 14),
                            new Color(0, 255, 0)
                    ));

                    // Make player time more prominent
                    myTimeLabel.setOpaque(true);
                    myTimeLabel.setBackground(Color.BLACK);
                    myTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
                    myTimeLabel.setForeground(new Color(0, 255, 0)); // Bright green

                    opponentTimeLabel.setOpaque(true);
                    opponentTimeLabel.setBackground(Color.BLACK);
                    opponentTimeLabel.setForeground(new Color(180, 180, 180));
                } else {
                    timerPanel.setBorder(BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(new Color(255, 80, 80), 1),
                            "Game Time - Opponent's Turn",
                            TitledBorder.CENTER,
                            TitledBorder.TOP,
                            new Font("SansSerif", Font.PLAIN, 12),
                            new Color(255, 80, 80)
                    ));

                    // Reset time label
                    myTimeLabel.setOpaque(true);
                    myTimeLabel.setBackground(Color.BLACK);
                    myTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
                    myTimeLabel.setForeground(new Color(180, 180, 180));

                    opponentTimeLabel.setOpaque(true);
                    opponentTimeLabel.setBackground(Color.BLACK);
                    opponentTimeLabel.setForeground(new Color(255, 80, 80));
                }
            }
        });
    }

    /**
     * Updates status with score and turn information
     *
     * @param me My score
     * @param opp Opponent's score
     * @param whoseTurn Whose turn it is (BLACK/WHITE)
     */
    public void updateStatus(int me, int opp, String whoseTurn) {
        final String turnIndicator;
        this.cachedMyScore = me;
        this.cachedOppScore = opp;
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

            // Sıradaki oyuncuya göre timer panelini güncelle
            if (timerPanel != null) {
                if (myTurn) {
                    timerPanel.setBorder(BorderFactory.createTitledBorder("Game Time - YOUR TURN"));
                    timerPanel.setBackground(new Color(230, 255, 230)); // Açık yeşil
                } else {
                    timerPanel.setBorder(BorderFactory.createTitledBorder("Game Time - Opponent's Turn"));
                    timerPanel.setBackground(new Color(255, 230, 230)); // Açık kırmızı
                }
            }
        });
    }

    /**
     * Server tarafından gelen süre bilgisini günceller
     *
     * @param myTime Oyuncunun kalan süresi
     * @param opponentTime Rakibin kalan süresi
     */
    public void updateTimers(String myTime, String opponentTime) {
        SwingUtilities.invokeLater(() -> {
            // Klasik labelları güncelle
            if (role.equalsIgnoreCase("BLACK")) {
                lblBlackTime.setText(myTime);
                lblWhiteTime.setText(opponentTime);
            } else {
                lblWhiteTime.setText(myTime);
                lblBlackTime.setText(opponentTime);
            }

            // Geliştirilmiş zaman gösterimini güncelle
            myTimeLabel.setText(myTime);
            opponentTimeLabel.setText(opponentTime);

            // Zamanı ayrıştır ve kalan süreye göre renk değiştir
            try {
                String[] timeParts = myTime.split(":");
                int minutes = Integer.parseInt(timeParts[0]);

                // Süre az kaldığında uyarı renkleri
                if (minutes < 5) {
                    myTimeLabel.setForeground(Color.RED);
                    // Kritik sürede ses uyarısı verilmediyse ver
                    if (!criticalWarningPlayed && chkSound.isSelected()) {
                        SoundEffects.play(SoundEffects.TIME_CRITICAL);
                        criticalWarningPlayed = true;
                    }
                } else if (minutes < 10) {
                    myTimeLabel.setForeground(new Color(255, 165, 0)); // Turuncu
                    // Uyarı süresinde ses uyarısı verilmediyse ver
                    if (!timeWarningPlayed && chkSound.isSelected()) {
                        SoundEffects.play(SoundEffects.TIME_WARNING);
                        timeWarningPlayed = true;
                    }
                } else {
                    myTimeLabel.setForeground(Color.BLUE);
                    // Süre normale döndüyse uyarı bayraklarını sıfırla
                    timeWarningPlayed = false;
                    criticalWarningPlayed = false;
                }
            } catch (Exception e) {
                // Parse hatası - varsayılan rengi kullan
                myTimeLabel.setForeground(Color.BLUE);
            }

            // Sıradaki oyuncuya göre arka plan rengini güncelle
            if (myTurn) {
                timerPanel.setBorder(BorderFactory.createTitledBorder("Game Time - YOUR TURN"));
                timerPanel.setBackground(new Color(230, 255, 230)); // Açık yeşil
            } else {
                timerPanel.setBorder(BorderFactory.createTitledBorder("Game Time - Opponent's Turn"));
                timerPanel.setBackground(new Color(255, 230, 230)); // Açık kırmızı
            }
        });
    }

    /**
     * Displays a chat message
     *
     * @param payload Message content
     */
    public void showChat(String payload) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(payload + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /**
     * Yeni oyun için sessiz bağlantı yenileme metodunu çağıran özel bir metot.
     * Bu metot handleDisconnect yerine çağrılmalıdır.
     */
    public void handleNewGameReconnect() {
        // Zamanlayıcıları durdur
        blackTimer.stop();
        whiteTimer.stop();

        // Oyun durumunu güncelle
        myTurn = false;
        gameInProgress = false;

        // UI'ı güncelle (bağlantı durumu hariç)
        SwingUtilities.invokeLater(() -> {
            // Tahtayı temizle
            char[][] emptyBoard = new char[19][19];
            for (int y = 0; y < 19; y++) {
                for (int x = 0; x < 19; x++) {
                    emptyBoard[y][x] = '.';
                }
            }
            board.setBoard(emptyBoard);
            board.setLastMove(null);

            // Durum metnini güncelle
            lblStatus.setText("Status: Preparing new game...");

            // Zaman göstergelerini sıfırla
            myTimeLabel.setText("00:00");
            opponentTimeLabel.setText("00:00");
            lblBlackTime.setText("00:00");
            lblWhiteTime.setText("00:00");

            // Bilgi mesajı ekle
            showChat("Setting up a new game...");
        });

        // Direkt olarak bağlantı kur
        try {
            // Yeni bağlantı oluştur
            client = new CClient(host, port, MainFrm.this);
            client.start();

            // Bağlantı durumunu güncelle
            updateConnectionStatus(true);
            showChat("Connected to server.");

            // Kısa bir bekleme sonra eşleştirme isteği gönder
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Yarım saniye bekle
                    SwingUtilities.invokeLater(() -> {
                        // Eşleştirme isteği gönder
                        if (client != null && client.isConnected()) {
                            client.send(new Message(Message.Type.READY_FOR_GAME, ""));
                            showChat("Waiting for game match...");
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            // Bağlantı hatası - daha az dikkat çekici bir bildirim göster
            updateConnectionStatus(false);
            showChat("Failed to connect. Will retry automatically...");

            // Birkaç saniye sonra tekrar dene
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2 saniye bekle
                    SwingUtilities.invokeLater(() -> connectToServer()); // Standart bağlantı metodunu kullan
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * Shows an error message
     *
     * @param message Error message
     */
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            SoundEffects.play("error");
        });
    }

    /**
     * Shows a connection error message
     *
     * @param message Error message
     */
    public void showConnectionError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Connection Error", JOptionPane.ERROR_MESSAGE);
            SoundEffects.play("error");
        });
    }

    /**
     * Shows a custom message dialog
     *
     * @param message Message content
     * @param title Dialog title
     * @param messageType Message type (from JOptionPane constants)
     */
    private void showMessageDialog(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, title, messageType);
        });
    }

    /**
     * SGF yükleme hatalarını standartlaştırılmış biçimde gösterir
     *
     * @param e Oluşan hata
     */
    private void showLoadingError(Exception e) {
        String errorMessage;

        // Hata tipine göre özelleştirilmiş mesajlar
        if (e instanceof IOException) {
            errorMessage = "File access error: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "File IO error: {0}", e.getMessage());
        } else if (e instanceof ParseException) {
            errorMessage = "SGF format error: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "SGF parse error: {0}", e.getMessage());
        } else {
            errorMessage = "Unexpected error: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Unknown error: {0}", e.getMessage());
        }

        // Hata mesajını göster
        showMessageDialog(errorMessage, "Load Error", JOptionPane.ERROR_MESSAGE);

        // Ayrıntılı hata bilgisini logla
        LOGGER.log(Level.SEVERE, "Error stack trace:", e);
    }

    /**
     * Shows the game over dialog
     *
     * @param result Result message
     * @param myScore Player's score
     * @param oppScore Opponent's score
     * @param reason Reason for game end
     */
    /**
     * Oyun bittiğinde gösterilen diyalog – sonucu taraf‑duyarlı hesaplar
     */
    public void showGameOverDialog(String resultIgnored,
            int myScore,
            int oppScore,
            String reason) {
        if (gameOverShown) return;  
        gameOverShown = true; 
        // Zamanlayıcıları durdur, oyun bitti...
        blackTimer.stop();
        whiteTimer.stop();
        gameInProgress = false;
        updateStatusVisuals();

        if (gameRecorder != null) {
            gameRecorder.printMoves();
        }

        /* ---------- SONUÇ METNİNİ HESAPLA ---------- */
        String result;
        String reasonUpper = reason == null ? "" : reason.toUpperCase();
String roleUpper   = role   == null ? "" : role.toUpperCase();

if (reasonUpper.contains("RESIGN")) {
    boolean resignColorKnown =
            reasonUpper.contains("BLACK") || reasonUpper.contains("WHITE");

    boolean iResigned = resignColorKnown
            ? reasonUpper.contains(roleUpper)  // renk belirtilmiş
            : selfResigned;                    // renk yoksa: ben bastım mı?

    result = iResigned
             ? "You lost – you resigned."
             : "You won – opponent resigned.";
} else {
    result = (myScore > oppScore) ? "You won!"
           : (myScore < oppScore) ? "You lost."
           : "It’s a draw.";
}


        SoundEffects.play("game_end");

        /* ---------- DİYALOG ---------- */
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.BLACK);

            JLabel resultLabel = new JLabel(result, JLabel.CENTER);
            resultLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            resultLabel.setForeground(Color.WHITE);

            JLabel scoreLabel = new JLabel(
                    "Score: " + myScore + " (You) : " + oppScore + " (Opponent)",
                    JLabel.CENTER);
            scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            scoreLabel.setForeground(Color.WHITE);

            JLabel reasonLabel = new JLabel(
                    (reason != null && !reason.isEmpty()) ? "Reason: " + reason : "",
                    JLabel.CENTER);
            reasonLabel.setForeground(Color.WHITE);

            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(Color.BLACK);
            top.add(resultLabel, BorderLayout.NORTH);
            top.add(scoreLabel, BorderLayout.CENTER);
            top.add(reasonLabel, BorderLayout.SOUTH);
            panel.add(top, BorderLayout.NORTH);

            /* Butonlar */
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            buttons.setBackground(Color.BLACK);

            JButton bNew = new JButton("New Game");
            JButton bBoard = new JButton("View Final Board");
            JButton bExit = new JButton("Full Exit");

            styleButtonDark(bNew, new Color(50, 150, 50));
            styleButtonDark(bBoard, new Color(50, 90, 180));
            styleButtonDark(bExit, new Color(180, 50, 50));

            bBoard.addActionListener(e
                    -> SwingUtilities.getWindowAncestor((Component) e.getSource()).dispose());

            bNew.addActionListener(e -> {
                newGameInProgress = true;
                SwingUtilities.getWindowAncestor((Component) e.getSource()).dispose();
                if (client != null) {
                    client.close();
                } else {
                    handleNewGameReconnect();
                }
            });

            bExit.addActionListener(e -> System.exit(0));

            buttons.add(bNew);
            buttons.add(bBoard);
            buttons.add(bExit);
            panel.add(buttons, BorderLayout.SOUTH);

            JOptionPane.showOptionDialog(
                    this, panel, "Game Over",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, new Object[]{}, null);
        });
    }

    /**
     * Yeni oyun için sunucuya hazır olduğumuzu bildirir
     */
    private void sendReadyForNewGame() {
        if (client != null && client.isConnected()) {
            client.send(new Message(Message.Type.READY_FOR_GAME, ""));
            showChat("Yeni oyun için eşleştirme bekleniyor...");
        }
    }

    /**
     * Sunucudan gelen tahta verisini işler ve ekranda gösterir. Bu metod ana
     * MainFrm sınıfında olmalıydı ancak silinmiş görünüyor.
     *
     * @param jsonBoard JSON formatındaki tahta verisi
     */
    private void processBoardState(String jsonBoard) {
        if (jsonBoard == null || jsonBoard.isEmpty()) {
            LOGGER.warning("Empty board data received");
            return;
        }

        try {
            // JSON formatını doğrula
            if (!jsonBoard.startsWith("[") || !jsonBoard.endsWith("]")) {
                LOGGER.warning("Invalid board format: " + jsonBoard);
                return;
            }

            // Board paketi üzerinden char[][] dönüşümü yap
            char[][] boardData = parseBoardJsonToCharArray(jsonBoard);

            // Tahta panelini güncelle
            if (boardData != null) {
                SwingUtilities.invokeLater(() -> {
                    board.setBoard(boardData);

                    // Bir hamle yapıldığında ve lastMove belirlendiyse vurgula
                    if (lastMove != null) {
                        board.setLastMove(lastMove);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing board state: " + jsonBoard, e);
            showError("Failed to process board: " + e.getMessage());
        }
    }

    /**
     * JSON formatındaki tahta verisini char dizisine dönüştürür
     *
     * @param jsonBoard JSON formatındaki tahta
     * @return Tahta matrisini temsil eden char dizisi
     */
    private char[][] parseBoardJsonToCharArray(String jsonBoard) {
        try {
            // Dizi boyutunu belirle
            int rows = 0;
            int startIdx = 0;
            while ((startIdx = jsonBoard.indexOf('[', startIdx + 1)) != -1) {
                rows++;
            }

            // Tahtayı çok büyük boyutlu olarak başlatma kontrolü
            if (rows <= 0 || rows > 25) {
                throw new IllegalArgumentException("Invalid board size: " + rows);
            }

            char[][] result = new char[rows][rows];

            // Satır ve sütunları ayrıştır
            String content = jsonBoard.substring(1, jsonBoard.length() - 1).trim();
            String[] rowStrings = content.split("\\],\\[");

            for (int y = 0; y < rowStrings.length; y++) {
                String rowStr = rowStrings[y]
                        .replace("[", "")
                        .replace("]", "")
                        .trim();

                String[] cellValues = rowStr.split(",");
                for (int x = 0; x < cellValues.length; x++) {
                    String value = cellValues[x].trim().replace("\"", "");
                    result[y][x] = value.equals(".") ? '.'
                            : (value.equals("B") ? 'B'
                            : (value.equals("W") ? 'W' : '.'));
                }
            }

            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing board JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Hamleyi kaydedici üzerinde kayıt altına alır. Hamlelerin doğru
     * kaydedilememesi sorunu için yeni yöntem.
     *
     * @param point Hamle noktası
     * @param playerStone Oynayan taş rengi
     * @return Hamlenin başarıyla kaydedilip kaydedilmediği
     */
    private boolean registerMoveInRecorder(Point point, Stone playerStone) {
        if (gameRecorder == null || !gameInProgress) {
            LOGGER.warning("Cannot record move - recorder is null or game not in progress");
            return false;
        }

        try {
            boolean recorded = gameRecorder.recordMove(point, playerStone);
            if (recorded) {
                LOGGER.info("Move recorded successfully: " + playerStone + " at " + point.x() + "," + point.y());
            } else {
                LOGGER.warning("Failed to record move: " + playerStone + " at " + point.x() + "," + point.y());
            }
            return recorded;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording move: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tahtadaki hamleleri doğrudan GameRecorder'a senkronize eder. Kayıt
     * sorunlarını çözmek için kullanılır.
     *
     * @param boardState Güncel tahta durumu
     */
    private void syncBoardWithRecorder(char[][] boardState) {
        if (gameRecorder == null || !gameInProgress) {
            return;
        }

        // Kaydın mevcut durumunu kontrol et
        int currentMoveCount = gameRecorder.getMoveCount();

        // Tahtadaki taşları say
        int blackStones = 0;
        int whiteStones = 0;
        for (char[] row : boardState) {
            for (char cell : row) {
                if (cell == 'B') {
                    blackStones++;
                }
                if (cell == 'W') {
                    whiteStones++;
                }
            }
        }

        int totalStones = blackStones + whiteStones;

        // Eksik hamle varsa uyarı göster
        if (totalStones > currentMoveCount) {
            LOGGER.warning("Move recording mismatch detected: "
                    + totalStones + " stones on board but only "
                    + currentMoveCount + " moves recorded");

            // Tahta durumunu yedekle
            try {
                String backupPath = System.getProperty("user.home")
                        + "/Go_Board_State_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                        + ".txt";

                StringBuilder boardText = new StringBuilder();
                for (char[] row : boardState) {
                    boardText.append(new String(row)).append("\n");
                }

                // Dosyaya yaz
                try (java.io.FileWriter writer = new java.io.FileWriter(backupPath)) {
                    writer.write(boardText.toString());
                }

                LOGGER.info("Board state backed up to: " + backupPath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to backup board state: " + e.getMessage());
            }
        }
    }

    /**
     * Oyuncunun rolünü ayarlar ve oyun durumunu günceller GameRecorder'ı
     * kullanmadan yeniden düzenlenmiş sürüm
     *
     * @param r Oyuncunun rolü (BLACK/WHITE)
     */
    public void setRole(String r) {
        selfResigned  = false;
gameOverShown = false;
        this.role = r;
        this.myTurn = r.equalsIgnoreCase("BLACK");
        final String initialTurn = myTurn ? "Your Turn" : "Waiting for Opponent";

        // Oyun durumunu güncelle
        gameInProgress = true; // Oyunu başlamış olarak işaretle

        // Eğer siyah oyuncuysak, oyun bizden başlıyor
        if (myTurn) {
            // Hamle işaretleyicisi ve UI güncelleme
            updateStatusVisuals();

            // Ses efekti çal
            if (chkSound.isSelected()) {
                SoundEffects.play("game_start");
            }
        }

        // UI'ı güncelle
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Role: " + this.role + " | " + initialTurn);
            setTitle("Go Game - " + this.role + " | " + initialTurn);

            // Siyah taş mı beyaz taş mı kullanacağımızı ayarla
            board.addHoverEffect(r.equalsIgnoreCase("BLACK") ? 'B' : 'W');

            // Pas ve istifa butonlarını etkinleştir
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);
        });

        // Konsola bilgi ver
        System.out.println("Role set to " + r + " - " + initialTurn);
    }

    /**
     * Shows a dialog to replay a loaded game
     *
     * @param gameRecord Game recorder with loaded game
     */
    /**
     * Handles disconnection from the server
     */
    public void handleDisconnect() {
        SwingUtilities.invokeLater(() -> {
            // Stop timers
            blackTimer.stop();
            whiteTimer.stop();

            // Update game state
            myTurn = false;
            gameInProgress = false;
            updateStatusVisuals();

            // Update UI
            showChat("Sunucu ile bağlantı kesildi.");
            updateConnectionStatus(false);

            // Offer to reconnect
            int option = JOptionPane.showConfirmDialog(this,
                    "Sunucu ile bağlantınız kesildi. Yeniden bağlanmak ister misiniz?",
                    "Bağlantı Kesildi",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                connectToServer();
            }
        });
    }

    /**
     * Returns the game board panel
     *
     * @return Game board panel
     */
    public GoBoardPanel getBoard() {
        return board;
    }

    public String getRole() {
        return role;
    }

    /**
     * Main method - starts the application
     *
     * @param args Command line arguments
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

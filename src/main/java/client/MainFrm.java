package client;

import common.Message;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Go oyunu için ana uygulama penceresi. Geliştirilmiş kullanıcı arayüzü, skor
 * ve rol yönetimi, bağlantı güvenilirliği özellikleri içerir.
 *
 * @version 2.0
 */
public class MainFrm extends JFrame {

    //==================================================================
    // CONSTANTS
    //==================================================================
    private static final Logger LOGGER = Logger.getLogger(MainFrm.class.getName());
    private static final long serialVersionUID = 1L;

    // UI Constants - Küçültülmüş boyutlar
    private static final int DEFAULT_WIDTH = 900;  // 1000'den 900'e düşürüldü
    private static final int DEFAULT_HEIGHT = 700; // 750'den 700'e düşürüldü
    private static final Font STATUS_FONT = new Font("SansSerif", Font.BOLD, 12); // 14'ten 12'ye
    private static final Font CHAT_FONT = new Font("SansSerif", Font.PLAIN, 11);  // 13'ten 11'e
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 12); // 14'ten 12'ye
    private static final Font TIMER_FONT = new Font("Monospaced", Font.BOLD, 14); // 16'dan 14'e

    // Game Constants
    private static final int DEFAULT_TIME_MINUTES = 30;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final int RECONNECT_DELAY_BASE = 1000; // ms

    // Theme Colors
    private static final Color DARK_BG = new Color(25, 25, 25);
    private static final Color DARK_SECONDARY = new Color(35, 35, 35);
    private static final Color DARK_BORDER = new Color(60, 60, 60);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color SUCCESS_COLOR = new Color(0, 180, 0);
    private static final Color WARNING_COLOR = new Color(255, 165, 0);
    private static final Color ERROR_COLOR = new Color(220, 0, 0);
    private static final Color ACCENT_COLOR = new Color(0, 120, 215);

    //==================================================================
    // UI COMPONENTS
    //==================================================================
    // Main UI Panels
    private final GoBoardPanel board = new GoBoardPanel();
    private JPanel topPanel;
    private JPanel timerPanel;
    private JPanel scorePanel;
    private JPanel rolePanel;

    // Status and Information
    private JLabel lblStatus;
    private JLabel lblConnectionStatus;
    private JLabel myTimeLabel;
    private JLabel opponentTimeLabel;
    private JLabel myScoreLabel;
    private JLabel oppScoreLabel;
    private JLabel myScoreCaption;
    private JLabel oppScoreCaption;
    private JLabel leadingIndicator;
    private JProgressBar timeProgressBar;
    private final JLabel lblBlackTime = new JLabel("00:00", SwingConstants.CENTER);
    private final JLabel lblWhiteTime = new JLabel("00:00", SwingConstants.CENTER);

    // Controls
    private JButton btnPass;
    private JButton btnResign;
    private JButton btnTutorial;
    private JButton btnNewGameMain;
    private JButton btnSendChat;
    private JCheckBox chkSound;

    // Chat Components
    private JTextField txtChatInput;
    private JTextArea chatArea;

    //==================================================================
    // STATE VARIABLES
    //==================================================================
    // Thread-safe state objects
    private final Object scoreLock = new Object();
    private final Object roleLock = new Object();
    private final AtomicBoolean gameOverDialogShown = new AtomicBoolean(false);

    // Game state
    private int myScore = 0;
    private int oppScore = 0;
    private boolean myTurn = false;
    private String role = "Unknown";
    private boolean gameInProgress = false;
    private boolean gameStarted = false;
    private boolean selfResigned = false;
    private boolean newGameInProgress = false;
    private Point lastMove = null;

    // Connection state
    private final String host;
    private final int port;
    private CClient client;
    private boolean intentionalDisconnect = false;

    // Time management
    private GameTimer blackTimer;
    private GameTimer whiteTimer;
    private boolean timeWarningPlayed = false;
    private boolean criticalWarningPlayed = false;
    private boolean lastTurnState = false;

    // Animation state
    private Timer pulsateTimer;
    private float pulsateAlpha = 0.0f;
    private boolean pulsateDirection = true;

    // Game model and recording
    private final GameGoModel gameModel = new GameGoModel(19);
    private GameRecorder gameRecorder;

    // User preferences
    private final Preferences preferences = Preferences.userNodeForPackage(MainFrm.class);

    // UI Theme 
    private Color BOARD_BG_COLOR = new Color(40, 40, 40);
    private Color PANEL_BG_COLOR = DARK_BG;

    //==================================================================
    // CONSTRUCTOR
    //==================================================================
    /**
     * Ana pencereyi oluşturur
     *
     * @param host Sunucu adresi
     * @param port Sunucu portu
     */
    public MainFrm(String host, int port) {
        super("Go Oyunu");

        // Sunucu bilgilerini kaydet
        this.host = host;
        this.port = port;

        // UI ve bağlantı kurulumu
        setupUIComponents();
        setupListeners();
        initializeTimers();
        connectToServer();

        // Pencere özellikleri - Küçültülmüş boyutlar
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 550)); // 800x600'den 700x550'ye düşürüldü

        // Kullanıcı tercihlerini yükle
        loadPreferences();

        // Log bilgisi
        LOGGER.log(Level.INFO, "MainFrm initialized. Host: {0}, Port: {1}", new Object[]{host, port});
    }

    private void connectToServer() {
        try {
            // Bağlantı durum bilgisini güncelle
            updateConnectionStatus(false);

            // Durum metnini güncelle
            lblStatus.setText("Durum: Sunucuya bağlanıyor...");

            // Eski bağlantıyı kapat (varsa)
            if (client != null) {
                client.close();
            }

            // Yeni istemci oluştur ve bağlan
            client = new CClient(host, port, this);
            client.start();

            // Bağlantı başarılı - UI'ı güncelle
            updateConnectionStatus(true);
            lblStatus.setText("Durum: Sunucuya bağlandı. Oyun bekleniyor...");

            // Sohbet mesajı göster
            showChat("Sunucuya bağlandı: " + host + ":" + port);

            // Oyun için hazır olduğunu bildir
            sendReadyForNewGame();

            LOGGER.log(Level.INFO, "Sunucuya başarıyla bağlandı: {0}:{1}", new Object[]{host, port});

        } catch (Exception e) {
            // Bağlantı hatası
            updateConnectionStatus(false);
            lblStatus.setText("Durum: Bağlantı hatası! Tekrar deneniyor...");

            LOGGER.log(Level.SEVERE, "Sunucuya bağlanırken hata: " + e.getMessage(), e);
            showChat("Bağlantı hatası: " + e.getMessage() + " - Tekrar deneniyor...");

            // 5 saniye sonra otomatik yeniden bağlanma
            Timer reconnectTimer = new Timer(5000, evt -> {
                connectToServer(); // Tekrar bağlanmayı dene
            });
            reconnectTimer.setRepeats(false);
            reconnectTimer.start();
        }
    }

    //==================================================================
    // UI SETUP METHODS
    //==================================================================
    /**
     * UI bileşenlerini oluşturur ve yapılandırır. Geliştirilmiş, daha modern ve
     * erişilebilir bir kullanıcı arayüzü tasarımı içerir.
     */
    private void setupUIComponents() {
        // Ana renkler ve stiller
        setBackground(DARK_BG);
        getContentPane().setBackground(DARK_BG);
        PANEL_BG_COLOR = DARK_BG;
        BOARD_BG_COLOR = new Color(40, 40, 40);
        board.setBackground(BOARD_BG_COLOR);

        // Ana düzen - küçültülmüş boşluklar
        setLayout(new BorderLayout(8, 8)); // 12'den 8'e
        JPanel contentPanel = new JPanel(new BorderLayout(8, 8)); // 12'den 8'e
        contentPanel.setBackground(DARK_BG);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); // 12'den 8'e
        setContentPane(contentPanel);

        //==================== ÜST PANEL ====================//
        topPanel = new JPanel(new BorderLayout(8, 0)); // 12'den 8'e
        topPanel.setBackground(DARK_BG);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0)); // 8'den 6'ya

        //---- Durum etiketi ----//
        lblStatus = new JLabel("Durum: Bağlanıyor...", SwingConstants.LEFT);
        lblStatus.setFont(STATUS_FONT);
        lblStatus.setForeground(TEXT_COLOR);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(DARK_BG);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4)); // 5,10,5,5'ten küçültüldü
        lblStatus.setToolTipText("Oyun durumu ve bağlantı bilgisi");
        lblStatus.getAccessibleContext().setAccessibleDescription("Oyun ve bağlantı durumunu gösteren etiket");

        //---- Rol paneli ----//
        rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1)); // 5,2'den 4,1'e
        rolePanel.setBackground(DARK_BG);
        rolePanel.setVisible(false); // Paneli gizle - mantık devam etsin ama görünürde olmasın
        topPanel.add(rolePanel, BorderLayout.WEST);

        //---- Bağlantı durumu göstergesi ----//
        lblConnectionStatus = new JLabel("⚠️ Bağlı Değil", SwingConstants.RIGHT);
        lblConnectionStatus.setFont(STATUS_FONT);
        lblConnectionStatus.setForeground(ERROR_COLOR);
        lblConnectionStatus.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8)); // 10'dan 8'e
        lblConnectionStatus.setOpaque(true);
        lblConnectionStatus.setBackground(DARK_BG);
        lblConnectionStatus.setToolTipText("Sunucu bağlantı durumu");
        topPanel.add(lblConnectionStatus, BorderLayout.EAST);

        //---- Durum etiketini merkeze ekle ----//
        topPanel.add(lblStatus, BorderLayout.CENTER);

        //==================== SKOR PANELİ ====================//
        createScorePanel(DARK_BG, TEXT_COLOR);
        topPanel.add(scorePanel, BorderLayout.NORTH);

        //==================== ZAMANLAYICI PANELİ ====================//
        setupTimerPanel();
        topPanel.add(timerPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        //==================== OYUN TAHTASI (MERKEZ) ====================//
        setupBoardPanel();

        //==================== SOHBET PANELİ (SAĞ) ====================//
        setupChatPanel();

        //==================== KONTROL PANEL (ALT) ====================//
        setupControlPanel();

        //==================== SON AYARLAR ====================//
        // Hamle yerleştirme ipuçlarını devre dışı bırak (başlangıçta)
        board.enablePlacementHints(false);

        // Tahtada hover efektini etkinleştir
        board.addHoverEffect('B'); // Varsayılan olarak siyah, rol belirlendiğinde güncellenecek

        // Gösterimi güncelle
        updateRoleVisuals();

        // Pencereye odaklanma durumunu yönet (bağlantı durumu için)
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // Pencere odağı kazandığında bağlantı durumunu kontrol et
                if (client != null) {
                    updateConnectionStatus(client.isConnected());
                }
            }
        });

        // Klavye kısayolları ekle
        setupKeyboardShortcuts();
    }

    /**
     * Tahta panelini oluşturur ve yapılandırır - küçültülmüş boyutlar
     */
    private void setupBoardPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(DARK_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6)); // 10'dan 6'ya

        //---- Tahtayı yapılandır - küçültülmüş boyutlar ----//
        board.setPreferredSize(new Dimension(400, 400)); // 500'den 400'e
        board.setMinimumSize(new Dimension(300, 300));   // 400'den 300'e
        board.setToolTipText("Go oyun tahtası - taş yerleştirmek için tıklayın");
        board.getAccessibleContext().setAccessibleDescription("19x19 Go oyun tahtası");

        //---- Tahtayı merkeze yerleştir ----//
        centerPanel.add(board, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Sohbet panelini oluşturur ve yapılandırır - küçültülmüş boyutlar
     */
    private void setupChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(4, 4)); // 5'ten 4'e
        chatPanel.setBackground(DARK_BG);
        chatPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                "Sohbet",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                CHAT_FONT,
                TEXT_COLOR
        ));
        chatPanel.setPreferredSize(new Dimension(250, 0)); // 280'den 250'ye küçültüldü

        //---- Sohbet alanı ----//
        chatArea = new JTextArea(8, 25); // 10,30'dan 8,25'e
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(CHAT_FONT);
        chatArea.setBackground(DARK_SECONDARY);
        chatArea.setForeground(TEXT_COLOR);
        chatArea.setCaretColor(Color.WHITE);
        chatArea.getAccessibleContext().setAccessibleDescription("Sohbet mesajları");

        //---- Sohbet kaydırma paneli ----//
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4)); // 5'ten 4'e
        chatScroll.getViewport().setBackground(DARK_SECONDARY);
        // Otomatik kaydırma için DefaultCaret ayarla
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        //---- Sohbet giriş paneli ----//
        JPanel inputPanel = new JPanel(new BorderLayout(4, 4)); // 5'ten 4'e
        inputPanel.setBackground(DARK_BG);

        //---- Sohbet giriş alanı ----//
        txtChatInput = new JTextField(18); // 20'den 18'e
        txtChatInput.setFont(CHAT_FONT);
        txtChatInput.setBackground(DARK_SECONDARY);
        txtChatInput.setForeground(Color.WHITE);
        txtChatInput.setCaretColor(Color.WHITE);
        txtChatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4) // 5'ten 4'e
        ));
        txtChatInput.setToolTipText("Mesajınızı buraya yazın, göndermek için Enter tuşuna basın");

        //---- Gönder butonu ----//
        btnSendChat = new JButton("Gönder");
        btnSendChat.setFont(BUTTON_FONT);
        styleButtonDark(btnSendChat, new Color(60, 60, 60));

        //---- Giriş paneline bileşenleri ekle ----//
        inputPanel.add(txtChatInput, BorderLayout.CENTER);
        inputPanel.add(btnSendChat, BorderLayout.EAST);

        //---- Sohbet paneline bileşenleri ekle ----//
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.EAST);
    }

    /**
     * Kontrol panelini oluşturur ve yapılandırır - küçültülmüş boyutlar
     */
    private void setupControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBackground(DARK_BG);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8)); // 8,10,10,10'dan küçültüldü

        //---- Oyun kontrol paneli ----//
        JPanel gameControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8)); // 20,10'dan 15,8'e
        gameControlPanel.setBackground(DARK_BG);

        //---- Butonları oluştur ve yapılandır ----//
        btnPass = new JButton("Pas");
        btnResign = new JButton("İstifa");
        btnTutorial = new JButton("Öğretici");
        btnNewGameMain = new JButton("Yeni Oyun");

        btnPass.setFont(BUTTON_FONT);
        btnResign.setFont(BUTTON_FONT);
        btnTutorial.setFont(BUTTON_FONT);
        btnNewGameMain.setFont(BUTTON_FONT);

        //---- Butonlara stil uygula ----//
        styleButtonDark(btnPass, new Color(50, 90, 180));
        styleButtonDark(btnResign, new Color(180, 50, 50));
        styleButtonDark(btnTutorial, new Color(40, 120, 40));
        styleButtonDark(btnNewGameMain, new Color(50, 150, 50));

        //---- Butonlara araç ipuçları ekle ----//
        btnPass.setToolTipText("Sırayı pas geçmek için tıklayın");
        btnResign.setToolTipText("Oyundan çekilmek için tıklayın");
        btnTutorial.setToolTipText("Go oyununun kurallarını öğrenmek için açın");
        btnNewGameMain.setToolTipText("Yeni bir oyun başlatın");

        //---- Erişilebilirlik için kısayol tuşları ekle ----//
        btnPass.setMnemonic(KeyEvent.VK_P);  // Alt+P
        btnResign.setMnemonic(KeyEvent.VK_I); // Alt+I (İstifa için)
        btnTutorial.setMnemonic(KeyEvent.VK_T); // Alt+T
        btnNewGameMain.setMnemonic(KeyEvent.VK_Y); // Alt+Y (Yeni oyun için)

        //---- Butonları başlangıçta devre dışı bırak (bağlantı kurulana kadar) ----//
        btnPass.setEnabled(false);
        btnResign.setEnabled(false);

        //---- Butonları panele ekle ----//
        gameControlPanel.add(btnPass);
        gameControlPanel.add(btnResign);
        gameControlPanel.add(btnNewGameMain);

        //---- Ayarlar paneli ----//
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        settingsPanel.setBackground(DARK_BG);

        //---- Ses kontrolü ----//
        chkSound = new JCheckBox("Ses", true);
        chkSound.setForeground(TEXT_COLOR);
        chkSound.setBackground(DARK_BG);
        chkSound.setFocusPainted(false);
        chkSound.setToolTipText("Oyun seslerini açıp kapatın");

        //---- Ayarlar paneline ekle ----//
        settingsPanel.add(chkSound);
        settingsPanel.add(btnTutorial);

        //---- Kontrol paneline ekle ----//
        controlPanel.add(gameControlPanel, BorderLayout.CENTER);
        controlPanel.add(settingsPanel, BorderLayout.EAST);

        //---- Ana panele kontrol panelini ekle ----//
        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Zamanlayıcı panelini oluşturur ve yapılandırır - küçültülmüş boyutlar
     */
    private void setupTimerPanel() {
        timerPanel = new JPanel(new BorderLayout());
        timerPanel.setBackground(DARK_BG);
        timerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DARK_BORDER, 1),
                "Oyun Süresi",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                STATUS_FONT,
                TEXT_COLOR
        ));

        //---- Zaman etiketleri paneli ----//
        JPanel timeLabelsPanel = new JPanel(new GridLayout(2, 2, 4, 4)); // 5'ten 4'e
        timeLabelsPanel.setBackground(DARK_BG);

        //---- Oyuncu zaman etiketleri ----//
        myTimeLabel = new JLabel("00:00", JLabel.CENTER);
        myTimeLabel.setFont(TIMER_FONT);
        myTimeLabel.setForeground(new Color(60, 180, 255)); // Parlak mavi
        myTimeLabel.setOpaque(true);
        myTimeLabel.setBackground(DARK_BG);
        myTimeLabel.setToolTipText("Kalan süreniz");

        opponentTimeLabel = new JLabel("00:00", JLabel.CENTER);
        opponentTimeLabel.setFont(TIMER_FONT);
        opponentTimeLabel.setForeground(new Color(255, 80, 80)); // Parlak kırmızı
        opponentTimeLabel.setOpaque(true);
        opponentTimeLabel.setBackground(DARK_BG);
        opponentTimeLabel.setToolTipText("Rakibin kalan süresi");

        //---- Zaman başlıkları ----//
        JLabel myTimeCaption = new JLabel("Süreniz:", JLabel.RIGHT);
        myTimeCaption.setForeground(TEXT_COLOR);
        myTimeCaption.setOpaque(true);
        myTimeCaption.setBackground(DARK_BG);

        JLabel oppTimeCaption = new JLabel("Rakibin Süresi:", JLabel.RIGHT);
        oppTimeCaption.setForeground(TEXT_COLOR);
        oppTimeCaption.setOpaque(true);
        oppTimeCaption.setBackground(DARK_BG);

        //---- Zaman etiketlerini panele ekle ----//
        timeLabelsPanel.add(myTimeCaption);
        timeLabelsPanel.add(myTimeLabel);
        timeLabelsPanel.add(oppTimeCaption);
        timeLabelsPanel.add(opponentTimeLabel);
        timerPanel.add(timeLabelsPanel, BorderLayout.CENTER);

        //---- Zaman ilerleme çubuğu ----//
        timeProgressBar = new JProgressBar(0, 100);
        timeProgressBar.setValue(100);
        timeProgressBar.setStringPainted(true);
        timeProgressBar.setString("Kalan Süre");
        timeProgressBar.setForeground(SUCCESS_COLOR);
        timeProgressBar.setBackground(DARK_BG);
        timeProgressBar.setToolTipText("Kalan süre yüzdesi");
        timerPanel.add(timeProgressBar, BorderLayout.SOUTH);
    }

    /**
     * Oyuncunun rolünü ayarlar
     *
     * @param r Rol (BLACK/WHITE)
     */
    public void setRole(String r) {
        synchronized (roleLock) {
            if (r == null || (!r.equalsIgnoreCase("BLACK") && !r.equalsIgnoreCase("WHITE"))) {
                LOGGER.warning("Hata: Geçersiz rol: " + r);
                this.role = "Unknown";
            } else {
                this.role = r.toUpperCase();
            }
            this.myTurn = this.role.equals("BLACK");
            this.myScore = 0; // Başlangıçta sıfır
            this.oppScore = 0; // Başlangıçta sıfır
            this.gameStarted = false; // Oyun henüz başlamadı

            SwingUtilities.invokeLater(() -> {
                String roleDisplay = role.equals("BLACK") ? "Siyah (Siz)" : "Beyaz (Siz)";
                lblStatus.setText("Rol: " + roleDisplay + " | Skor: 0 (Siz) - 0 (Rakip) | "
                        + (myTurn ? "Sıra Sizde" : "Rakibin Sırası"));
                setTitle("Go - " + roleDisplay);

                updateScoreLabelsForRole(); // Etiketleri güncelle
                updateDetailedScoreDisplay(myScore, oppScore); // Skorları sıfır olarak göster
            });
        }
    }

    /**
     * Klavye kısayollarını ayarlar
     */
    private void setupKeyboardShortcuts() {
        // Tüm butonlar için Enter tuşunu aktifleştir
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        am.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ESC tuşuna basıldığında sohbet alanındaki metni temizle
                txtChatInput.setText("");
                txtChatInput.requestFocus();
            }
        });

        // F1 tuşu - Öğretici açma
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "tutorial");
        am.put("tutorial", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnTutorial.isEnabled()) {
                    btnTutorial.doClick();
                }
            }
        });

        // Ctrl+N - Yeni oyun
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newGame");
        am.put("newGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnNewGameMain.isEnabled()) {
                    btnNewGameMain.doClick();
                }
            }
        });
    }

    /**
     * Skor panelini oluşturur ve yapılandırır - küçültülmüş boyutlar
     *
     * @param bgColor Panel arka plan rengi
     * @param textColor Metin rengi
     */
    private void createScorePanel(Color bgColor, Color textColor) {
        // Ana skor paneli - BorderLayout kullanarak daha esnek tasarım
        scorePanel = new JPanel(new BorderLayout(4, 4)); // 5'ten 4'e
        scorePanel.setBackground(bgColor);
        scorePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10) // 8,12,8,12'den küçültüldü
        ));

        // Skor görüntüleme paneli - GridBagLayout ile daha iyi hizalama
        JPanel scoreDisplayPanel = new JPanel(new GridBagLayout());
        scoreDisplayPanel.setBackground(bgColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 4, 0, 4); // 0,5,0,5'ten küçültüldü

        // Skor etiketlerini oluştur - role göre güncellenebilir yapıda
        myScoreCaption = new JLabel("Siz:", JLabel.RIGHT);
        myScoreCaption.setForeground(textColor);
        myScoreCaption.setFont(new Font("SansSerif", Font.BOLD, 12)); // 14'ten 12'ye

        // Skor göstergesi - daha belirgin, yuvarlak köşeli tasarım - küçültülmüş
        myScoreLabel = new JLabel("0", JLabel.CENTER);
        myScoreLabel.setFont(new Font("Monospaced", Font.BOLD, 18)); // 20'den 18'e
        myScoreLabel.setForeground(Color.WHITE);
        myScoreLabel.setBackground(new Color(50, 50, 50)); // Koyu gri
        myScoreLabel.setOpaque(true);
        myScoreLabel.setPreferredSize(new Dimension(50, 30)); // 60,35'ten küçültüldü
        myScoreLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30, 30, 30), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8) // 3,10,3,10'dan küçültüldü
        ));

        // VS etiketi - daha şık stil
        JLabel vsLabel = new JLabel("VS", JLabel.CENTER);
        vsLabel.setForeground(new Color(200, 200, 200));
        vsLabel.setFont(new Font("SansSerif", Font.BOLD, 14)); // 16'dan 14'e

        // Rakip skor göstergesi - küçültülmüş
        oppScoreLabel = new JLabel("0", JLabel.CENTER);
        oppScoreLabel.setFont(new Font("Monospaced", Font.BOLD, 18)); // 20'den 18'e
        oppScoreLabel.setForeground(Color.BLACK);
        oppScoreLabel.setBackground(new Color(220, 220, 220)); // Açık gri
        oppScoreLabel.setOpaque(true);
        oppScoreLabel.setPreferredSize(new Dimension(50, 30)); // 60,35'ten küçültüldü
        oppScoreLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8) // 3,10,3,10'dan küçültüldü
        ));

        oppScoreCaption = new JLabel("Rakip:", JLabel.LEFT);
        oppScoreCaption.setForeground(textColor);
        oppScoreCaption.setFont(new Font("SansSerif", Font.BOLD, 12)); // 14'ten 12'ye

        // GridBagLayout ile bileşenleri yerleştir - daha iyi hizalama için
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        scoreDisplayPanel.add(myScoreCaption, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.2;
        scoreDisplayPanel.add(myScoreLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.1;
        scoreDisplayPanel.add(vsLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.2;
        scoreDisplayPanel.add(oppScoreLabel, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.2;
        scoreDisplayPanel.add(oppScoreCaption, gbc);

        // Lider göstergesi - daha şık ve bilgilendirici
        leadingIndicator = new JLabel("Oyun Başlıyor...", JLabel.CENTER);
        leadingIndicator.setForeground(new Color(180, 180, 180));
        leadingIndicator.setFont(new Font("SansSerif", Font.ITALIC, 11)); // 13'ten 11'e
        leadingIndicator.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0)); // 5'ten 4'e

        // Ana panele ekle
        scorePanel.add(scoreDisplayPanel, BorderLayout.CENTER);
        scorePanel.add(leadingIndicator, BorderLayout.SOUTH);

        // Skorları role göre güncellemek için özel metodu çağır
        updateScoreLabelsForRole();

        // İlk durum güncelleme
        updateDetailedScoreDisplay(0, 0);
    }

    /**
     * Role göre UI görselleştirmesini günceller
     */
    private void updateRoleVisuals() {
        SwingUtilities.invokeLater(() -> {
            // Rol renklendirmesi
            Color roleColor;
            String roleText;

            if ("BLACK".equalsIgnoreCase(role)) {
                roleColor = new Color(50, 50, 50);
                roleText = "Siyah";
            } else if ("WHITE".equalsIgnoreCase(role)) {
                roleColor = new Color(220, 220, 220);
                roleText = "Beyaz";
            } else {
                roleColor = new Color(150, 150, 150);
                roleText = "Bilinmiyor";
            }

            // Rol etiketini güncelle
            JLabel roleLabel = new JLabel("  " + roleText + "  ");
            roleLabel.setOpaque(true);
            roleLabel.setBackground(roleColor);
            roleLabel.setForeground("BLACK".equalsIgnoreCase(role) ? Color.WHITE : Color.BLACK);
            roleLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

            // Panele ekle (mevcut etiketleri kaldır)
            rolePanel.removeAll();
            rolePanel.add(roleLabel);
            rolePanel.revalidate();
            rolePanel.repaint();

            // Skor görsellerini de güncelle
            updateDetailedScoreDisplay(myScore, oppScore);
        });
    }

    /**
     * Skor etiketlerini oyuncunun rolüne göre düzgün şekilde günceller
     */
    private void updateScoreLabelsForRole() {
        SwingUtilities.invokeLater(() -> {
            if (role == null || role.equalsIgnoreCase("Unknown")) {
                LOGGER.warning("Hata: Rol bilinmiyor, etiketler güncellenemiyor.");
                myScoreCaption.setText("Siz:");
                oppScoreCaption.setText("Rakip:");
            } else if (role.equalsIgnoreCase("BLACK")) {
                myScoreCaption.setText("Siyah (Siz):");
                oppScoreCaption.setText("Beyaz (Rakip):");

                // Skor göstergesini Siyah için doğru renkte ayarla
                myScoreLabel.setBackground(new Color(50, 50, 50)); // Koyu gri
                myScoreLabel.setForeground(Color.WHITE);

                // Rakip (Beyaz) için doğru renk
                oppScoreLabel.setBackground(new Color(220, 220, 220)); // Açık gri
                oppScoreLabel.setForeground(Color.BLACK);
            } else if (role.equalsIgnoreCase("WHITE")) {
                myScoreCaption.setText("Beyaz (Siz):");
                oppScoreCaption.setText("Siyah (Rakip):");

                // Skor göstergesini Beyaz için doğru renkte ayarla
                myScoreLabel.setBackground(new Color(220, 220, 220)); // Açık gri
                myScoreLabel.setForeground(Color.BLACK);

                // Rakip (Siyah) için doğru renk
                oppScoreLabel.setBackground(new Color(50, 50, 50)); // Koyu gri
                oppScoreLabel.setForeground(Color.WHITE);
            }
        });
    }

    /**
     * Detaylı skor gösterimini ve öndeki oyuncuyu belirten göstergeyi günceller
     * Komi ve anormal başlangıç puanlarını düzelterek
     *
     * @param myScore Kendi skorunuz
     * @param oppScore Rakibin skoru
     */
    private void updateDetailedScoreDisplay(int myScore, int oppScore) {
        // Anormal skorları kontrol et (361'i engelle)
        if (!gameStarted || myScore > 100 || oppScore > 100) {
            myScore = 0;
            oppScore = 0;
            LOGGER.log(Level.WARNING, "Anormal skorlar sıfırlandı: {0}, {1}", 
                      new Object[]{myScore, oppScore});
        }

        // İlk hamlelerde skoru kontrol et
        if (gameStarted) {
            // Tahta durumunu kontrol et
            int blackCount = 0;
            int whiteCount = 0;
            
            for (int y = 0; y < board.getBoardSize(); y++) {
                for (int x = 0; x < board.getBoardSize(); x++) {
                    char stone = board.getBoard()[y][x];
                    if (stone == 'B') blackCount++;
                    if (stone == 'W') whiteCount++;
                }
            }

            // İlk hamlelerde skoru düzelt
            if (role.equalsIgnoreCase("BLACK")) {
                if (blackCount == 1) myScore = 1;  // Siyahın ilk hamlesi
                if (whiteCount == 1) oppScore = 1;  // Beyazın ilk hamlesi
            } else if (role.equalsIgnoreCase("WHITE")) {
                if (whiteCount == 1) myScore = 1;  // Beyazın ilk hamlesi
                if (blackCount == 1) oppScore = 1;  // Siyahın ilk hamlesi
            }
        }

        // Skor etiketlerini güncelle
        myScoreLabel.setText(String.valueOf(myScore));
        oppScoreLabel.setText(String.valueOf(oppScore));

        // Resign (istifa) kontrolü
        if (myScore == -1) {
            myScoreLabel.setText("İstifa");
            myScoreLabel.setForeground(Color.RED);
        } else if (oppScore == -1) {
            oppScoreLabel.setText("İstifa");
            oppScoreLabel.setForeground(Color.RED);
        }

        // Skor göstergesini güncelle
        if (!gameStarted) {
            if (role.equalsIgnoreCase("WHITE")) {
                leadingIndicator.setText("Oyun sonunda +6.5 komi puanı alacaksınız");
                leadingIndicator.setForeground(new Color(0, 120, 200));
            } else if (role.equalsIgnoreCase("BLACK")) {
                leadingIndicator.setText("Oyun sonunda rakip +6.5 komi puanı alacak");
                leadingIndicator.setForeground(new Color(0, 120, 200));
            } else {
                leadingIndicator.setText("Oyun Başlıyor...");
                leadingIndicator.setForeground(new Color(180, 180, 180));
            }
        } else {
            // Normal oyun sırasında skor göstergesi
            if (myScore > oppScore) {
                leadingIndicator.setText("Siz öndesiniz!");
                leadingIndicator.setForeground(new Color(0, 180, 0));
            } else if (oppScore > myScore) {
                leadingIndicator.setText("Rakip önde!");
                leadingIndicator.setForeground(new Color(220, 0, 0));
            } else {
                leadingIndicator.setText("Berabere!");
                leadingIndicator.setForeground(new Color(180, 180, 0));
            }
        }
    }

    /**
     * Oyunu başlatır ve skor gösterimini düzenler
     */
    public void startGame() {
        this.gameStarted = true;
        this.gameInProgress = true;
        LOGGER.info("Oyun başlatıldı");
        
        // UI'ı güncelle
        SwingUtilities.invokeLater(() -> {
            // Durum göstergesini güncelle
            updateStatusVisuals();
            
            // Başlangıç skorlarını tekrar ayarla
            updateDetailedScoreDisplay(myScore, oppScore);
            
            // Oyunun başladığını bildir
            showChat("Oyun başladı!");
            
            // Komi bilgisini göster
            if (role.equalsIgnoreCase("WHITE")) {
                showChat("Komi avantajı: +6.5 puan alacaksınız (oyun sonunda eklenecek)");
            } else {
                showChat("Rakip (beyaz) +6.5 komi puanı alacak (oyun sonunda eklenecek)");
            }
        });
    }

    /**
     * Skor ve sıra bilgisini günceller
     *
     * @param myScore Benim skorum
     * @param oppScore Rakibin skoru
     * @param whoseTurn Sıra kimde (BLACK/WHITE)
     */
    public void updateStatus(final int myScore, final int oppScore, final String whoseTurn) {
        synchronized (scoreLock) {
            if (whoseTurn == null || role == null) {
                LOGGER.warning("Hata: Rol veya sıra bilgisi eksik.");
                return;
            }

            // Skoru güncelle - anormal değerleri filtrele
            boolean hasAbnormalScore = false;
            int filteredMyScore = myScore;
            int filteredOppScore = oppScore;
            
            if (!gameStarted) {
                // 361 puan veya komi gibi anormal başlangıç değerlerini tespit et
                if (myScore == 361 || oppScore == 361 || 
                    (role.equalsIgnoreCase("WHITE") && myScore == 6) || 
                    (role.equalsIgnoreCase("BLACK") && oppScore == 6) ||
                    myScore > 20 || oppScore > 20) {
                    
                    hasAbnormalScore = true;
                    filteredMyScore = 0;
                    filteredOppScore = 0;
                    LOGGER.log(Level.INFO, "Anormal başlangıç değerleri filtrelendi: {0},{1}", 
                              new Object[]{myScore, oppScore});
                }
            }
            
            // Filtrelenmiş veya normal değerleri kaydet
            this.myScore = hasAbnormalScore ? filteredMyScore : myScore;
            this.oppScore = hasAbnormalScore ? filteredOppScore : oppScore;
            
            // Loglama
            LOGGER.log(Level.FINE, "Durum güncelleniyor: raw={0},{1}, filtered={2},{3}, gameStarted={4}", 
                      new Object[]{myScore, oppScore, this.myScore, this.oppScore, gameStarted});
            
            // Sıra değişimi oluyor mu kontrol et
            boolean turnChanged = (this.myTurn != whoseTurn.equalsIgnoreCase(this.role));
            boolean newTurn = whoseTurn.equalsIgnoreCase(this.role);
            
            this.myTurn = newTurn;

            // UI güncellemelerini EDT'de yap
            SwingUtilities.invokeLater(() -> {
                String roleText = role.equals("BLACK") ? "Siyah" : "Beyaz";
                String turnText = myTurn ? "► SIRANIZ" : "Rakip";
                
                // Daha kısa ve net durum metni
                lblStatus.setText(roleText + " | " + turnText + " | Skor: " + this.myScore + ":" + this.oppScore);
                
                // Pencere başlığını güncelle
                setTitle("Go - " + roleText + " [" + this.myScore + ":" + this.oppScore + "]");

                // Skor gösterimini güncelle
                updateDetailedScoreDisplay(this.myScore, this.oppScore);
                
                // Eğer sıra değiştiyse, animasyon çalıştır
                if (turnChanged) {
                    // Oyun başlamış mı kontrol et
                    if (gameInProgress && (lastTurnState != newTurn)) {
                        animateTurnChange(newTurn);
                    }
                    lastTurnState = newTurn;
                    updateStatusVisuals();
                }
            });
        }
    }

    /**
     * Oyunun başlayıp başlamadığı durumunu ayarlar
     * 
     * @param started Oyun başladıysa true
     */
    public void setGameStarted(boolean started) {
        this.gameStarted = started;
        
        // Oyun başladığında yapılacak ek işlemler
        if (started && !gameInProgress) {
            gameInProgress = true;
            LOGGER.log(Level.INFO, "Oyun başladı olarak işaretlendi: gameStarted={0}, gameInProgress={1}", 
                     new Object[]{gameStarted, gameInProgress});
            
            // UI'ı güncelle
            SwingUtilities.invokeLater(() -> {
                updateStatusVisuals();
                updateDetailedScoreDisplay(myScore, oppScore);
            });
        }
    }

    /**
     * Tahtada taş olup olmadığını kontrol eder
     * Bu metot, GoBoardPanel'den tahtayı alıp kontrol eder
     */
    private boolean tahtadaTasVarMi() {
        char[][] boardData = board.getBoard();
        if (boardData == null) return false;
        
        for (char[] row : boardData) {
            for (char cell : row) {
                if (cell != '.') {
                    return true; // Tahtada en az bir taş var
                }
            }
        }
        return false; // Tahtada hiç taş yok
    }

    /**
     * UI bileşenleri için olay dinleyicilerini ayarlar.
     */
    private void setupListeners() {
        //==================== SOHBET DİNLEYİCİLERİ ====================//

        // Sohbet gönderme düğmesi
        btnSendChat.addActionListener(e -> {
            sendChatMessage();
        });

        // Enter tuşu ile sohbet gönderme
        txtChatInput.addActionListener(e -> {
            sendChatMessage();
        });

        // Sohbet için maksimum karakter sınırı uygula
        txtChatInput.addKeyListener(new KeyAdapter() {
            private final int MAX_CHAT_LENGTH = 100; // Maksimum karakter sayısı

            @Override
            public void keyTyped(KeyEvent e) {
                if (txtChatInput.getText().length() >= MAX_CHAT_LENGTH
                        && e.getKeyChar() != KeyEvent.VK_BACK_SPACE
                        && e.getKeyChar() != KeyEvent.VK_DELETE) {
                    e.consume(); // Maksimum uzunluğa ulaşıldıysa yeni karakteri yoksay

                    // Kullanıcıya görsel/işitsel geribildirim
                    Toolkit.getDefaultToolkit().beep();
                    txtChatInput.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

                    // Sınıra ulaşıldığında geçici uyarı göster
                    Timer resetBorder = new Timer(1000, evt -> {
                        txtChatInput.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
                                BorderFactory.createEmptyBorder(4, 4, 4, 4) // 5'ten 4'e
                        ));
                    });
                    resetBorder.setRepeats(false);
                    resetBorder.start();
                }
            }
        });

        //==================== PAS GEÇME DÜĞMESİ ====================//
        btnPass.addActionListener(e -> {
            if (client == null || !client.isConnected()) {
                showConnectionError("Bağlantı yok - pas geçilemiyor");
                return;
            }

            if (!myTurn) {
                showTurnWarning();
                return;
            }

            // Oyuncu gerçekten pas geçmek istiyor mu onay al
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Pas geçmek istediğinizden emin misiniz? İki oyuncu üst üste pas geçerse oyun bitebilir.",
                    "Pas Geçme Onayı",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                // Pas mesajını gönder
                client.send(new Message(Message.Type.PASS, ""));

                // Pas hamlesi kaydet
                if (gameRecorder != null && gameInProgress) {
                    Stone playerStone = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
                    gameRecorder.recordPass(playerStone);
                    LOGGER.log(Level.INFO, "Pass recorded by {0}", playerStone);
                }

                // Zamanlayıcıyı durdur ve diğerini başlat
                if (role.equalsIgnoreCase("BLACK")) {
                    blackTimer.stop();
                    whiteTimer.start();
                } else {
                    whiteTimer.stop();
                    blackTimer.start();
                }

                // Sıra durumunu güncelle
                myTurn = false;
                updateStatusVisuals();

                // Son hamle vurgusunu temizle
                lastMove = null;
                board.setLastMove(null);

                // Görsel efekt - tahtanın sınırını kısa süreliğine değiştir
                board.setBorder(BorderFactory.createLineBorder(new Color(255, 165, 0), 3)); // Turuncu kenarlık
                Timer resetBorder = new Timer(1500, evt -> {
                    board.setBorder(null);
                });
                resetBorder.setRepeats(false);
                resetBorder.start();

                // Ses çal
                SoundEffects.play("stone_place");

                // İşlem başarılı mesajı
                showChat("Hamleyi pas geçtiniz. Sıra rakibe geçti.");

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Pas geçme işlemi sırasında hata: ", ex);
                showError("Pas geçme işlemi sırasında hata oluştu: " + ex.getMessage());
            }
        });

        //==================== İSTİFA DÜĞMESİ ====================//
        btnResign.addActionListener(e -> {
            if (client == null || !client.isConnected()) {
                showConnectionError("Bağlantı yok - istifa edilemiyor");
                return;
            }

            // Daha dikkat çekici uyarı diyaloğu oluştur
            JPanel warningPanel = new JPanel(new BorderLayout(10, 10));
            warningPanel.setBackground(new Color(50, 0, 0));

            // Uyarı simgesi ve metin
            JLabel warningIcon = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
            warningIcon.setBackground(new Color(50, 0, 0));

            JLabel warningText = new JLabel("<html><b>İstifa etmek istediğinizden emin misiniz?</b><br>"
                    + "Bu oyunu <font color='red'>kaybetmenize</font> neden olacak ve geri alınamaz.</html>");
            warningText.setForeground(Color.WHITE);

            warningPanel.add(warningIcon, BorderLayout.WEST);
            warningPanel.add(warningText, BorderLayout.CENTER);

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    warningPanel,
                    "İstifa Onayı",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                // İstifa bayrağını ayarla
                selfResigned = true;

                // Oyun sonu diyalogu bayraklarını sıfırla
                gameOverDialogShown.set(false);

                // İstemcinin de istifa durumunu bilmesini sağla
                if (client != null) {
                    client.markResigned();
                }

                // Sunucuya rol içeren istifa mesajı gönder
                client.send(new Message(Message.Type.RESIGN, role));

                // Oyun kaydı
                if (gameRecorder != null && gameInProgress) {
                    Stone me = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
                    gameRecorder.recordResign(me);
                    LOGGER.log(Level.INFO, "Resignation recorded by {0}", me);
                }

                // Skoru -1 olarak güncelle (istifa)
                myScore = -1;
                updateDetailedScoreDisplay(myScore, oppScore);

                // Zamanlayıcıları durdur ve oyunu bitir
                blackTimer.stop();
                whiteTimer.stop();
                gameInProgress = false;
                myTurn = false;
                updateStatusVisuals();

                // Görsel feedback - tahtayı kırmızı çerçeve ile vurgula
                board.setBorder(BorderFactory.createLineBorder(Color.RED, 3));

                // Ses efekti
                SoundEffects.play("game_end");

                // Kullanıcıya istifa bilgisi
                showChat("İstifa ettiniz. Oyun sona erdi.");

                // NOT: Oyun sonu diyalogunu burada gösterme, sunucudan gelen mesajı bekle
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "İstifa işlemi sırasında hata: ", ex);
                showError("İstifa işlemi sırasında hata oluştu: " + ex.getMessage());
            }
        });

        //==================== ÖĞRETİCİ DÜĞMESİ ====================//
        btnTutorial.addActionListener(e -> {
            try {
                // Öğretici penceresi aç - arka planda çalışmaya devam et
                TutorialFrame.showTutorial();

                // Kullanıcı için bilgi mesajı
                showChat("Öğretici açıldı. Oyuna her zaman geri dönebilirsiniz.");
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Öğretici açılırken hata: ", ex);
                showError("Öğretici açılamadı: " + ex.getMessage());
            }
        });

        //==================== SES AÇMA/KAPAMA ====================//
        chkSound.addActionListener(e -> {
            boolean soundEnabled = chkSound.isSelected();
            SoundEffects.enableSound(soundEnabled);
            preferences.putBoolean("soundEnabled", soundEnabled);

            // Kullanıcıya bilgi ver
            if (soundEnabled) {
                showChat("Ses efektleri açıldı.");
                // Ses açma efekti - onay için
                SoundEffects.play("stone_place");
            } else {
                showChat("Ses efektleri kapatıldı.");
            }
        });

        //==================== YENİ OYUN DÜĞMESİ ====================//
        btnNewGameMain.addActionListener(e -> {
            // Aktif oyunda mıyız kontrol et
            if (gameInProgress) {
                // Çekici bir uyarı diyaloğu oluştur
                JPanel warningPanel = new JPanel(new BorderLayout(10, 10));
                warningPanel.setBackground(new Color(40, 40, 40));

                JLabel warningText = new JLabel("<html><b>Şu anki oyunu bitirip yeni bir oyun başlatmak istediğinizden emin misiniz?</b><br><br>"
                        + "Mevcut oyun <font color='red'>kaybedilmiş</font> sayılacak ve geri alınamaz.</html>");
                warningText.setForeground(Color.WHITE);

                warningPanel.add(warningText, BorderLayout.CENTER);

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        warningPanel,
                        "Yeni Oyun Onayı",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    startNewGame();
                }
            } else {
                // Oyun aktif değilse, doğrudan yeni oyun başlat
                startNewGame();
            }
        });

        //==================== TAHTA TIKLAMA İŞLEYİCİSİ ====================//
        board.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Bağlantı kontrolü
                if (client == null || !client.isConnected()) {
                    showConnectionError("Bağlantı yok - hamle yapılamıyor");
                    return;
                }

                // Sıra kontrolü
                if (!myTurn) {
                    showTurnWarning();
                    return;
                }

                // Tahta koordinatlarını hesapla
                Point boardCoord = board.getBoardCoordinates(e.getX(), e.getY());

                // Geçerli tıklama kontrolü
                if (boardCoord == null) {
                    // Geçersiz tıklama - tahtanın dışında
                    return;
                }

                try {
                    // Tahtada taş var mı kontrol et
                    char[][] currentBoard = board.getBoard();
                    if (currentBoard[boardCoord.y()][boardCoord.x()] != '.') {
                        // Tıklanan yerde zaten taş var
                        showError("Bu pozisyonda zaten bir taş var!");
                        return;
                    }

                    // İntihar hamlesi kontrolü (kullanılabilirse)
                    if (board.isSuicideMove(boardCoord, role.equalsIgnoreCase("BLACK") ? 'B' : 'W')) {
                        int suicideConfirm = JOptionPane.showConfirmDialog(
                                MainFrm.this,
                                "Bu hamle intihar hamlesi olabilir ve taşınız hemen yakalanacak. Devam etmek istiyor musunuz?",
                                "İntihar Hamlesi Uyarısı",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);

                        if (suicideConfirm != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }

                    // Hamle yapma işlemi
                    placeStone(boardCoord);

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Taş yerleştirme sırasında hata: ", ex);
                    showError("Taş yerleştirme sırasında hata oluştu: " + ex.getMessage());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Tahta üzerine geldiğinde ipucu göster (sadece sıra bendeyse)
                if (myTurn && gameInProgress) {
                    board.enablePlacementHints(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Tahta dışına çıkıldığında ipuçlarını kaldır
                if (!myTurn) {
                    board.enablePlacementHints(false);
                }
            }
        });

        // Fareyi sürükleme işlemleri için dinleyiciler
        board.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Fare hareketi - hover efekti için tahtaya bildir
                if (myTurn && gameInProgress) {
                    Point boardCoord = board.getBoardCoordinates(e.getX(), e.getY());
                    if (boardCoord != null) {
                        board.updateHoverPosition(boardCoord);
                    }
                }
            }
        });

        // Pencere kapanma olayı - kaynakları temizle
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    //==================================================================
    // TIMER METHODS
    //==================================================================
    /**
     * Oyun zamanlayıcılarını başlatır.
     */
    private void initializeTimers() {
        blackTimer = new GameTimer(DEFAULT_TIME_MINUTES, lblBlackTime);
        whiteTimer = new GameTimer(DEFAULT_TIME_MINUTES, lblWhiteTime);

        // Zaman aşımı aksiyonlarını ayarla
        blackTimer.setTimeoutAction(() -> {
            if (!role.equalsIgnoreCase("BLACK")) {
                showMessageDialog("Siyahın süresi doldu! Kazandınız!", "Süre Doldu", JOptionPane.INFORMATION_MESSAGE);
                if (client != null && client.isConnected()) {
                    client.send(new Message(Message.Type.RESIGN, ""));
                }
            }
        });

        whiteTimer.setTimeoutAction(() -> {
            if (!role.equalsIgnoreCase("WHITE")) {
                showMessageDialog("Beyazın süresi doldu! Kazandınız!", "Süre Doldu", JOptionPane.INFORMATION_MESSAGE);
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
     * Zaman ilerleme çubuğunu günceller
     */
    private void updateTimeProgressBar(int remainingSeconds, int totalSeconds) {
        if (timeProgressBar != null) {
            int percentage = (int) ((remainingSeconds * 100.0) / totalSeconds);
            timeProgressBar.setValue(percentage);

            // Kalan süreye göre renk
            if (percentage < 20) {
                timeProgressBar.setForeground(Color.RED);
            } else if (percentage < 40) {
                timeProgressBar.setForeground(new Color(255, 165, 0)); // Turuncu
            } else {
                timeProgressBar.setForeground(new Color(0, 150, 0)); // Yeşil
            }
        }
    }

    /**
     * Bir sohbet mesajı gönderir
     */
    private void sendChatMessage() {
        if (client == null || !client.isConnected()) {
            showConnectionError("Bağlantı yok - mesaj gönderilemiyor");
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
     * Zamanlayıcı güncellemesini işler
     *
     * @param myTime Oyuncunun kalan süresi
     * @param opponentTime Rakibin kalan süresi
     */
    public void updateTimers(String myTime, String opponentTime) {
        // Final değişkenlere ata - thread-safe erişim için
        final String finalMyTime = myTime;
        final String finalOpponentTime = opponentTime;

        SwingUtilities.invokeLater(() -> {
            // Klasik labelları güncelle
            if (role.equalsIgnoreCase("BLACK")) {
                lblBlackTime.setText(finalMyTime);
                lblWhiteTime.setText(finalOpponentTime);
            } else {
                lblWhiteTime.setText(finalMyTime);
                lblBlackTime.setText(finalOpponentTime);
            }

            // Gelişmiş zaman gösterimini güncelle
            myTimeLabel.setText(finalMyTime);
            opponentTimeLabel.setText(finalOpponentTime);

            // Zamanı ayrıştır ve kalan süreye göre renk değiştir
            try {
                String[] timeParts = finalMyTime.split(":");
                int minutes = Integer.parseInt(timeParts[0]);
                int seconds = Integer.parseInt(timeParts[1]);
                int totalSeconds = minutes * 60 + seconds;

                // Süre az kaldığında uyarı renkleri ve efektleri
                if (totalSeconds < 300) { // 5 dakikadan az
                    myTimeLabel.setForeground(Color.RED);
                    myTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 18)); // 20'den 18'e

                    // Kritik sürede ses uyarısı
                    if (!criticalWarningPlayed && chkSound.isSelected() && myTurn) {
                        SoundEffects.play(SoundEffects.TIME_CRITICAL);
                        criticalWarningPlayed = true;
                    }
                } else if (totalSeconds < 600) { // 10 dakikadan az
                    myTimeLabel.setForeground(new Color(255, 165, 0)); // Turuncu
                    myTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 16)); // 18'den 16'ya

                    // Uyarı süresinde ses uyarısı
                    if (!timeWarningPlayed && chkSound.isSelected() && myTurn) {
                        SoundEffects.play(SoundEffects.TIME_WARNING);
                        timeWarningPlayed = true;
                    }
                } else {
                    myTimeLabel.setForeground(new Color(0, 100, 200)); // Mavi
                    myTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 14)); // 16'dan 14'e

                    // Süre normale döndüyse uyarı bayraklarını sıfırla
                    timeWarningPlayed = false;
                    criticalWarningPlayed = false;
                }

                // Zamanı daha görsel bir şekilde göster - ilerlemeli çubuk
                updateTimeProgressBar(totalSeconds, 30 * 60); // 30 dakika max
            } catch (Exception e) {
                // Parse hatası - varsayılan rengi kullan
                myTimeLabel.setForeground(new Color(0, 100, 200));
                LOGGER.log(Level.WARNING, "Error parsing time: {0}", e.getMessage());
            }
        });
    }

    //==================================================================
    // GAME STATE UPDATE METHODS
    //==================================================================
    /**
     * Sıra ve durum göstergelerini koyu tema ile günceller
     */
    private void updateStatusVisuals() {
        SwingUtilities.invokeLater(() -> {
            String turnIndicator = myTurn ? "SIRANIZ" : "Rakibin Sırası";
            String roleDisplay = role.equals("BLACK") ? "Siyah" : "Beyaz";

            // Geliştirilmiş başlık metni 
            setTitle("Go Oyunu - " + roleDisplay + " | " + turnIndicator);

            // Durum etiketini sıraya göre güncelle - rolü gizle ama sırayı belirgin göster
            lblStatus.setOpaque(true);
            lblStatus.setBackground(Color.BLACK);

            if (myTurn) {
                // Sıra bendeyken pulsating efekt ve belirgin gösterim
                lblStatus.setText("▶ SİZİN SIRANIZ ◀");
                lblStatus.setForeground(new Color(0, 255, 0)); // Parlak yeşil
                lblStatus.setFont(new Font(STATUS_FONT.getFamily(), Font.BOLD, STATUS_FONT.getSize() + 1)); // +2'den +1'e
                lblStatus.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 255, 0), 2),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8) // 5,10,5,10'dan küçültüldü
                ));

                // Sıra bendeyken tahtaya yeşil kenarlık ekle
                board.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 0), 2)); // 3'ten 2'ye

                // Hamle yerleştirme ipuçlarını göster
                board.enablePlacementHints(true);

                // Pulsating animasyon efekti
                startPulsatingAnimation();

            } else {
                // Rakibin sırasındayken daha sönük gösterim
                lblStatus.setText("Rakibin Sırası...");
                lblStatus.setForeground(new Color(255, 80, 80)); // Parlak kırmızı
                lblStatus.setFont(STATUS_FONT);
                lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4)); // 5'ten 4'e

                // Pulsating animasyonu durdur
                stopPulsatingAnimation();

                // Tahta kenarlığını kaldır
                board.setBorder(null);

                // Hamle yerleştirme ipuçlarını kapat
                board.enablePlacementHints(false);
            }

            // Düğme durumlarını güncelle
            btnPass.setEnabled(myTurn && gameInProgress);
            btnResign.setEnabled(myTurn && gameInProgress);

            // Yeni Oyun düğmesini oyun durumuna göre güncelle
            if (gameInProgress) {
                btnNewGameMain.setText("Bitir & Yeni Oyun");
                btnNewGameMain.setToolTipText("Mevcut oyunu bitir ve yeni bir oyun başlat");
            } else {
                btnNewGameMain.setText("Yeni Oyun");
                btnNewGameMain.setToolTipText("Yeni bir oyun başlat");
            }

            // Zamanlayıcı panelini koyu tema ile güncelle
            if (timerPanel != null) {
                timerPanel.setOpaque(true);
                timerPanel.setBackground(Color.BLACK);

                TitledBorder border = (TitledBorder) timerPanel.getBorder();

                if (myTurn) {
                    border.setTitle("OYUN SÜRESİ - SIRANIZ");
                    border.setTitleColor(new Color(0, 255, 0));

                    // Oyuncunun zamanını daha belirgin yap
                    myTimeLabel.setOpaque(true);
                    myTimeLabel.setBackground(Color.BLACK);
                    myTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 16)); // 20'den 16'ya
                    myTimeLabel.setForeground(new Color(0, 255, 0)); // Parlak yeşil

                    opponentTimeLabel.setOpaque(true);
                    opponentTimeLabel.setBackground(Color.BLACK);
                    opponentTimeLabel.setForeground(new Color(180, 180, 180));
                } else {
                    border.setTitle("Oyun Süresi - Rakibin Sırası");
                    border.setTitleColor(new Color(255, 80, 80));

                    // Zaman etiketini sıfırla
                    myTimeLabel.setOpaque(true);
                    myTimeLabel.setBackground(Color.BLACK);
                    myTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 14)); // 16'dan 14'e
                    myTimeLabel.setForeground(new Color(180, 180, 180));

                    opponentTimeLabel.setOpaque(true);
                    opponentTimeLabel.setBackground(Color.BLACK);
                    opponentTimeLabel.setForeground(new Color(255, 80, 80));
                }

                timerPanel.repaint(); // Border'i yenile
            }
        });
    }

    /**
     * Sıra göstergesi için pulsating animasyonu başlatır
     */
    private void startPulsatingAnimation() {
        if (pulsateTimer != null && pulsateTimer.isRunning()) {
            return; // Zaten çalışıyor
        }

        pulsateAlpha = 0.3f;
        pulsateDirection = true;

        pulsateTimer = new Timer(50, e -> {
            // Alpha değerini güncelleyerek yanıp sönme efekti oluştur
            if (pulsateDirection) {
                pulsateAlpha += 0.05f;
                if (pulsateAlpha >= 1.0f) {
                    pulsateAlpha = 1.0f;
                    pulsateDirection = false;
                }
            } else {
                pulsateAlpha -= 0.05f;
                if (pulsateAlpha <= 0.3f) {
                    pulsateAlpha = 0.3f;
                    pulsateDirection = true;
                }
            }

            // Sıra göstergesinin rengini güncelle
            lblStatus.setForeground(new Color(0,
                    (int) (255 * pulsateAlpha),
                    0));

            // Tahta kenarlığının rengini güncelle
            if (board.getBorder() != null) {
                board.setBorder(BorderFactory.createLineBorder(
                        new Color(0, (int) (255 * pulsateAlpha), 0), 2)); // 3'ten 2'ye
            }
        });

        pulsateTimer.start();
    }

    /**
     * Pulsating animasyonu durdurur
     */
    private void stopPulsatingAnimation() {
        if (pulsateTimer != null && pulsateTimer.isRunning()) {
            pulsateTimer.stop();
        }
    }

    /**
     * Sıra değişimini animasyonla belirginleştirir
     */
    private void animateTurnChange(boolean myTurn) {
        if (!gameInProgress) {
            return;
        }

        // Sıra değişiminde görsel ve ses efektleri
        if (myTurn) {
            // Eğer benim sıramsa, dikkat çekmek için efektler

            // Ses çal
            if (chkSound.isSelected()) {
                SoundEffects.play("turn_alert");
            }

            // Ekran titreşimi veya yanıp sönme animasyonu
            new Thread(() -> {
                try {
                    for (int i = 0; i < 3; i++) {
                        SwingUtilities.invokeLater(() -> {
                            lblStatus.setBackground(new Color(0, 80, 0));
                            board.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 0), 3)); // 4'ten 3'e
                        });
                        Thread.sleep(150);

                        SwingUtilities.invokeLater(() -> {
                            lblStatus.setBackground(Color.BLACK);
                            board.setBorder(BorderFactory.createLineBorder(new Color(20, 150, 20), 3)); // 4'ten 3'e
                        });
                        Thread.sleep(150);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            // Taş yerleştirme ipuçlarını göster
            board.enablePlacementHints(true);

            // Sıra göstergesi için parlama efekti
            final Color startColor = new Color(0, 255, 0);
            final Color endColor = new Color(0, 120, 0);

            Timer pulseTimer = new Timer(80, null);
            AtomicInteger pulseCount = new AtomicInteger(0);

            pulseTimer.addActionListener(e -> {
                int count = pulseCount.getAndIncrement();
                if (count >= 6) {
                    pulseTimer.stop();
                    lblStatus.setForeground(endColor);
                    return;
                }

                lblStatus.setForeground(count % 2 == 0 ? startColor : endColor);
            });

            pulseTimer.start();

            // Ayrıca tahta kenarını vurgula
            board.highlightBorder(true);
        } else {
            // Rakibin sırası, normal renge dön
            board.highlightBorder(false);
            board.enablePlacementHints(false);
        }
    }

    /**
     * Taş yerleştirme işlemini gerçekleştirir
     *
     * @param boardCoord Taş yerleştirilecek koordinatlar
     */
    private void placeStone(Point boardCoord) {
        // Son hamleyi kaydet ve vurgula
        lastMove = boardCoord;
        board.setLastMove(lastMove);

        // Hamle yapılıyor bayrağını ayarla
        if (client != null) {
            client.setJustMadeMove(true);
        }

        // Hamleyi sunucuya gönder
        client.send(new Message(Message.Type.MOVE, boardCoord.x() + "," + boardCoord.y()));

        // Hamleyi kaydet
        if (gameRecorder != null && gameInProgress) {
            Stone playerStone = role.equalsIgnoreCase("BLACK") ? Stone.BLACK : Stone.WHITE;
            registerMoveInRecorder(boardCoord, playerStone);
        }

        // Sıra durumunu güncelle
        myTurn = false;
        updateStatusVisuals();

        // Zamanlayıcıları değiştir
        if (role.equalsIgnoreCase("BLACK")) {
            blackTimer.stop();
            whiteTimer.start();
        } else {
            whiteTimer.stop();
            blackTimer.start();
        }

        // Görsel geri bildirim - geçici vurgu
        board.animateStonePlace(boardCoord, role.equalsIgnoreCase("BLACK") ? 'B' : 'W');

        // Ses çal
        SoundEffects.play("stone_place");

        // Taş yerleştirme ipuçlarını kapat (sıra bana geçene kadar)
        board.enablePlacementHints(false);
    }

    /**
     * Hamleyi kaydedici üzerinde kayıt altına alır.
     */
    private boolean registerMoveInRecorder(Point point, Stone playerStone) {
        if (gameRecorder == null || !gameInProgress) {
            LOGGER.warning("Cannot record move - recorder is null or game not in progress");
            return false;
        }

        try {
            boolean recorded = gameRecorder.recordMove(point, playerStone);
            if (recorded) {
                LOGGER.log(Level.INFO, "Move recorded successfully: {0} at {1},{2}",
                        new Object[]{playerStone, point.x(), point.y()});
            } else {
                LOGGER.log(Level.WARNING, "Failed to record move: {0} at {1},{2}",
                        new Object[]{playerStone, point.x(), point.y()});
            }
            return recorded;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording move: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Yeni oyun başlatma sürecini yönetir
     */
    private void startNewGame() {
        try {
            // Yeni oyun durumunu başlat
            newGameInProgress = true;

            // Kullanıcıya bilgi ver
            showChat("Yeni oyun hazırlanıyor...");

            // Mevcut bağlantıyı kapat - bu handleNewGameReconnect'i tetikleyecek
            if (client != null) {
                intentionalDisconnect = true;
                client.close();
            } else {
                // Client yoksa direkt olarak yeni bağlantı metodunu çağır
                handleNewGameReconnect();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Yeni oyun başlatılırken hata: ", ex);
            showError("Yeni oyun başlatılamadı: " + ex.getMessage());
            newGameInProgress = false;
        }
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
     * Yeni oyun durumunda olup olmadığını döndürür
     */
    public boolean isNewGameInProgress() {
        return newGameInProgress;
    }

    /**
     * Yeni oyun için sessiz bağlantı yenileme metodunu çağıran özel bir metot.
     */
    public void handleNewGameReconnect() {
        // Zamanlayıcıları durdur
        blackTimer.stop();
        whiteTimer.stop();

        // Oyun durumunu güncelle
        myTurn = false;
        gameInProgress = false;
        gameStarted = false;

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
            lblStatus.setText("Durum: Yeni oyun hazırlanıyor...");

            // Zaman göstergelerini sıfırla
            myTimeLabel.setText("00:00");
            opponentTimeLabel.setText("00:00");
            lblBlackTime.setText("00:00");
            lblWhiteTime.setText("00:00");

            // Bilgi mesajı ekle
            showChat("Yeni oyun ayarlanıyor...");
        });

        // Direkt olarak bağlantı kur
        try {
            // Yeni bağlantı oluştur
            client = new CClient(host, port, MainFrm.this);
            client.start();

            // Bağlantı durumunu güncelle
            updateConnectionStatus(true);
            showChat("Sunucuya bağlandı.");

            // Kısa bir bekleme sonra eşleştirme isteği gönder
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Yarım saniye bekle
                    SwingUtilities.invokeLater(() -> {
                        // Eşleştirme isteği gönder
                        if (client != null && client.isConnected()) {
                            client.send(new Message(Message.Type.READY_FOR_GAME, ""));
                            showChat("Oyun eşleşmesi bekleniyor...");
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            // Bağlantı hatası - daha az dikkat çekici bir bildirim göster
            updateConnectionStatus(false);
            showChat("Bağlanılamadı. Otomatik olarak tekrar denenecek...");

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
     * Sunucu bağlantısı kesildiğinde çağrılır
     */
    public void handleDisconnect() {
        SwingUtilities.invokeLater(() -> {
            // Kasıtlı bağlantı kesme ise, handleNewGameReconnect zaten çağrılacak
            if (intentionalDisconnect) {
                intentionalDisconnect = false;
                return;
            }

            // Zamanlayıcıları durdur
            blackTimer.stop();
            whiteTimer.stop();

            // Oyun durumunu güncelle
            myTurn = false;
            gameInProgress = false;
            updateStatusVisuals();

            // UI'ı güncelle
            showChat("Sunucu ile bağlantı kesildi.");
            updateConnectionStatus(false);

            // Gelişmiş yeniden bağlanma seçenekleri
            Object[] options = {"Yeniden Bağlan", "Uygulamayı Kapat", "Bağlantı Kesildi Olarak Kal"};
            int option = JOptionPane.showOptionDialog(this,
                    "Sunucu ile bağlantınız kesildi. Ne yapmak istersiniz?",
                    "Bağlantı Kesildi",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (option == 0) {
                // Yeniden bağlan - denemeler arası artan bekleme süreleriyle
                connectWithRetry(MAX_RECONNECT_ATTEMPTS);
            } else if (option == 1) {
                // Çık
                System.exit(0);
            }
            // Option 2: Hiçbir şey yapma - kullanıcı manuel olarak yeniden bağlanabilir
        });
    }

    /**
     * Yeniden bağlanma denemesi yapar
     */
    private void connectWithRetry(int maxAttempts) {
        new Thread(() -> {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                final int currentAttempt = attempt;

                SwingUtilities.invokeLater(() -> {
                    showChat("Bağlantı denemesi " + currentAttempt + "/" + maxAttempts + "...");
                    lblStatus.setText("Sunucuya bağlanmaya çalışılıyor (" + currentAttempt + "/" + maxAttempts + ")");
                });

                try {
                    // Her denemede artan bekleme süresi
                    Thread.sleep(RECONNECT_DELAY_BASE * attempt);

                    // Bağlantı kur
                    client = new CClient(host, port, this);
                    client.start();

                    // Bağlantı başarılı
                    SwingUtilities.invokeLater(() -> {
                        showChat("Sunucuya başarıyla bağlandı: " + host + ":" + port);
                        updateConnectionStatus(true);
                        sendReadyForNewGame();
                    });

                    return; // Başarılı bağlantı
                } catch (Exception e) {
                    // Başarısız bağlantı
                    LOGGER.log(Level.WARNING, "Reconnection attempt {0}/{1} failed: {2}",
                            new Object[]{currentAttempt, maxAttempts, e.getMessage()});

                    if (attempt == maxAttempts) {
                        // Son deneme başarısız
                        SwingUtilities.invokeLater(() -> {
                            showChat("Tüm bağlantı denemeleri başarısız oldu: " + e.getMessage());
                            lblStatus.setText("Bağlantı başarısız. Lütfen tekrar deneyin.");

                            // Kullanıcıya daha fazla deneme yapıp yapmak istemediğini sor
                            int response = JOptionPane.showConfirmDialog(
                                    this,
                                    "Tüm bağlantı denemeleri başarısız oldu. Tekrar denemek ister misiniz?",
                                    "Bağlantı Başarısız",
                                    JOptionPane.YES_NO_OPTION);

                            if (response == JOptionPane.YES_OPTION) {
                                connectWithRetry(maxAttempts); // Tekrar dene
                            }
                        });
                    }
                }
            }
        }).start();
    }

    /**
     * Bağlantı durumu göstergesini günceller
     */
    private void updateConnectionStatus(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                lblConnectionStatus.setText("✓ Bağlı");
                lblConnectionStatus.setForeground(new Color(0, 180, 0));

                // Parlama animasyonu yap
                Timer fadeTimer = new Timer(30, null);
                final int[] alpha = {255};

                fadeTimer.addActionListener(e -> {
                    alpha[0] -= 5;
                    if (alpha[0] <= 100) {
                        alpha[0] = 100;
                        fadeTimer.stop();
                    }
                    lblConnectionStatus.setForeground(new Color(0, 180, 0, alpha[0]));
                });

                fadeTimer.start();

            } else {
                lblConnectionStatus.setText("⚠️ Bağlı Değil");
                lblConnectionStatus.setForeground(new Color(200, 0, 0));

                // Yanıp sönme animasyonu yap
                Timer blinkTimer = new Timer(500, null);
                final boolean[] visible = {true};

                blinkTimer.addActionListener(e -> {
                    visible[0] = !visible[0];
                    lblConnectionStatus.setVisible(visible[0]);
                });

                // 3 saniye yanıp sönsün sonra sabit kalsın
                Timer stopTimer = new Timer(3000, e -> {
                    blinkTimer.stop();
                    lblConnectionStatus.setVisible(true);
                });

                blinkTimer.start();
                stopTimer.setRepeats(false);
                stopTimer.start();
            }
        });
    }

    /**
     * Sohbet mesajını gösterir
     */
    public void showChat(String payload) {
        SwingUtilities.invokeLater(() -> {
            // Zaman damgası ekle
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());

            chatArea.append("[" + timestamp + "] " + payload + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /**
     * "Sıranız değil" uyarısını gösterir
     */
    private void showTurnWarning() {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(this,
                "Şu anda sıra sizde değil!",
                "Uyarı",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Hata mesajı gösterir
     */
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Hata", JOptionPane.ERROR_MESSAGE);
            SoundEffects.play("error");
        });
    }

    /**
     * Bağlantı hatası mesajı gösterir
     */
    public void showConnectionError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
            SoundEffects.play("error");
        });
    }

    /**
     * Özel mesaj diyaloğu gösterir
     */
    private void showMessageDialog(String message, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, title, messageType);
        });
    }

    /**
     * SGF yükleme hatalarını standartlaştırılmış biçimde gösterir
     */
    private void showLoadingError(Exception e) {
        String errorMessage;

        // Hata tipine göre özelleştirilmiş mesajlar
        if (e instanceof IOException) {
            errorMessage = "Dosya erişim hatası: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "File IO error: {0}", e.getMessage());
        } else if (e instanceof ParseException) {
            errorMessage = "SGF format hatası: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "SGF parse error: {0}", e.getMessage());
        } else {
            errorMessage = "Beklenmeyen hata: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Unknown error: {0}", e.getMessage());
        }

        // Hata mesajını göster
        showMessageDialog(errorMessage, "Yükleme Hatası", JOptionPane.ERROR_MESSAGE);

        // Ayrıntılı hata bilgisini logla
        LOGGER.log(Level.SEVERE, "Error stack trace:", e);
    }

    /**
     * Oyun sonu diyaloğunu gösterir - küçültülmüş boyutlar
     */
    public void showGameOverDialog(String resultIgnored, int myScore, int oppScore, String reason) {
        // Aynı diyaloğun birden fazla kez gösterilmesini önle
        if (!gameOverDialogShown.compareAndSet(false, true)) {
            LOGGER.info("Game over dialog already shown, ignoring request");
            return;
        }

        // Zamanlayıcıları durdur
        blackTimer.stop();
        whiteTimer.stop();
        gameInProgress = false;
        updateStatusVisuals();

        // Kaydedici bilgisini logla (varsa)
        if (gameRecorder != null) {
            gameRecorder.printMoves();
        }

        /* ---------- SONUÇ METNİNİ HESAPLA ---------- */
        String result;
        String reasonUpper = reason == null ? "" : reason.toUpperCase();
        String roleUpper = role == null ? "" : role.toUpperCase();

        if (reasonUpper.contains("RESIGN") || reasonUpper.contains("PES")) {
            boolean resignColorKnown = reasonUpper.contains("BLACK") || reasonUpper.contains("WHITE")
                    || reasonUpper.contains("SIYAH") || reasonUpper.contains("BEYAZ");

            boolean iResigned = resignColorKnown
                    ? (reasonUpper.contains(roleUpper)
                    || (roleUpper.equals("BLACK") && reasonUpper.contains("SIYAH"))
                    || (roleUpper.equals("WHITE") && reasonUpper.contains("BEYAZ")))
                    : selfResigned; // Renk yoksa: ben bastım mı?

            result = iResigned
                    ? "Kaybettiniz - istifa ettiniz."
                    : "Kazandınız - rakip istifa etti.";
        } else {
            result = (myScore > oppScore) ? "Kazandınız!"
                    : (myScore < oppScore) ? "Kaybettiniz."
                    : "Berabere.";
        }

        // Ses efekti
        SoundEffects.play(result.contains("Kazandınız") ? "victory" : "game_end");

        /* ---------- DİYALOG - küçültülmüş boyutlar ---------- */
        SwingUtilities.invokeLater(() -> {
            // Daha gelişmiş ve görsel bir diyalog
            JPanel panel = new JPanel(new BorderLayout(8, 8)); // 10'dan 8'e
            panel.setBackground(new Color(30, 30, 30)); // Koyu arka plan

            // Sonuç başlığı - küçültülmüş font
            JLabel resultLabel = new JLabel(result, JLabel.CENTER);
            resultLabel.setFont(new Font("SansSerif", Font.BOLD, 20)); // 24'ten 20'ye
            resultLabel.setForeground(result.contains("Kazandınız")
                    ? new Color(0, 255, 0)
                    : // Parlak yeşil
                    result.contains("Kaybettiniz")
                    ? new Color(255, 0, 0)
                    : // Kırmızı
                    new Color(220, 220, 0)); // Sarı (berabere)

            // Skor gösterimi - küçültülmüş
            JPanel scorePanel = new JPanel(new GridLayout(3, 2, 8, 4)); // 10,5'ten 8,4'e
            scorePanel.setBackground(new Color(40, 40, 40));
            scorePanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16)); // 10,20'den küçültüldü

            // Skor etiketleri - küçültülmüş fontlar
            JLabel myScoreTitle = new JLabel("Sizin Skorunuz:", JLabel.RIGHT);
            myScoreTitle.setForeground(Color.WHITE);
            JLabel myScoreValue = new JLabel(String.valueOf(myScore), JLabel.LEFT);
            myScoreValue.setForeground(new Color(0, 255, 0));
            myScoreValue.setFont(new Font("Monospaced", Font.BOLD, 16)); // 18'den 16'ya

            JLabel oppScoreTitle = new JLabel("Rakip Skoru:", JLabel.RIGHT);
            oppScoreTitle.setForeground(Color.WHITE);
            JLabel oppScoreValue = new JLabel(String.valueOf(oppScore), JLabel.LEFT);
            oppScoreValue.setForeground(new Color(255, 100, 100));
            oppScoreValue.setFont(new Font("Monospaced", Font.BOLD, 16)); // 18'den 16'ya

            JLabel reasonTitle = new JLabel("Neden:", JLabel.RIGHT);
            reasonTitle.setForeground(Color.WHITE);
            JLabel reasonValue = new JLabel((reason != null && !reason.isEmpty()) ? reason : "Oyun bitti", JLabel.LEFT);
            reasonValue.setForeground(Color.YELLOW);

            // Panele ekle
            scorePanel.add(myScoreTitle);
            scorePanel.add(myScoreValue);
            scorePanel.add(oppScoreTitle);
            scorePanel.add(oppScoreValue);
            scorePanel.add(reasonTitle);
            scorePanel.add(reasonValue);

            // Ana panele ekle
            panel.add(resultLabel, BorderLayout.NORTH);
            panel.add(scorePanel, BorderLayout.CENTER);

            // Butonlar - küçültülmüş
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8)); // 15,10'dan 12,8'e
            buttons.setBackground(new Color(40, 40, 40));

            // Daha modern butonlar - küçültülmüş
            JButton bNew = createStyledButton("Yeni Oyun", new Color(0, 120, 0));
            JButton bBoard = createStyledButton("Son Durumu Görüntüle", new Color(0, 80, 150));
            JButton bExit = createStyledButton("Çıkış", new Color(150, 0, 0));

            // Buton aksiyonları
            bBoard.addActionListener(e
                    -> SwingUtilities.getWindowAncestor((Component) e.getSource()).dispose()
            );

            bNew.addActionListener(e -> {
                newGameInProgress = true;
                SwingUtilities.getWindowAncestor((Component) e.getSource()).dispose();
                if (client != null) {
                    intentionalDisconnect = true;
                    client.close();
                } else {
                    handleNewGameReconnect();
                }
            });

            bExit.addActionListener(e -> System.exit(0));

            // Butonları ekle
            buttons.add(bNew);
            buttons.add(bBoard);
            buttons.add(bExit);
            panel.add(buttons, BorderLayout.SOUTH);

            // Özel diyalog göster - küçültülmüş boyut
            JOptionPane optionPane = new JOptionPane(
                    panel,
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    null,
                    new Object[]{}, // Buton yok - kendi butonlarımızı kullanıyoruz
                    null
            );

            JDialog dialog = optionPane.createDialog(this, "Oyun Bitti");
            dialog.setSize(400, 300); // 450,350'den küçültüldü
            dialog.setVisible(true);
        });
    }

    /**
     * Oyun yapılandırmasını günceller
     */
    public void updateGameConfig(int boardSize, int handicap, double komi) {
        LOGGER.log(Level.INFO, "Updating game config: size={0}, handicap={1}, komi={2}",
                new Object[]{boardSize, handicap, komi});

        // Tahta boyutu değiştiyse tahtayı yeniden oluştur
        if (board.getBoardSize() != boardSize) {
            board.setBoardSize(boardSize);

            // Yeni boş tahta oluştur
            char[][] emptyBoard = new char[boardSize][boardSize];
            for (int y = 0; y < boardSize; y++) {
                for (int x = 0; x < boardSize; x++) {
                    emptyBoard[y][x] = '.';
                }
            }
            board.setBoard(emptyBoard);
        }

        // Handikap taşlarını UI'a göster veya işle
        if (handicap > 0) {
            showChat("Handikap: " + handicap + " taş");
        }

        // Komi değerini göster
        showChat("Komi: " + komi);

        // Pencere boyutunu tahta boyutuna göre ayarla
        adjustWindowSize(boardSize);
    }

    /**
     * Pencere boyutunu tahta boyutuna göre ayarlar - küçültülmüş boyutlar
     */
    private void adjustWindowSize(int boardSize) {
        int width, height;

        // Tahta boyutuna göre pencere boyutunu belirle - tüm boyutlar küçültüldü
        switch (boardSize) {
            case 9:
                width = 700; // 800'den 700'e
                height = 550; // 600'dan 550'ye
                break;
            case 13:
                width = 800; // 900'den 800'e
                height = 650; // 700'den 650'ye
                break;
            case 19:
            default:
                width = DEFAULT_WIDTH; // Zaten küçültülmüş: 900
                height = DEFAULT_HEIGHT; // Zaten küçültülmüş: 700
                break;
        }

        setSize(width, height);
        setLocationRelativeTo(null); // Ekranda ortala
    }

    /**
     * Verilen rengin daha açık bir tonunu döndürür
     */
    private Color lighten(Color color, float amount) {
        int r = Math.min(255, (int) (color.getRed() * (1 + amount)));
        int g = Math.min(255, (int) (color.getGreen() * (1 + amount)));
        int b = Math.min(255, (int) (color.getBlue() * (1 + amount)));
        return new Color(r, g, b);
    }

    /**
     * Stil uygulanmış buton oluşturur - küçültülmüş boyutlar
     */
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12)); // 14'ten 12'ye
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 35)); // 180,40'dan küçültüldü
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover efekti
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(lighten(color, 0.2f));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    /**
     * Belirtilen renkle bir düğmeyi stillendirir - küçültülmüş boyutlar
     */
    private void styleButtonDark(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(100, 30)); // 120,35'ten küçültüldü
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover efektleri ekle
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(lighten(color, 0.2f));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(color);
            }
        });
    }

    /**
     * Kullanıcı tercihlerini yükler
     */
    private void loadPreferences() {
        boolean soundEnabled = preferences.getBoolean("soundEnabled", true);
        chkSound.setSelected(soundEnabled);
        SoundEffects.enableSound(soundEnabled);
    }

    /**
     * Kullanıcı tercihlerini kaydeder
     */
    private void savePreferences() {
        preferences.putBoolean("soundEnabled", chkSound.isSelected());
    }

    /**
     * Uygulama kapatılırken temizlik yapar
     */
    public void cleanup() {
        // Zamanlayıcıları durdur
        if (blackTimer != null) {
            blackTimer.stop();
        }
        if (whiteTimer != null) {
            whiteTimer.stop();
        }

        // Bağlantıyı kapat
        if (client != null) {
            client.close();
        }

        // Tercihleri kaydet
        savePreferences();

        LOGGER.info("Uygulama kaynakları temizlendi.");
    }

    /**
     * Tahtayı döndürür
     */
    public GoBoardPanel getBoard() {
        return board;
    }

    /**
     * Oyuncunun rolünü döndürür
     */
    public String getRole() {
        return role;
    }

    /**
     * Oyun sonu diyaloğunun gösterilip gösterilmediğini döndürür
     */
    public boolean isGameOverShown() {
        return gameOverDialogShown.get();
    }

    /**
     * Ana metot - uygulamayı başlatır - küçültülmüş pencere boyutu
     */
    public static void main(String[] args) {
        try {
            // Sistem görünümünü ayarla
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Sistem teması yüklenemedi: " + e.getMessage(), e);
        }

        // Host ve port bilgisini argümanlardan al veya varsayılanları kullan
        String host = args.length > 0 ? args[0] : "51.20.252.52";
        int port = 5000;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                LOGGER.warning("Geçersiz port numarası: " + args[1] + ". Varsayılan port (5000) kullanılıyor.");
            }
        }

        final String finalHost = host;
        final int finalPort = port;
        SwingUtilities.invokeLater(() -> {
            MainFrm mainFrame = new MainFrm(finalHost, finalPort);
            mainFrame.setVisible(true);

            // Uygulama kapanırken temizlik için shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                mainFrame.cleanup();
            }));
        });
    }
}
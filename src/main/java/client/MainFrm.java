package client;

import common.Message;
import javax.swing.*;
import java.awt.*;
import client.ui.GoBoardPanel;
import game.go.model.Point;

/**
 * Go oyunu için ana uygulama penceresi
 */
public class MainFrm extends JFrame {

    // Görsel bileşenler
    private final GoBoardPanel board = new GoBoardPanel();
    private final JTextField txtChatInput = new JTextField(20);
    private final JTextArea chatArea = new JTextArea(10, 30);
    private final JButton btnSendChat = new JButton("Gönder");
    private final JButton btnPass = new JButton("Pas");
    private final JButton btnResign = new JButton("Pes Et");
    private final JLabel lblStatus = new JLabel("Durum: Bağlanılıyor...", SwingConstants.LEFT);
    
    // Skor ve durum bilgileri
    private boolean myTurn = false;
    private String role = "Bilinmiyor";
    private CClient client;
    private final String host;
    private final int port;
    private Point lastMove = null;
    
    // Sabit değerler
    private static final int DEFAULT_WIDTH = 900;
    private static final int DEFAULT_HEIGHT = 700;
    private static final Color BOARD_BG_COLOR = new Color(219, 176, 102);
    private static final Color PANEL_BG_COLOR = new Color(240, 230, 210);
    private static final Font STATUS_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font CHAT_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    public MainFrm(String host, int port) {
        super("Go Oyunu");
        this.host = host;
        this.port = port;

        setupUIComponents();
        setupListeners();
        connectToServer();
        
        // Pencere özellikleri
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 500));
    }

    /**
     * UI bileşenlerini hazırlar ve düzenler
     */
    private void setupUIComponents() {
        // Ana renk ve stil ayarları
        setBackground(PANEL_BG_COLOR);
        board.setBackground(BOARD_BG_COLOR);
        
        // Ana layout
        setLayout(new BorderLayout(10, 10));
        
        // Durum çubuğu (üst)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(PANEL_BG_COLOR);
        lblStatus.setFont(STATUS_FONT);
        topPanel.add(lblStatus);
        add(topPanel, BorderLayout.NORTH);
        
        // Oyun tahtası (merkez)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(PANEL_BG_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.add(board, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        
        // Sohbet paneli (sağ)
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(PANEL_BG_COLOR);
        chatPanel.setBorder(BorderFactory.createTitledBorder("Sohbet"));
        
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
        
        // Kontrol düğmeleri (alt)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        controlPanel.setBackground(PANEL_BG_COLOR);
        
        btnPass.setFont(BUTTON_FONT);
        btnResign.setFont(BUTTON_FONT);
        
        styleButton(btnPass, new Color(80, 120, 220));
        styleButton(btnResign, new Color(220, 80, 80));
        
        // Oyun kuralları yardım butonu ekleme
        JButton btnRules = new JButton("Kurallar");
        styleButton(btnRules, new Color(50, 150, 50));
        btnRules.addActionListener(e -> showRules());
        
        controlPanel.add(btnPass);
        controlPanel.add(btnResign);
        controlPanel.add(btnRules);
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Go oyunu kurallarını gösteren dialog
     */
    private void showRules() {
        JTextArea rulesText = new JTextArea();
        rulesText.setEditable(false);
        rulesText.setLineWrap(true);
        rulesText.setWrapStyleWord(true);
        rulesText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        rulesText.setText(
            "Go Oyunu Temel Kuralları:\n\n" +
            "1. Oyun sırayla taş koyarak oynanır. Siyah başlar.\n\n" +
            "2. Bir taş yerleştirildikten sonra tahtadan kaldırılamaz, ancak esir alınabilir.\n\n" +
            "3. Esir alma: Bir taş veya taş grubunun tüm özgürlükleri (yanındaki boş noktalar) rakip taşlarla çevrilirse, o taşlar esir alınır ve tahtadan çıkarılır.\n\n" +
            "4. 'Ko' kuralı: Bir oyuncu bir önceki hamleden hemen önceki tahta durumunu oluşturamaz.\n\n" +
            "5. 'Suicide' kuralı: Bir oyuncu, kendi taşını hiç özgürlüğü olmayan bir noktaya koyamaz, ancak bu hamle rakip taşların esir alınmasını sağlıyorsa yapılabilir.\n\n" +
            "6. İki oyuncu arka arkaya pas geçerse oyun biter.\n\n" +
            "7. Skor hesaplaması: Kontrol edilen bölge + esir alınan taş sayısı."
        );
        
        JScrollPane scrollPane = new JScrollPane(rulesText);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Go Oyunu Kuralları", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Düğmeleri stilize eder
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
     * Olay dinleyicilerini tanımlar
     */
    private void setupListeners() {
        // Sohbet gönderme
        btnSendChat.addActionListener(e -> sendChatMessage());
        txtChatInput.addActionListener(e -> sendChatMessage());
        
        // Pas geçme
        btnPass.addActionListener(e -> {
            if (client != null) {
                if (!myTurn) {
                    showTurnWarning();
                    return;
                }
                client.send(new Message(Message.Type.PASS, ""));
                myTurn = false;
                updateStatusVisuals();
                
                // Son hamle göstergesini temizle
                lastMove = null;
                board.setLastMove(null);
            }
        });
        
        // Pes etme
        btnResign.addActionListener(e -> {
            if (client != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Pes etmek istediğinizden emin misiniz?", 
                        "Pes Etme Onayı",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    client.send(new Message(Message.Type.RESIGN, ""));
                    myTurn = false;
                    updateStatusVisuals();
                }
            }
        });
        
        // Tahtaya tıklama (hamle yapma)
        board.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (client == null || !myTurn) {
                    if (client != null && !myTurn) {
                        showTurnWarning();
                    }
                    return;
                }
                
                // Düzeltilmiş koordinat dönüşümü
                int boardSize = board.getBoardSize();
                int cellSize = board.getCellSize();
                int offset = cellSize; // Tahta kenarı için offset
                
                // Tıklama konumunu tahta koordinatlarına dönüştür
                // Math.round yerine daha doğru bir hesaplama
                int boardX = (e.getX() - offset + cellSize/2) / cellSize;
                int boardY = (e.getY() - offset + cellSize/2) / cellSize;
                
                // Sınırları kontrol et
                if (boardX >= 0 && boardX < boardSize && boardY >= 0 && boardY < boardSize) {
                    // Son hamleyi kaydet ve görsel olarak işaretle
                    lastMove = new Point(boardX, boardY);
                    board.setLastMove(lastMove);
                    
                    client.send(new Message(Message.Type.MOVE, boardX + "," + boardY));
                    myTurn = false;
                    updateStatusVisuals();
                    System.out.println("Hamle gönderildi: " + boardX + "," + boardY);
                }
            }
        });
    }
    
    /**
     * Sunucuya bağlanır
     */
    private void connectToServer() {
        try {
            client = new CClient(host, port, this);
            client.start();
            showChat("Sunucuya bağlanılıyor: " + host + ":" + port);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                    "Sunucuya bağlanılamadı:\n" + ex.getMessage(), 
                    "Bağlantı Hatası", 
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    /**
     * Sıra hatası uyarısı gösterir
     */
    private void showTurnWarning() {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(this, 
                "Sıra sizde değil!", 
                "Uyarı", 
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Sohbet mesajını gönderir
     */
    private void sendChatMessage() {
        if (client == null) return;
        
        String msg = txtChatInput.getText().trim();
        if (!msg.isEmpty()) {
            client.send(new Message(Message.Type.TO_CLIENT, msg));
            txtChatInput.setText("");
            txtChatInput.requestFocus();
        }
    }

    /**
     * Sıra ve durum göstergelerini günceller
     */
    private void updateStatusVisuals() {
        SwingUtilities.invokeLater(() -> {
            String turnIndicator = myTurn ? "Sıra Sizde" : "Rakip Bekleniyor";
            lblStatus.setText("Rol: " + this.role + " | " + turnIndicator);
            setTitle("Go Oyunu - " + this.role + " | " + turnIndicator);
            
            // Sıra göstergesi için renk değiştirme
            lblStatus.setForeground(myTurn ? new Color(0, 120, 0) : new Color(120, 0, 0));
            
            // Butonların etkinlik durumunu güncelle
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);
        });
    }

    /**
     * Sunucudan gelen skor ve sıra bilgisiyle durumu günceller
     */
    public void updateStatus(int me, int opp, String whoseTurn) {
        final String turnIndicator;
        
        if (this.role == null || this.role.equals("Bilinmiyor") || 
            this.role.equals("Connecting...") || whoseTurn == null || whoseTurn.isEmpty()) {
            turnIndicator = "";
            this.myTurn = false;
        } else {
            this.myTurn = whoseTurn.equalsIgnoreCase(this.role);
            turnIndicator = myTurn ? "Sıra Sizde" : "Rakip Bekleniyor";
        }

        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Rol: " + this.role + " | Skor: " + me + " (Siz) : " + opp + " (Rakip) | " + turnIndicator);
            setTitle("Go Oyunu - " + this.role + " [" + me + ":" + opp + "] " + turnIndicator);
            lblStatus.setForeground(myTurn ? new Color(0, 120, 0) : new Color(120, 0, 0));
            
            // Butonların etkinlik durumunu güncelle
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);
        });
    }

    /**
     * Sunucudan gelen rol bilgisini ayarlar
     */
    public void setRole(String r) {
        this.role = r;
        this.myTurn = r.equalsIgnoreCase("BLACK");
        final String initialTurn = myTurn ? "Sıra Sizde" : "Rakip Bekleniyor";

        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Rol: " + this.role + " | Skor: 0:0 | " + initialTurn);
            setTitle("Go Oyunu - " + this.role + " | " + initialTurn);
            lblStatus.setForeground(myTurn ? new Color(0, 120, 0) : new Color(120, 0, 0));
            
            // Butonların etkinlik durumunu güncelle
            btnPass.setEnabled(myTurn);
            btnResign.setEnabled(myTurn);
            
            // Rol atandığında kullanıcıya bildir
            showChat("Rolünüz: " + this.role + (myTurn ? " (Oyuna siz başlıyorsunuz)" : " (Rakip başlıyor)"));
        });
    }

    /**
     * Gelen sohbet mesajını ekranda gösterir
     */
    public void showChat(String payload) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(payload + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /**
     * Hata mesajını gösterir
     */
    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Hata", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    /**
     * Bağlantı hatasını gösterir
     */
    public void showConnectionError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Bağlantı Hatası", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    /**
     * Oyun bittiğinde diyalog gösterir
     */
    public void showGameOverDialog(String result, int myScore, int oppScore, String reason) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, 
                result + "\n\nSkor: " + myScore + " (Siz) : " + oppScore + " (Rakip)" + 
                (reason != null && !reason.isEmpty() ? "\n\nNeden: " + reason : ""), 
                "Oyun Bitti", 
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    /**
     * Bağlantı kesildiğinde UI durumunu günceller
     */
    public void handleDisconnect() {
        SwingUtilities.invokeLater(() -> {
            myTurn = false;
            updateStatusVisuals();
            showChat("Sunucu ile bağlantı kesildi.");
        });
    }
    
    /**
     * Go tahtası referansını döndürür
     */
    public GoBoardPanel getBoard() {
        return board;
    }

    /**
     * Ana metod - Uygulamayı başlatır
     */
    public static void main(String[] args) {
        try {
            // Modern look & feel ayarla
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Argümanlardan host ve port al, yoksa varsayılan kullan
        String host = args.length > 0 ? args[0] : "localhost";
        int port = 6000;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Geçersiz port numarası: " + args[1] + ". Varsayılan port (6000) kullanılıyor.");
            }
        }

        final String finalHost = host;
        final int finalPort = port;
        SwingUtilities.invokeLater(() -> {
            new MainFrm(finalHost, finalPort).setVisible(true);
        });
    }
}
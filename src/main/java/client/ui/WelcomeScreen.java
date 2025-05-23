// WelcomeScreen.java - Başlangıç ekranı iyileştirmesi

package client.ui;

import client.MainFrm;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Go oyunu başlangıç ekranı.
 * Kullanıcı dostu bir arayüz ve Go oyunu hakkında temel bilgiler sunar.
 */
public class WelcomeScreen extends JFrame {
    
    // UI Bileşenleri
    private final JTextField txtServerAddress;
    private final JTextField txtPort;
    private final JTextField txtPlayerName;
    private final JButton btnConnect;
    private final JButton btnHelp;
    private final JComboBox<String> cmbBoardSize;
    
    // Arka plan görseli
    private Image backgroundImage;
    
    /**
     * Yeni bir başlangıç ekranı oluşturur.
     */
    public WelcomeScreen() {
        super("Go Game - Hoş Geldiniz");
        
        // Arka plan resmini yüklemeye çalış
        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/images/go_background.jpg"));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Background image could not be loaded: " + e.getMessage());
        }
        
        // Ana panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Arka plan resmini çiz
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                    // Metinleri okunabilir kılmak için yarı saydam bir arkaplan ekleyelim
                    g.setColor(new Color(0, 0, 0, 180));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    // Arka plan resmi yoksa gradyan arkaplan kullan
                    Graphics2D g2d = (Graphics2D) g;
                    GradientPaint gp = new GradientPaint(0, 0, new Color(40, 40, 40), 
                                                       0, getHeight(), new Color(20, 20, 20));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        
        // Başlık paneli
        JPanel titlePanel = createTransparentPanel();
        JLabel lblTitle = new JLabel("GO OYUNU", JLabel.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 36));
        lblTitle.setForeground(Color.WHITE);
        
        JLabel lblSubtitle = new JLabel("Stratejik Zeka Oyunu", JLabel.CENTER);
        lblSubtitle.setFont(new Font("SansSerif", Font.ITALIC, 18));
        lblSubtitle.setForeground(new Color(200, 200, 200));
        
        titlePanel.setLayout(new BorderLayout(5, 10));
        titlePanel.add(lblTitle, BorderLayout.CENTER);
        titlePanel.add(lblSubtitle, BorderLayout.SOUTH);
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Giriş paneli
        JPanel loginPanel = createTransparentPanel();
        loginPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        
        // Sunucu adres alanı
        JLabel lblServer = new JLabel("Sunucu Adresi:");
        lblServer.setForeground(Color.WHITE);
        lblServer.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtServerAddress = new JTextField("localhost", 20);
        styleTextField(txtServerAddress);
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 1;
        loginPanel.add(lblServer, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(txtServerAddress, gbc);
        
        // Port alanı
        JLabel lblPort = new JLabel("Port:");
        lblPort.setForeground(Color.WHITE);
        lblPort.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtPort = new JTextField("5000", 6);
        styleTextField(txtPort);
        
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 1;
        loginPanel.add(lblPort, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.gridwidth = 2;
        loginPanel.add(txtPort, gbc);
        
        // Oyuncu adı alanı
        JLabel lblPlayerName = new JLabel("Oyuncu Adı:");
        lblPlayerName.setForeground(Color.WHITE);
        lblPlayerName.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtPlayerName = new JTextField("", 20);
        styleTextField(txtPlayerName);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 1;
        loginPanel.add(lblPlayerName, gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.gridwidth = 2;
        loginPanel.add(txtPlayerName, gbc);
        
        // Tahta boyutu alanı
        JLabel lblBoardSize = new JLabel("Tahta Boyutu:");
        lblBoardSize.setForeground(Color.WHITE);
        lblBoardSize.setFont(new Font("SansSerif", Font.BOLD, 14));
        cmbBoardSize = new JComboBox<>(new String[]{"19x19 (Standart)", "13x13 (Orta)", "9x9 (Küçük)"});
        styleComboBox(cmbBoardSize);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 1;
        loginPanel.add(lblBoardSize, gbc);
        
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginPanel.add(cmbBoardSize, gbc);
        
        // Bağlan butonu
        btnConnect = new JButton("Oyuna Başla");
        styleButton(btnConnect, new Color(0, 120, 0));
        
        gbc.gridx = 1; gbc.gridy = 4;
        gbc.gridwidth = 1;
        loginPanel.add(btnConnect, gbc);
        
        // Yardım butonu
        btnHelp = new JButton("Yardım & Kurallar");
        styleButton(btnHelp, new Color(70, 70, 170));
        
        gbc.gridx = 2; gbc.gridy = 4;
        gbc.gridwidth = 1;
        loginPanel.add(btnHelp, gbc);
        
        // Giriş panelini ekle
        mainPanel.add(loginPanel, BorderLayout.CENTER);
        
        // Oyun bilgi paneli
        JPanel infoPanel = createTransparentPanel();
        infoPanel.setLayout(new BorderLayout(10, 10));
        
        JTextArea txtInfo = new JTextArea(
                "Go, dünyanın en eski ve en stratejik oyunlarından biridir. Bu oyunda amacınız rakibinizden " +
                "daha fazla bölgeyi kontrol etmektir. Taşlarınızı tahtaya yerleştirerek bölgeler oluşturun ve " +
                "rakibinizin taşlarını kuşatarak ele geçirin.\n\n" +
                "Başlamak için sunucu bilgilerini girin ve 'Oyuna Başla' butonuna tıklayın. Daha fazla bilgi " +
                "için 'Yardım & Kurallar' bölümüne bakın."
        );
        txtInfo.setEditable(false);
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        txtInfo.setOpaque(false);
        txtInfo.setForeground(new Color(200, 200, 200));
        txtInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        infoPanel.add(txtInfo, BorderLayout.CENTER);
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        
        // Buton olayları
        btnConnect.addActionListener(e -> connectToServer());
        btnHelp.addActionListener(e -> showHelp());
        
        // Enter tuşuna basıldığında bağlan
        txtServerAddress.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    connectToServer();
                }
            }
        });
        
        txtPort.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    connectToServer();
                }
            }
        });
        
        // Pencere özellikleri
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(400, 500));
        
        // Pencere kapanırken yapılacaklar
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
    
    /**
     * Yeni bir yarı saydam panel oluşturur.
     * 
     * @return Oluşturulan panel
     */
    private JPanel createTransparentPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setOpaque(false);
        return panel;
    }
    
    /**
     * Metin alanlarına ortak stil uygular.
     * 
     * @param textField Stillendirilecek metin alanı
     */
    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textField.setBackground(new Color(40, 40, 40));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
    }
    
    /**
     * Açılır liste kutusuna stil uygular.
     * 
     * @param comboBox Stillendirilecek açılır liste
     */
    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        comboBox.setBackground(new Color(40, 40, 40));
        comboBox.setForeground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        
        // Açılır liste görünümünü özelleştir
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                        int index, boolean isSelected,
                                                        boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? new Color(60, 60, 120) : new Color(40, 40, 40));
                setForeground(Color.WHITE);
                return this;
            }
        });
    }
    
    /**
     * Butonlara stil uygular.
     * 
     * @param button Stillendirilecek buton
     * @param color Buton arka plan rengi
     */
    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker()),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Buton üzerine gelindiğinde renk değiştir
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(color.darker());
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(color);
            }
        });
    }
    
    /**
     * Sunucuya bağlanır ve oyunu başlatır.
     */
    private void connectToServer() {
        String server = txtServerAddress.getText().trim();
        String portText = txtPort.getText().trim();
        String playerName = txtPlayerName.getText().trim();
        
        // Giriş alanları kontrolü
        if (server.isEmpty()) {
            showError("Lütfen sunucu adresini girin.");
            return;
        }
        
        if (portText.isEmpty()) {
            showError("Lütfen port numarasını girin.");
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                showError("Port numarası 1-65535 arasında olmalıdır.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Geçerli bir port numarası girin.");
            return;
        }
        
        if (playerName.isEmpty()) {
            playerName = "Oyuncu_" + (int)(Math.random() * 1000);
            txtPlayerName.setText(playerName);
        }
        
        // Tahta boyutunu al
        int boardSize = 19; // Varsayılan
        String boardSizeSelection = (String) cmbBoardSize.getSelectedItem();
        if (boardSizeSelection != null) {
            if (boardSizeSelection.startsWith("13")) {
                boardSize = 13;
            } else if (boardSizeSelection.startsWith("9")) {
                boardSize = 9;
            }
        }
        
        // Bağlanmaya çalışıyor mesajı
        btnConnect.setEnabled(false);
        btnConnect.setText("Bağlanıyor...");
        
        // Arka planda sunucuya bağlan
        final int finalBoardSize = boardSize;
        final String finalPlayerName = playerName;
        
        new Thread(() -> {
            try {
                // Oyun penceresini oluştur ve göster
                MainFrm mainFrame = new MainFrm(server, port);
                mainFrame.setVisible(true);
                
                // Bağlantı başarılıysa başlangıç ekranını kapat
                SwingUtilities.invokeLater(() -> dispose());
                
            } catch (Exception e) {
                // Bağlantı hatası durumunda
                SwingUtilities.invokeLater(() -> {
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Oyuna Başla");
                    showError("Sunucuya bağlanırken hata oluştu: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Yardım penceresini gösterir.
     */
    private void showHelp() {
        TutorialFrame.showTutorial();
    }
    
    /**
     * Hata mesajı gösterir.
     * 
     * @param message Hata mesajı
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Hata", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Ana metod - uygulamayı başlatır.
     * 
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        try {
            // Sistem görünümünü kullan
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            WelcomeScreen welcomeScreen = new WelcomeScreen();
            welcomeScreen.setVisible(true);
        });
    }
}
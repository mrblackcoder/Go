package client.ui;

import game.go.model.Stone;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;


public class GameOverDialog extends JDialog {

    // Ana bileşenler
    private final JLabel lblResult;
    private final JLabel lblScore;
    private final JLabel lblReason;
    private final JTextArea txtStats;
    private final JButton btnNewGame;
    private final JButton btnViewBoard;
    private final JButton btnExit;
    private JProgressBar blackScoreBar;
    private JProgressBar whiteScoreBar;

    // Animasyon için
    private final List<FallingStone> fallingStones = new ArrayList<>();
    private final Timer animationTimer;
    private final JPanel animationPanel;
    private float sparkleOpacity = 0.0f;
    private final Timer sparkleTimer;

    // Arka plan görseli ve efektler
    private BufferedImage backgroundImage;
    private BufferedImage boardTexture;
    private BufferedImage stonePattern;
    private final Color RESULT_WIN_COLOR = new Color(76, 175, 80);
    private final Color RESULT_LOSE_COLOR = new Color(244, 67, 54);
    private final Color RESULT_DRAW_COLOR = new Color(33, 150, 243);

    /**
     * Oyun sonu diyaloğunu oluşturur.
     *
     * @param owner Üst pencere
     * @param result Sonuç metni
     * @param myScore Oyuncunun puanı
     * @param oppScore Rakibin puanı
     * @param reason Oyunun bitme nedeni
     * @param gameStats Oyun istatistikleri
     * @param role Oyuncunun rolü (BLACK/WHITE)
     */
    public GameOverDialog(Frame owner, String result, int myScore, int oppScore,
            String reason, String gameStats, String role) {
        super(owner, "Game Over", true);

        // Görselleri yüklemeye çalış
        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/images/go_end_background.jpg"));
            boardTexture = ImageIO.read(getClass().getResourceAsStream("/images/wood_texture.jpg"));
            stonePattern = ImageIO.read(getClass().getResourceAsStream("/images/stone_pattern.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Some images could not be loaded: " + e.getMessage());
        }

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (backgroundImage != null) {
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                    
                    // Yarı saydam siyah katman
                    g2d.setColor(new Color(0, 0, 0, 200));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    Color topColor, bottomColor;
                    
                    if (result.contains("won")) {
                        topColor = new Color(0, 60, 0);
                        bottomColor = new Color(0, 30, 0);
                    } else if (result.contains("lost")) {
                        topColor = new Color(60, 0, 0);
                        bottomColor = new Color(30, 0, 0); 
                    } else {
                        topColor = new Color(0, 0, 60);
                        bottomColor = new Color(0, 0, 30);
                    }
                    
                    GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                
                if (boardTexture != null) {
                    // Yarı saydam tahta deseni
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                    for (int i = 0; i < getWidth(); i += 100) {
                        for (int j = 0; j < getHeight(); j += 100) {
                            g2d.drawImage(boardTexture, i, j, 100, 100, null);
                        }
                    }
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                
                // Üstte ve altta dekoratif çizgiler
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawLine(50, 20, getWidth() - 50, 20);
                g2d.drawLine(50, getHeight() - 20, getWidth() - 50, getHeight() - 20);
                
                // Go deseni
                if (stonePattern != null) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
                    g2d.drawImage(stonePattern, getWidth() - 220, getHeight() - 220, 200, 200, null);
                    g2d.drawImage(stonePattern, 20, 20, 150, 150, null);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                
                // Işık efekti - kazanma/kaybetme durumuna göre
                float glowOpacity = sparkleOpacity * 0.4f;
                Color glowColor;
                
                if (result.contains("won")) {
                    glowColor = new Color(0, 255, 0, (int)(glowOpacity * 255));
                } else if (result.contains("lost")) {
                    glowColor = new Color(255, 0, 0, (int)(glowOpacity * 255));
                } else {
                    glowColor = new Color(0, 0, 255, (int)(glowOpacity * 255));
                }
                
                g2d.setColor(glowColor);
                g2d.fillOval(-100, -100, getWidth() + 200, getHeight() / 2);
            }
        };
        
        mainPanel.setLayout(new BorderLayout(30, 30));
        mainPanel.setBorder(new EmptyBorder(40, 50, 40, 50));

        JPanel titlePanel = createModernPanel();
        titlePanel.setLayout(new BorderLayout(15, 15));
        
        lblResult = new JLabel(result, JLabel.CENTER);
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 48));
        
        if (result.contains("won")) {
            lblResult.setForeground(RESULT_WIN_COLOR);
        } else if (result.contains("lost")) {
            lblResult.setForeground(RESULT_LOSE_COLOR);
        } else {
            lblResult.setForeground(RESULT_DRAW_COLOR);
        }
        
        // Işık efekti oluştur
        lblResult.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Modern HTML skor gösterimi
        String scorePattern = "<html><div style='text-align: center; font-family: \"Segoe UI\"'>" +
                "<span style='font-size: 16pt; color: white'>SCORE</span><br>" +
                "<span style='font-size: 20pt; font-weight: bold; color: white'>%d (You) - %d (Opponent)</span>" +
                "</div></html>";
        
        lblScore = new JLabel(String.format(scorePattern, myScore, oppScore), JLabel.CENTER);
        
        // Skor göstergeleri (çubuklar)
        JPanel scoreBarPanel = new JPanel(new BorderLayout(5, 10));
        scoreBarPanel.setOpaque(false);
        
        // Toplam skor
        int totalScore = Math.max(1, myScore + oppScore);
        
        // Siyah skor çubuğu
        blackScoreBar = new JProgressBar(0, totalScore);
        blackScoreBar.setValue(myScore);
        blackScoreBar.setStringPainted(true);
        blackScoreBar.setString("You: " + myScore);
        blackScoreBar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        blackScoreBar.setForeground(new Color(50, 50, 50));
        blackScoreBar.setBackground(new Color(20, 20, 20, 100));
        blackScoreBar.setBorder(BorderFactory.createEmptyBorder());
        
        // Beyaz skor çubuğu
        whiteScoreBar = new JProgressBar(0, totalScore);
        whiteScoreBar.setValue(oppScore);
        whiteScoreBar.setStringPainted(true);
        whiteScoreBar.setString("Opponent: " + oppScore);
        whiteScoreBar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        whiteScoreBar.setForeground(new Color(220, 220, 220));
        whiteScoreBar.setBackground(new Color(200, 200, 200, 100));
        whiteScoreBar.setBorder(BorderFactory.createEmptyBorder());
        
        scoreBarPanel.add(blackScoreBar, BorderLayout.NORTH);
        scoreBarPanel.add(whiteScoreBar, BorderLayout.SOUTH);
        
        // Sebep etiketi modernize edilmiş
        String formattedReason = reason;
        if (reason != null && !reason.isEmpty()) {
            if (reason.contains("RESIGN")) {
                if (result.contains("lost")) {
                    formattedReason = "You resigned from the game";
                } else {
                    formattedReason = "Opponent resigned from the game";
                }
            } else if ("SCORE".equals(reason)) {
                formattedReason = "Game ended by scoring";
            }
        }
        
        lblReason = new JLabel(formattedReason, JLabel.CENTER);
        lblReason.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        lblReason.setForeground(new Color(200, 200, 200));
        
        // Bileşenleri başlık paneline ekle
        JPanel topInfoPanel = new JPanel(new BorderLayout(10, 5));
        topInfoPanel.setOpaque(false);
        topInfoPanel.add(lblResult, BorderLayout.NORTH);
        topInfoPanel.add(lblScore, BorderLayout.CENTER);
        
        titlePanel.add(topInfoPanel, BorderLayout.NORTH);
        titlePanel.add(scoreBarPanel, BorderLayout.CENTER);
        titlePanel.add(lblReason, BorderLayout.SOUTH);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Orta panel - istatistikler ve animasyon
        JPanel centerPanel = createModernPanel();
        centerPanel.setLayout(new BorderLayout(15, 15));
        
        // İstatistikler
        txtStats = new JTextArea(gameStats);
        txtStats.setEditable(false);
        txtStats.setLineWrap(true);
        txtStats.setWrapStyleWord(true);
        txtStats.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtStats.setForeground(Color.WHITE);
        txtStats.setOpaque(false);
        txtStats.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Scroll panel modernize edilmiş
        JScrollPane scrollPane = new JScrollPane(txtStats);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180, 100), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        
        // Scroll çubuklarını özelleştir
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Animasyon paneli - geliştirilmiş
        animationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawFallingStones((Graphics2D) g);
            }
        };
        animationPanel.setOpaque(false);
        animationPanel.setPreferredSize(new Dimension(120, 0));
        
        centerPanel.add(animationPanel, BorderLayout.EAST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Alt panel - modernize edilmiş butonlar
        JPanel buttonPanel = createModernPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 15));
        
        // Yeni oyun butonu
        btnNewGame = new JButton("New Game");
        styleButtonModern(btnNewGame, new Color(76, 175, 80)); // Material Green
        
        // Tahtayı görüntüle butonu
        btnViewBoard = new JButton("View Board");
        styleButtonModern(btnViewBoard, new Color(33, 150, 243)); // Material Blue
        
        // Çıkış butonu
        btnExit = new JButton("Exit Game");
        styleButtonModern(btnExit, new Color(244, 67, 54)); // Material Red
        
        buttonPanel.add(btnNewGame);
        buttonPanel.add(btnViewBoard);
        buttonPanel.add(btnExit);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Animasyon için düşen taşlar oluştur
        createFallingStones(role);
        
        // Animasyon zamanlayıcısı
        animationTimer = new Timer(50, e -> {
            updateFallingStones();
            animationPanel.repaint();
        });
        
        // Işık efekti için zamanlayıcı
        sparkleTimer = new Timer(50, e -> {
            // Işık efekti dalgalanması
            double pulse = Math.sin(System.currentTimeMillis() / 1000.0);
            sparkleOpacity = 0.3f + (float)Math.abs(pulse) * 0.2f;
            mainPanel.repaint();
        });
        
        // Animasyonları başlat
        animationTimer.start();
        sparkleTimer.start();
        
        // Zamanla sönümleyen skor çubukları animasyonu
        new Thread(() -> {
            try {
                // Önce skor çubuklarını sıfırla
                SwingUtilities.invokeLater(() -> {
                    blackScoreBar.setValue(0);
                    whiteScoreBar.setValue(0);
                });
                
                Thread.sleep(500);
                
                // Skoru animasyonlu şekilde doldur
                int steps = 20;
                for (int i = 1; i <= steps; i++) {
                    final int blackValue = myScore * i / steps;
                    final int whiteValue = oppScore * i / steps;
                    
                    SwingUtilities.invokeLater(() -> {
                        blackScoreBar.setValue(blackValue);
                        blackScoreBar.setString("You: " + blackValue);
                        whiteScoreBar.setValue(whiteValue);
                        whiteScoreBar.setString("Opponent: " + whiteValue);
                    });
                    
                    Thread.sleep(1500 / steps);
                }
                
                // Son değerleri ayarla
                SwingUtilities.invokeLater(() -> {
                    blackScoreBar.setValue(myScore);
                    blackScoreBar.setString("You: " + myScore);
                    whiteScoreBar.setValue(oppScore);
                    whiteScoreBar.setString("Opponent: " + oppScore);
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Pencere özellikleri
        setContentPane(mainPanel);
        setSize(800, 650);
        setLocationRelativeTo(owner);
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Pencere kapanırken animasyonu durdur
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                animationTimer.stop();
                sparkleTimer.stop();
            }
        });
    }

    /**
     * Yeni bir modern, yarı saydam panel oluşturur.
     *
     * @return Oluşturulan panel
     */
    private JPanel createModernPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Yarı-saydam arkaplan
                g2d.setColor(new Color(0, 0, 0, 120));
                
                // Yuvarlatılmış köşeler
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // İnce kenarlık
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 15, 15);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    /**
     * Butonlara modern material stil uygular.
     *
     * @param button Stillendirilecek buton
     * @param color Buton arka plan rengi
     */
    private void styleButtonModern(JButton button, Color color) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 45));
        
        // Özel çizim için
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                AbstractButton b = (AbstractButton) c;
                ButtonModel model = b.getModel();
                
                // Buton rengi - basılı, üzerinde ve normal durumlar
                Color bgColor = color;
                if (model.isPressed()) {
                    bgColor = color.darker();
                } else if (model.isRollover()) {
                    bgColor = color.brighter();
                }
                
                // Gölge efekti
                if (!model.isPressed()) {
                    g2d.setColor(new Color(0, 0, 0, 50));
                    g2d.fillRoundRect(5, 5, c.getWidth() - 8, c.getHeight() - 8, 10, 10);
                }
                
                // Arka plan
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, c.getWidth() - 3, c.getHeight() - 3, 10, 10);
                
                // Hafif parlaklık efekti
                if (!model.isPressed()) {
                    GradientPaint gp = new GradientPaint(
                        0, 0, new Color(255, 255, 255, 100),
                        0, c.getHeight() / 2, new Color(255, 255, 255, 0)
                    );
                    g2d.setPaint(gp);
                    g2d.fillRoundRect(0, 0, c.getWidth() - 3, c.getHeight() / 2, 10, 10);
                }
                
                // Metin çizimi
                FontMetrics fm = g2d.getFontMetrics();
                Rectangle textRect = new Rectangle(0, 0, c.getWidth(), c.getHeight());
                String text = SwingUtilities.layoutCompoundLabel(
                    fm, b.getText(), null, b.getVerticalAlignment(),
                    b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                    b.getHorizontalTextPosition(), textRect, new Rectangle(), new Rectangle(),
                    0
                );
                
                g2d.setColor(b.getForeground());
                g2d.setFont(b.getFont());
                
                int textX = (c.getWidth() - fm.stringWidth(text)) / 2;
                int textY = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                
                // Metin gölgesi
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.drawString(text, textX + 1, textY + 1);
                
                // Metin
                g2d.setColor(b.getForeground());
                g2d.drawString(text, textX, textY);
                
                g2d.dispose();
            }
        });
        
        // Hover ve basış efektleri
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Hover ses efekti
                if (e.getSource() == btnNewGame) {
                    playSound("hover_sound");
                }
                button.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                // Klik ses efekti
                playSound("click_sound");
                button.repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                button.repaint();
            }
        });
    }
    
    /**
     * Ses çalmak için yardımcı metot - çalma mekanizması başka bir sınıfta uygulayalacak
     */
    private void playSound(String soundName) {
        // İleriki uygulama için placeholder - SoundEffects sınıfına bağlantı
        // SoundEffects.play(soundName);
    }

    /**
     * Düşen taşlar için animasyon oluşturur.
     */
    private void createFallingStones(String role) {
        boolean playerIsBlack = "BLACK".equalsIgnoreCase(role);
        
        // 30 tane rastgele taş oluştur (eskisinden 10 tane daha fazla)
        for (int i = 0; i < 30; i++) {
            // Rastgele taş rengi - sonuca göre dağılım değişir
            Stone stoneType;
            
            // Kazanan taşların oranını artır
            double winningStoneChance = 0.7;
            
            if (Math.random() < winningStoneChance) {
                if (lblResult.getText().contains("won")) {
                    // Kazandıysa çoğunlukla kendi taşının rengini göster
                    stoneType = playerIsBlack ? Stone.BLACK : Stone.WHITE;
                } else if (lblResult.getText().contains("lost")) {
                    // Kaybettiyse çoğunlukla rakibin taşının rengini göster
                    stoneType = playerIsBlack ? Stone.WHITE : Stone.BLACK;
                } else {
                    // Beraberlikte eşit dağılım
                    stoneType = Math.random() < 0.5 ? Stone.BLACK : Stone.WHITE;
                }
            } else {
                // Geri kalan taşlar
                if (lblResult.getText().contains("won")) {
                    stoneType = playerIsBlack ? Stone.WHITE : Stone.BLACK;
                } else if (lblResult.getText().contains("lost")) {
                    stoneType = playerIsBlack ? Stone.BLACK : Stone.WHITE;
                } else {
                    stoneType = Math.random() < 0.5 ? Stone.BLACK : Stone.WHITE;
                }
            }
            
            // Rasgele başlangıç pozisyonu
            float x = (float) (Math.random() * 120);
            float y = (float) (Math.random() * -800); // Daha yüksekten başlasınlar
            
            // Rasgele boyut - daha çeşitli
            float size = (float) (8 + Math.random() * 25);
            
            // Rasgele hız - daha çeşitli
            float speed = (float) (0.8 + Math.random() * 3.5);
            
            // Rasgele dönüş
            float rotation = (float) (Math.random() * Math.PI * 2);
            float rotationSpeed = (float) ((Math.random() - 0.5) * 0.25);
            
            // Özel taş efekti - parlama
            boolean glowing = Math.random() < 0.3; // %30 şansla parlayan taş
            
            // Taş oluştur ve listeye ekle
            FallingStone stone = new FallingStone(stoneType, x, y, size, speed, rotation, rotationSpeed, glowing);
            fallingStones.add(stone);
        }
    }

    /**
     * Düşen taşların konumlarını günceller.
     */
    private void updateFallingStones() {
        int panelHeight = animationPanel.getHeight();
        
        for (int i = 0; i < fallingStones.size(); i++) {
            FallingStone stone = fallingStones.get(i);
            
            // Taşı hareket ettir
            stone.y += stone.speed;
            stone.rotation += stone.rotationSpeed;
            
            // Animasyonlar için zaman bazlı değerler
            stone.animationPhase = (float) ((stone.animationPhase + 0.05f) % (2 * Math.PI));
            
            // Ekranın altına ulaşırsa, tekrar yukarıdan başlat
            if (stone.y > panelHeight + stone.size) {
                stone.y = -stone.size;
                stone.x = (float) (Math.random() * animationPanel.getWidth());
                
                // Tekrar başlatınca farklı bir taş efekti olasılığı
                stone.glowing = Math.random() < 0.3;
            }
        }
    }

    /**
     * Düşen taşları gelişmiş efektlerle çizer.
     */
    private void drawFallingStones(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        for (FallingStone fs : fallingStones) {
            AffineTransform oldTransform = g2d.getTransform();
            
            // Taşı döndür
            g2d.translate(fs.x, fs.y);
            g2d.rotate(fs.rotation);
            
            // Animasyon için boyut varyasyonu
            float pulseScale = 1.0f + (float) Math.sin(fs.animationPhase) * 0.05f;
            int stoneSize = Math.round(fs.size * pulseScale);
            
            // Taşı çiz
            if (fs.type == Stone.BLACK) {
                // Siyah taş
                
                // Parlama efekti
                if (fs.glowing) {
                    // Dış parlama
                    g2d.setColor(new Color(80, 80, 255, 40));
                    g2d.fillOval(-stoneSize/2 - 5, -stoneSize/2 - 5, stoneSize + 10, stoneSize + 10);
                }
                
                // Ana taş gövdesi
                g2d.setColor(new Color(30, 30, 30, 200));
                g2d.fillOval(-stoneSize/2, -stoneSize/2, stoneSize, stoneSize);
                
                // Parlaklık - siyah taş için ışık yansıması
                int highlightSize = stoneSize/2;
                g2d.setColor(new Color(100, 100, 100, 70));
                g2d.fillOval(-highlightSize/2, -highlightSize/2, highlightSize, highlightSize);
                
            } else {
                // Beyaz taş
                
                // Parlama efekti
                if (fs.glowing) {
                    // Dış parlama
                    g2d.setColor(new Color(255, 255, 80, 40));
                    g2d.fillOval(-stoneSize/2 - 5, -stoneSize/2 - 5, stoneSize + 10, stoneSize + 10);
                }
                
                // Ana taş gövdesi
                g2d.setColor(new Color(220, 220, 220, 200));
                g2d.fillOval(-stoneSize/2, -stoneSize/2, stoneSize, stoneSize);
                
                // Dış çizgi
                g2d.setColor(new Color(150, 150, 150, 150));
                g2d.drawOval(-stoneSize/2, -stoneSize/2, stoneSize, stoneSize);
                
                // İç detaylar - hafif doku
                g2d.setColor(new Color(200, 200, 200, 50));
                g2d.fillOval(-stoneSize/3, -stoneSize/3, stoneSize/2, stoneSize/2);
            }
            
            // Transformasyonu sıfırla
            g2d.setTransform(oldTransform);
        }
    }

    /**
     * Yeni oyun butonuna tıklama olayını işler.
     */
    public void setNewGameAction(ActionListener listener) {
        btnNewGame.addActionListener(listener);
    }

    /**
     * Tahtayı görüntüle butonuna tıklama olayını işler.
     */
    public void setViewBoardAction(ActionListener listener) {
        btnViewBoard.addActionListener(listener);
    }

    /**
     * Çıkış butonuna tıklama olayını işler.
     */
    public void setExitAction(ActionListener listener) {
        btnExit.addActionListener(listener);
    }

    /**
     * Oyun sonu diyaloğunu görüntüler.
     */
    public static GameOverDialog showDialog(Frame owner, String result, int myScore,
            int oppScore, String reason, String gameStats, String role) {
        GameOverDialog dialog = new GameOverDialog(owner, result, myScore, oppScore,
                reason, gameStats, role);
        dialog.setVisible(true);
        return dialog;
    }

    /**
     * Düşen taş bilgilerini tutan geliştirilmiş iç sınıf.
     */
    private static class FallingStone {
        float x, y;          // Konum
        float size;          // Boyut
        float speed;         // Düşme hızı
        float rotation;      // Dönüş açısı
        float rotationSpeed; // Dönüş hızı
        Stone type;          // Taş tipi (BLACK/WHITE)
        boolean glowing;     // Parlama efekti
        float animationPhase; // Animasyon fazı (zaman bazlı efektler için)

        /**
         * Yeni bir düşen taş oluşturur.
         */
        public FallingStone(Stone type, float x, float y, float size,
                float speed, float rotation, float rotationSpeed, boolean glowing) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.glowing = glowing;
            this.animationPhase = (float)(Math.random() * 2 * Math.PI); // Rastgele başlangıç fazı
        }
    }
    
    /**
     * Modern kaydırma çubuğu görünümü
     */
    private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(150, 150, 150, 100);
            this.thumbDarkShadowColor = new Color(0, 0, 0, 0);
            this.thumbHighlightColor = new Color(0, 0, 0, 0);
            this.thumbLightShadowColor = new Color(0, 0, 0, 0);
            this.trackColor = new Color(0, 0, 0, 0);
            this.trackHighlightColor = new Color(0, 0, 0, 0);
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
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
        
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(thumbColor);
            g2d.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, 
                            thumbBounds.width - 2, thumbBounds.height - 2, 10, 10);
            
            g2d.dispose();
        }
        
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fillRoundRect(trackBounds.x, trackBounds.y, 
                            trackBounds.width, trackBounds.height, 10, 10);
            
            g2d.dispose();
        }
    }
}
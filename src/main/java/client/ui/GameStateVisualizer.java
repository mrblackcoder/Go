
package client.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import game.go.model.Point;
import game.go.model.*;
import game.go.util.GameRecorder;

/**
 * Go oyunu durum görselleştiricisi ve test aracı.
 * <p>
 * Bu sınıf, Go oyun durumunu görselleştirmek, hamleleri test etmek ve 
 * oyun durumunu incelemek için kullanılabilir. Özellikle geliştirme ve 
 * test aşamalarında faydalıdır.
 * </p>
 * 
 * @author Sistem Geliştirici
 * @version 1.0
 */
public class GameStateVisualizer extends JFrame {
    
    private final GoBoardPanel boardPanel;
    private final GameState gameState;
    private final JTextArea logArea;
    private final JButton btnBlackStone;
    private final JButton btnWhiteStone;
    private final JButton btnPass;
    private final JButton btnUndo;
    private final JButton btnSaveImage;
    private final JButton btnCalculateScore;
    private final JLabel lblTurn;
    private final JLabel lblScore;
    
    private Stone currentStone = Stone.BLACK;
    private final GameRecorder recorder;
    
    /**
     * Görselleştirici oluşturur.
     * 
     * @param boardSize Tahta boyutu (9, 13 veya 19)
     */
    public GameStateVisualizer(int boardSize) {
        super("Go Oyun Durumu Görselleştiricisi");
        
        this.gameState = new GameState(boardSize);
        this.recorder = new GameRecorder(boardSize, "Black", "White");
        gameState.setRecorder(recorder);
        
        // Ana panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(40, 40, 40));
        
        // Tahta paneli
        boardPanel = new GoBoardPanel();
        boardPanel.setBoard(gameState.board().getGridAsCharArray());
        boardPanel.setPreferredSize(new Dimension(600, 600));
        
        // Durum paneli
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBackground(new Color(60, 60, 60));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        
        // Sıra göstergesi
        lblTurn = new JLabel("Sıra: Siyah", JLabel.CENTER);
        lblTurn.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTurn.setForeground(Color.WHITE);
        
        // Skor göstergesi
        lblScore = new JLabel("Skor: Siyah 0 - Beyaz 0", JLabel.CENTER);
        lblScore.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblScore.setForeground(Color.WHITE);
        
        // Buton paneli
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        buttonPanel.setOpaque(false);
        
        btnBlackStone = new JButton("Siyah");
        btnBlackStone.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnBlackStone.setBackground(new Color(30, 30, 30));
        btnBlackStone.setForeground(Color.WHITE);
        btnBlackStone.setFocusPainted(false);
        btnBlackStone.setBorderPainted(false);
        
        btnWhiteStone = new JButton("Beyaz");
        btnWhiteStone.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnWhiteStone.setBackground(new Color(220, 220, 220));
        btnWhiteStone.setForeground(Color.BLACK);
        btnWhiteStone.setFocusPainted(false);
        btnWhiteStone.setBorderPainted(false);
        
        btnPass = new JButton("Pas");
        styleButton(btnPass, new Color(70, 70, 170));
        
        btnUndo = new JButton("Geri Al");
        styleButton(btnUndo, new Color(170, 70, 70));
        
        btnCalculateScore = new JButton("Puanla");
        styleButton(btnCalculateScore, new Color(70, 170, 70));
        
        buttonPanel.add(btnBlackStone);
        buttonPanel.add(btnWhiteStone);
        buttonPanel.add(btnPass);
        buttonPanel.add(btnUndo);
        buttonPanel.add(btnCalculateScore);
        
        // Log alanı
        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(50, 50, 50));
        logArea.setForeground(Color.WHITE);
        logArea.setCaretColor(Color.WHITE);
        
        JScrollPane logScrollPane = new JScrollPane(logArea);
        
        // Ekstra işlemler paneli
        JPanel extraPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        extraPanel.setOpaque(false);
        
        btnSaveImage = new JButton("Tahtayı Kaydet");
        styleButton(btnSaveImage, new Color(100, 100, 100));
        
        extraPanel.add(btnSaveImage);
        
        // Panelleri yerleştir
        statusPanel.add(lblTurn, BorderLayout.NORTH);
        statusPanel.add(lblScore, BorderLayout.CENTER);
        statusPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        JPanel eastPanel = new JPanel(new BorderLayout(5, 10));
        eastPanel.setOpaque(false);
        eastPanel.add(statusPanel, BorderLayout.NORTH);
        eastPanel.add(logScrollPane, BorderLayout.CENTER);
        eastPanel.add(extraPanel, BorderLayout.SOUTH);
        
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(eastPanel, BorderLayout.EAST);
        
        // Olayları ayarla
        setupEvents();
        
        // Son düzenlemeler
        setContentPane(mainPanel);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Başlangıç durumunu göster
        updateUI();
        logAction("Görselleştirici başlatıldı - " + boardSize + "x" + boardSize + " tahta");
    }
    
    /**
     * Buton stilini ayarlar.
     * 
     * @param button Stillendirilecek buton
     * @param color Buton rengi
     */
    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }
    
    /**
     * Tüm olayları ayarlar.
     */
    private void setupEvents() {
        // Taş seçme butonları
        btnBlackStone.addActionListener(e -> {
            currentStone = Stone.BLACK;
            updateUI();
            logAction("Siyah taş seçildi");
        });
        
        btnWhiteStone.addActionListener(e -> {
            currentStone = Stone.WHITE;
            updateUI();
            logAction("Beyaz taş seçildi");
        });
        
        // Tahta tıklama olayı
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = boardPanel.getBoardCoordinates(e.getX(), e.getY());
                if (p != null) {
                    makeMove(p);
                }
            }
        });
        
        // Pas geçme butonu
        btnPass.addActionListener(e -> {
            Board.MoveResult result = gameState.pass();
            if (result.valid) {
                logAction(currentStone + " pas geçti");
                switchTurn();
                updateUI();
                
                // İki pas üst üste oyunu bitirir
                if (gameState.isOver()) {
                    showGameOverDialog("İki pas üst üste - oyun bitti");
                }
            } else {
                logAction("Pas geçme başarısız: " + result.message);
            }
        });
        
        // Geri alma butonu
        btnUndo.addActionListener(e -> {
            // Bu basit görselleştiricide geri alma işlevselliği yok
            // Tam implementasyon için oyun durumunun önceki halini saklamak gerekir
            logAction("Geri alma işlevi bu görselleştiricide desteklenmiyor");
        });
        
        // Puanlama butonu
        btnCalculateScore.addActionListener(e -> {
            calculateAndShowScore();
        });
        
        // Tahta görüntüsünü kaydetme butonu
        btnSaveImage.addActionListener(e -> {
            saveBoardImage();
        });
    }
    
    /**
     * Belirtilen noktaya hamle yapar.
     * 
     * @param p Hamle yapılacak nokta
     */
private void makeMove(game.go.model.Point p) {
    // Hamleyi yap
    Board.MoveResult result = gameState.play(p);
    
    if (result.valid) {
        logAction(currentStone + " hamle yaptı: (" + p.x() + "," + p.y() + ")");
        
        // Başarılı hamle sonrası UI güncelleme
        boardPanel.setBoard(gameState.board().getGridAsCharArray());
        boardPanel.setLastMove(p);
        
        // Sırayı değiştir
        switchTurn();
        
        // UI güncelle
        updateUI();
    } else {
        logAction("Geçersiz hamle: " + result.message);
    }
}
    
    /**
     * Sırayı diğer oyuncuya geçirir.
     */
    private void switchTurn() {
        currentStone = (currentStone == Stone.BLACK) ? Stone.WHITE : Stone.BLACK;
    }
    
    /**
     * Puanları hesaplar ve gösterir.
     */
    private void calculateAndShowScore() {
        Map<Stone, Integer> scores = gameState.calculateTerritorialScores();
        int blackScore = scores.get(Stone.BLACK);
        int whiteScore = scores.get(Stone.WHITE);
        
        String winner;
        if (blackScore > whiteScore) {
            winner = "Siyah " + (blackScore - whiteScore) + " farkla kazandı";
        } else if (whiteScore > blackScore) {
            winner = "Beyaz " + (whiteScore - blackScore) + " farkla kazandı";
        } else {
            winner = "Berabere";
        }
        
        logAction("Puanlama: Siyah " + blackScore + " - Beyaz " + whiteScore + " (" + winner + ")");
        updateScore(blackScore, whiteScore);
        
        JOptionPane.showMessageDialog(this, 
                "Siyah: " + blackScore + "\nBeyaz: " + whiteScore + "\n\n" + winner, 
                "Oyun Puanlaması", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Skoru günceller.
     * 
     * @param blackScore Siyah skoru
     * @param whiteScore Beyaz skoru
     */
    private void updateScore(int blackScore, int whiteScore) {
        lblScore.setText("Skor: Siyah " + blackScore + " - Beyaz " + whiteScore);
    }
    
    /**
     * Oyun durumuna göre UI'ı günceller.
     */
    private void updateUI() {
        // Sıra göstergesini güncelle
        lblTurn.setText("Sıra: " + (currentStone == Stone.BLACK ? "Siyah" : "Beyaz"));
        
        // Buton vurgularını güncelle
        btnBlackStone.setBorder(currentStone == Stone.BLACK ? 
                BorderFactory.createLineBorder(Color.GREEN, 2) : null);
        btnWhiteStone.setBorder(currentStone == Stone.WHITE ? 
                BorderFactory.createLineBorder(Color.GREEN, 2) : null);
        
        // Tahta durumunu güncelle
        boardPanel.setBoard(gameState.board().getGridAsCharArray());
        
        // Skor güncelle
        int blackScore = gameState.scoreFor(Stone.BLACK);
        int whiteScore = gameState.scoreFor(Stone.WHITE);
        updateScore(blackScore, whiteScore);
    }
    
    /**
     * Log alanına yeni bir eylem kaydeder.
     * 
     * @param action Kaydedilecek eylem
     */
    private void logAction(String action) {
        logArea.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + action + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    /**
     * Oyun sonu diyaloğunu gösterir.
     * 
     * @param reason Oyunun bitme nedeni
     */
    private void showGameOverDialog(String reason) {
        calculateAndShowScore();
        
        JOptionPane.showMessageDialog(this, 
                "Oyun bitti!\nNeden: " + reason + "\n\nYeni oyun için uygulamayı yeniden başlatın.", 
                "Oyun Sonu", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Tahta görüntüsünü dosyaya kaydeder.
     */
    private void saveBoardImage() {
        // Tahtanın boyutlarını al
        int width = boardPanel.getWidth();
        int height = boardPanel.getHeight();
        
        // Boş görüntü oluştur
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Tahtayı görüntüye çiz
        Graphics2D g2d = image.createGraphics();
        boardPanel.paint(g2d);
        g2d.dispose();
        
        // Dosya kaydet diyaloğunu göster
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Tahtayı Kaydet");
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            
            // Dosya uzantısını kontrol et ve gerekirse ekle
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".png")) {
                filePath += ".png";
                fileToSave = new File(filePath);
            }
            
            try {
                ImageIO.write(image, "png", fileToSave);
                logAction("Tahta görüntüsü kaydedildi: " + fileToSave.getName());
            } catch (IOException ex) {
                logAction("Görüntü kaydedilemedi: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, 
                        "Görüntü kaydedilemedi: " + ex.getMessage(),
                        "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Uygulamayı başlatır.
     * 
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            // Kullanıcıdan tahta boyutunu sor
            String[] options = {"9x9", "13x13", "19x19"};
            int choice = JOptionPane.showOptionDialog(null, 
                    "Tahta boyutunu seçin:", 
                    "Go Görselleştiricisi", 
                    JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, options, options[2]);
            
            int boardSize;
            if (choice == 0) {
                boardSize = 9;
            } else if (choice == 1) {
                boardSize = 13;
            } else {
                boardSize = 19; // Varsayılan
            }
            
            new GameStateVisualizer(boardSize).setVisible(true);
        });
    }
}
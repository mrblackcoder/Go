package client.ui;

import common.Message;
import game.go.model.Board;
import game.go.model.Point;
import game.go.model.Stone;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;
import game.go.model.Point;


/**
 * Go oyunu tahtasını görselleştiren ve kullanıcı etkileşimlerini yöneten panel.
 * İyileştirilmiş grafik özellikleri ve animasyonlar içerir.
 */
public class GoBoardPanel extends JPanel {

    /**
     * Hücre boyutu için minimum piksel değeri
     */
    private static final int MIN_CELL_SIZE = 20;

    /**
     * Varsayılan Go tahtası boyutu
     */
    private static final int DEFAULT_BOARD_SIZE = 19;

    /**
     * Koordinat gösterimi için tahta kenarı marjı
     */
    private static final int BOARD_MARGIN = 30;

    /**
     * Tahta arka plan rengi - geleneksel Go tahtası rengi
     */
    private final Color BOARD_COLOR = new Color(219, 176, 102);

    /**
     * İkincil tahta rengi - ızgara için
     */
    private final Color BOARD_COLOR_SECONDARY = new Color(200, 160, 90);

    /**
     * Izgara çizgi rengi
     */
    private final Color LINE_COLOR = new Color(30, 30, 30);

    /**
     * Siyah taş rengi ve vurgu
     */
    private final Color BLACK_STONE_COLOR = new Color(20, 20, 20);
    private final Color BLACK_STONE_HIGHLIGHT = new Color(50, 50, 50);

    /**
     * Beyaz taş rengi ve vurgu
     */
    private final Color WHITE_STONE_COLOR = new Color(240, 240, 240);
    private final Color WHITE_STONE_HIGHLIGHT = new Color(255, 255, 255);

    /**
     * Son hamle gösterimi için renk
     */
    private final Color LAST_MOVE_COLOR = new Color(255, 0, 0, 140);

    /**
     * Taş görünümü için ışık kaynağı açısı
     */
    private static final double LIGHT_ANGLE = Math.PI / 4;

    /**
     * Taşların 3D görünümü için yükseklik
     */
    private static final int STONE_HEIGHT = 5;

    // Tahta boyutları
    private int boardSize = DEFAULT_BOARD_SIZE;
    private int cellSize = MIN_CELL_SIZE;
    private int boardX = BOARD_MARGIN;
    private int boardY = BOARD_MARGIN;
    private int boardWidth;
    private int boardHeight;
    private boolean boardSizeChanged = true;

    // Oyun durumu
    private char[][] board = new char[DEFAULT_BOARD_SIZE][DEFAULT_BOARD_SIZE];
    private Point lastMove = null;
    private Point hoverPoint = null;
    private Color hoverColor = new Color(0, 0, 0, 64);

    // Animasyon kontrolü
    private Point animatedStone = null;
    private float animationProgress = 0f;
    private Timer animationTimer = new Timer(16, null); // Burada başlat, lambda ifadesini constructor'da ekleyin

    // Görsel kaynaklar
    private final Map<String, Image> imageCache = new HashMap<>();

    // Eksik değişkenler - eklendi
    private boolean soundEnabled = true;
    private Timer blinkTimer;
    private Timer blinkStopTimer;
    private Timer captureAnimationTimer;
    private int animationType = 0; // 0: None, 1: Place, 2: Capture
    private boolean showPlacementHints = false;
    private boolean blinkState = true;

    /**
     * Tahta için arkaplan dokusu
     */
    private BufferedImage boardTexture;

    /**
     * Go tahtası paneli oluşturur.
     */
    public GoBoardPanel() {
        // Panel ayarları
        setBackground(new Color(240, 230, 200));

        // Tahta başlangıç durumu
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j] = '.';
            }
        }

        // Tahta dokusunu yükle
        try {
            boardTexture = ImageIO.read(getClass().getResourceAsStream("/images/wood_texture.jpg"));
            if (boardTexture == null) {
                // Kaynak bulunamadıysa veya yüklenemiyorsa, düz renk kullanılacak
                System.out.println("Board texture could not be loaded. Using plain color.");
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Error loading board texture: " + e.getMessage());
        }

        // Animasyon zamanlayıcısı
        animationTimer = new Timer(16, e -> {
            if (animatedStone != null) {
                animationProgress += 0.1f;
                if (animationProgress >= 1.0f) {
                    animationProgress = 0f;
                    animatedStone = null;
                    animationTimer.stop();
                }
                repaint();
            }
        });

        // Yakalama animasyonu zamanlayıcısı - eklendi
        captureAnimationTimer = new Timer(16, e -> {
            if (animatedStone != null) {
                animationProgress += 0.1f;
                if (animationProgress >= 1.0f) {
                    animationProgress = 0f;
                    animatedStone = null;
                    captureAnimationTimer.stop();
                }
                repaint();
            }
        });

        // Boyutlandırma hesaplaması
        calculateBoardDimensions();
    }




  

    /**
     * Mevcut panel boyutlarına göre tahta boyutlarını hesaplar. Responsive
     * tasarım için tahtanın pencere boyutuna uyarlanmasını sağlar.
     */
    private void calculateBoardDimensions() {
        // Mevcut panel boyutlarını al
        Dimension size = getSize();
        if (size.width <= 0 || size.height <= 0) {
            // Eğer panel henüz boyutlandırılmamışsa varsayılan değerler kullan
            size = new Dimension(600, 600);
        }

        // Panel boyutlarına göre hücre boyutunu hesapla
        int availableWidth = size.width - 2 * BOARD_MARGIN;
        int availableHeight = size.height - 2 * BOARD_MARGIN;

        // En büyük olası hücre boyutu, panel boyutlarına göre sınırlanmış
        int maxCellSize = Math.min(availableWidth, availableHeight) / boardSize;

        // Hücre boyutunu ayarla, minimum boyut kontrolü ile
        cellSize = Math.max(MIN_CELL_SIZE, maxCellSize);

        // Tahta boyutlarını hesapla
        boardWidth = boardSize * cellSize;
        boardHeight = boardSize * cellSize;

        // Tahtayı panel içinde ortala
        boardX = (size.width - boardWidth) / 2;
        boardY = (size.height - boardHeight) / 2;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        calculateBoardDimensions();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        calculateBoardDimensions();
    }

    @Override
    public void setSize(Dimension d) {
        super.setSize(d);
        calculateBoardDimensions();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother drawing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw board
        drawBoard(g2d);

        // Draw coordinates
        drawCoordinates(g2d);

        // Draw stones
        drawStones(g2d);

        // Draw placement hints
        if (showPlacementHints) {
            drawPlacementHints(g2d);
        }

        // Draw hover highlight
        drawHoverHighlight(g2d);

        // Highlight last move
        drawLastMoveHighlight(g2d);
    }

    /**
     * Hamle yerleştirme ipuçlarını etkinleştirir veya devre dışı bırakır
     *
     * @param enable true ise ipuçları gösterilir, false ise gösterilmez
     */
    public void enablePlacementHints(boolean enable) {
        this.showPlacementHints = enable;
        repaint(); // Tahtayı yenile
    }

    /**
     * Hamle yerleştirme ipuçlarını çizer
     */
    private void drawPlacementHints(Graphics2D g2d) {
        if (!showPlacementHints) {
            return; // İpuçları devre dışıysa hiçbir şey çizme
        }

        // Tüm boş noktalara hafif vurgu ekle
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] == '.') { // Boş ise
                    int centerX = boardX + x * cellSize;
                    int centerY = boardY + y * cellSize;

                    // Hamle yapılabilecek yer göstergesi - hafif yeşil daire
                    g2d.setColor(new Color(0, 255, 0, 30)); // Çok şeffaf yeşil
                    g2d.fillOval(centerX - cellSize / 4, centerY - cellSize / 4, cellSize / 2, cellSize / 2);
                }
            }
        }
    }

    /**
     * Tahta boyutunu ayarlar
     *
     * @param newSize Yeni tahta boyutu
     */
    public void setSize(int newSize) {
        this.boardSize = newSize;
        boardSizeChanged = true;
        calculateBoardDimensions();
        repaint();
    }

    private Point hoverPosition = null;


 
    /**
     * Tahta arka planını ve ızgara çizgilerini çizer. İyileştirilmiş görsel
     * efektler içerir.
     *
     * @param g2d Grafik bağlamı
     */
    private void drawBoard(Graphics2D g2d) {
        // Tahta arka planını çiz
        if (boardTexture != null) {
            // Doku kullan
            g2d.drawImage(boardTexture, boardX, boardY, boardWidth, boardHeight, this);

            // Doku üzerine yarı saydam renk kaplama
            g2d.setColor(new Color(BOARD_COLOR.getRed(), BOARD_COLOR.getGreen(),
                    BOARD_COLOR.getBlue(), 120));
            g2d.fillRect(boardX, boardY, boardWidth, boardHeight);
        } else {
            // Doku yoksa düz renk kullan
            g2d.setColor(BOARD_COLOR);
            g2d.fillRect(boardX, boardY, boardWidth, boardHeight);

            // Ahşap görünümü için gradyan ekle
            Paint oldPaint = g2d.getPaint();
            GradientPaint woodGrain = new GradientPaint(
                    boardX, boardY, new Color(219, 176, 102, 255),
                    boardX + boardWidth, boardY + boardHeight, new Color(200, 160, 90, 255));
            g2d.setPaint(woodGrain);
            g2d.fillRect(boardX, boardY, boardWidth, boardHeight);
            g2d.setPaint(oldPaint);
        }

        // Izgara çizgilerini çiz
        g2d.setColor(LINE_COLOR);
        g2d.setStroke(new BasicStroke(1.0f));

        for (int i = 0; i < boardSize; i++) {
            // Yatay çizgiler
            g2d.drawLine(boardX, boardY + i * cellSize,
                    boardX + boardWidth - cellSize, boardY + i * cellSize);

            // Dikey çizgiler
            g2d.drawLine(boardX + i * cellSize, boardY,
                    boardX + i * cellSize, boardY + boardHeight - cellSize);
        }

        // Yıldız noktalarını (hoshi) çiz
        drawStarPoints(g2d);
    }

    /**
     * Tahtadaki yıldız noktalarını (hoshi) çizer.
     *
     * @param g2d Grafik bağlamı
     */
    private void drawStarPoints(Graphics2D g2d) {
        g2d.setColor(new Color(30, 30, 30));

        // Tahta boyutuna göre yıldız noktaları tanımla
        int[] starPoints;
        if (boardSize == 19) {
            starPoints = new int[]{3, 9, 15};
        } else if (boardSize == 13) {
            starPoints = new int[]{3, 6, 9};
        } else if (boardSize == 9) {
            starPoints = new int[]{2, 4, 6};
        } else {
            return; // Diğer tahta boyutları için yıldız noktası yok
        }

        // Yıldız noktalarını çiz
        int dotSize = Math.max(4, cellSize / 5);
        for (int x : starPoints) {
            for (int y : starPoints) {
                g2d.fillOval(boardX + x * cellSize - dotSize / 2,
                        boardY + y * cellSize - dotSize / 2,
                        dotSize, dotSize);
            }
        }
    }

// Son hamleyi çizmek için Graphics2D parametreli özel metot
    private void drawLastMoveHighlight(Graphics2D g2d) {
        if (lastMove != null) {
            int centerX = boardX + lastMove.x() * cellSize;
            int centerY = boardY + lastMove.y() * cellSize;
            g2d.setColor(LAST_MOVE_COLOR);
            g2d.fillOval(centerX - cellSize / 3, centerY - cellSize / 3, cellSize * 2 / 3, cellSize * 2 / 3);
        }
    }

    /**
     * Tahta etrafına koordinat etiketlerini çizer.
     *
     * @param g2d Grafik bağlamı
     */
    private void drawCoordinates(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        Font coordFont = new Font("SansSerif", Font.BOLD, Math.max(12, cellSize / 3));
        g2d.setFont(coordFont);
        g2d.setStroke(new BasicStroke(1.5f));

        // Koordinat kutuları ekleyin
        int boxSize = Math.max(18, cellSize / 2);

        // Geleneksel Go notasyonuna uygun olarak "I" harf atlanır
        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T'};

        // Sütunlar için harf koordinatları çiz
        for (int i = 0; i < Math.min(boardSize, letters.length); i++) {
            String colLabel = String.valueOf(letters[i]);

            // Sütun harfini üstte göster
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(colLabel);
            g2d.drawString(colLabel,
                    boardX + i * cellSize + (cellSize - labelWidth) / 2,
                    boardY - 5);

            // Sütun harfini altta göster
            g2d.drawString(colLabel,
                    boardX + i * cellSize + (cellSize - labelWidth) / 2,
                    boardY + boardHeight + fm.getAscent() + 2);
        }

        // Satırlar için rakam koordinatları çiz (yukarıdan aşağıya: 19, 18, ..., 1)
        for (int i = 0; i < boardSize; i++) {
            String rowLabel = String.valueOf(boardSize - i);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(rowLabel);
            int labelHeight = fm.getAscent();

            // Satır numarasını solda göster
            g2d.drawString(rowLabel,
                    boardX - labelWidth - 5,
                    boardY + i * cellSize + (cellSize + labelHeight) / 2 - 2);

            // Satır numarasını sağda göster
            g2d.drawString(rowLabel,
                    boardX + boardWidth + 5,
                    boardY + i * cellSize + (cellSize + labelHeight) / 2 - 2);
        }
    }

    /**
     * Tahta boyutunu döndürür
     *
     * @return Tahta boyutu
     */
    public int getBoardSize() {
        return boardSize;
    }

    /**
     * Mevcut tahta durumunu döndürür
     *
     * @return Tahta durumu (2D char dizisi olarak: '.', 'B', 'W')
     */
    public char[][] getBoard() {
        int size = getBoardSize();
        char[][] currentBoard = new char[size][size];

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Point p = new Point(x, y);
                Stone stone = this.getStoneAt(p);

                if (stone == Stone.BLACK) {
                    currentBoard[y][x] = 'B';
                } else if (stone == Stone.WHITE) {
                    currentBoard[y][x] = 'W';
                } else {
                    currentBoard[y][x] = '.';
                }
            }
        }

        return currentBoard;
    }

    /**
     * Belirtilen konumdaki taşı döndürür (Bu yardımcı metot getBoard için
     * gerekli)
     */
    private Stone getStoneAt(Point p) {
        if (p.x() >= 0 && p.x() < boardSize && p.y() >= 0 && p.y() < boardSize) {
            char value = board[p.y()][p.x()];
            if (value == 'B' || value == 'b') {
                return Stone.BLACK;
            } else if (value == 'W' || value == 'w') {
                return Stone.WHITE;
            }
        }
        return Stone.EMPTY;
    }

    /**
     * Tahtadaki tüm taşları çizer. İyileştirilmiş 3B görünüm ve gölgelendirme
     * efektleri ile.
     *
     * @param g2d Grafik bağlamı
     */
    private void drawStones(Graphics2D g2d) {
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                char stone = board[y][x];
                if (stone == '.') {
                    continue; // Boş kesişim
                }

                if (animatedStone != null && animatedStone.x() == x && animatedStone.y() == y) {
                    continue; // Bu taş animasyonlu, ayrı çizilecek
                }

                // Taş boyutu - hücre boyutu biraz küçük olacak şekilde ayarla
                int stoneSize = (int) (cellSize * 0.95);
                int centerX = boardX + x * cellSize;
                int centerY = boardY + y * cellSize;

                if (stone == 'B' || stone == 'b') {
                    // Siyah taş çiz (ölü taş için küçük harfle 'b' kullanılır)
                    drawStone3D(g2d, centerX, centerY, stoneSize, true, stone == 'b');

                } else if (stone == 'W' || stone == 'w') {
                    // Beyaz taş çiz (ölü taş için küçük harfle 'w' kullanılır)
                    drawStone3D(g2d, centerX, centerY, stoneSize, false, stone == 'w');
                }
            }
        }
    }

    /**
     * Animasyonlu taşı çizer.
     *
     * @param g2d Grafik bağlamı
     */
    private void drawAnimatedStone(Graphics2D g2d) {
        if (animatedStone == null) {
            return;
        }

        // Animasyon taş bilgisini al
        int x = animatedStone.x();
        int y = animatedStone.y();
        char stone = board[y][x];

        if (stone == '.') {
            return; // Boş noktada animasyon yok
        }
        int stoneSize = (int) (cellSize * 0.95);
        int centerX = boardX + x * cellSize;
        int centerY = boardY + y * cellSize;

        // Animasyon etkisi: Küçülüp büyüme ve taşın yere düşmesi
        float scale = Math.min(1.0f, animationProgress * 1.2f);
        if (animationProgress < 0.2f) {
            // Başlangıçta taş biraz düşüyor gibi yukarıdan gelmeli
            centerY -= (1 - animationProgress * 5) * cellSize * 0.5;
        } else if (animationProgress < 0.5f) {
            // Taş zemine hafifçe zıplamalı
            float bounce = (float) Math.sin((animationProgress - 0.2f) * Math.PI / 0.3f) * 0.1f;
            centerY -= bounce * cellSize;
        }

        int animStoneSize = (int) (stoneSize * scale);

        // Taşı çiz
        boolean isBlack = (stone == 'B' || stone == 'b');
        boolean isDead = (stone == 'b' || stone == 'w');

        drawStone3D(g2d, centerX, centerY, animStoneSize, isBlack, isDead);
    }

    private void setupBlinkEffect() {
        if (blinkTimer == null) {
            blinkTimer = new Timer(500, e -> {
                blinkState = !blinkState;
                repaint();
            });
            blinkTimer.start();
        }
    }

    

// Hover pozisyonunu güncelleme
public void updateHoverPosition(Point position) {
    // Eski pozisyonla aynı mı kontrol et (gereksiz yeniden çizimi önle)
    if ((hoverPosition == null && position != null) ||
        (hoverPosition != null && !hoverPosition.equals(position))) {
        
        hoverPosition = position;
        repaint(); // Tahta görüntüsünü yenile
    }
}

/**
 * Taşın veya taş grubunun özgürlüğü (liberty) olup olmadığını kontrol eder
 *
 * @param board Tahta durumu
 * @param x X koordinatı
 * @param y Y koordinatı
 * @param stone Kontrol edilecek taş rengi karakteri
 * @return Özgürlük varsa true, yoksa false
 */
private boolean hasLiberties(char[][] board, int x, int y, char stone) {
    int boardSize = board.length;
    boolean[][] visited = new boolean[boardSize][boardSize];
    
    return checkLiberties(board, x, y, stone, visited);
}

/**
 * Recursive olarak bir taş grubunun özgürlüklerini kontrol eder
 */
private boolean checkLiberties(char[][] board, int x, int y, char stone, boolean[][] visited) {
    int boardSize = board.length;
    
    // Sınırları kontrol et
    if (x < 0 || x >= boardSize || y < 0 || y >= boardSize) {
        return false;
    }
    
    // Zaten ziyaret edilmiş mi?
    if (visited[y][x]) {
        return false;
    }
    
    // Eğer boş bir hücre ise, liberty bulundu demektir
    if (board[y][x] == '.') {
        return true;
    }
    
    // Eğer farklı bir taş ise, bu yoldan liberty yok
    if (board[y][x] != stone) {
        return false;
    }
    
    // Bu noktayı ziyaret edildi olarak işaretle
    visited[y][x] = true;
    
    // Komşularda liberty kontrolü yap (kuzey, doğu, güney, batı)
    return checkLiberties(board, x, y - 1, stone, visited) || 
           checkLiberties(board, x + 1, y, stone, visited) ||
           checkLiberties(board, x, y + 1, stone, visited) ||
           checkLiberties(board, x - 1, y, stone, visited);
}
    
    
    /**
     * 3B görünümlü ve gölgeli bir taş çizer.
     *
     * @param g2d Grafik bağlamı
     * @param centerX Taşın X merkez koordinatı
     * @param centerY Taşın Y merkez koordinatı
     * @param stoneSize Taş boyutu
     * @param isBlack Siyah taş ise true, beyaz ise false
     * @param isDead Ölü taş ise true
     */
    private void drawStone3D(Graphics2D g2d, int centerX, int centerY,
            int stoneSize, boolean isBlack, boolean isDead) {
        // Taş çizim alanını belirle
        int x = centerX - stoneSize / 2;
        int y = centerY - stoneSize / 2;

        // Hafif gölge efekti
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillOval(x + 2, y + 2, stoneSize, stoneSize);

        // Taş gövdesi için ana renk
        Color baseColor, highlightColor;
        if (isBlack) {
            baseColor = BLACK_STONE_COLOR;
            highlightColor = BLACK_STONE_HIGHLIGHT;
        } else {
            baseColor = WHITE_STONE_COLOR;
            highlightColor = WHITE_STONE_HIGHLIGHT;
        }

        // Ölü taşların görünümünü biraz değiştir
        if (isDead) {
            // Ölü taşlar için daha soluk bir görünüm
            if (isBlack) {
                baseColor = new Color(60, 60, 60);
                highlightColor = new Color(90, 90, 90);
            } else {
                baseColor = new Color(180, 180, 180);
                highlightColor = new Color(210, 210, 210);
            }
        }

        // Taş gövdesi
        g2d.setColor(baseColor);
        g2d.fillOval(x, y, stoneSize, stoneSize);

        // Işık kaynağını simüle etmek için gradyan
        Paint oldPaint = g2d.getPaint();

        // Ön yüzeye gradyan
        int lightX = (int) (centerX + Math.cos(LIGHT_ANGLE) * stoneSize * 0.2);
        int lightY = (int) (centerY - Math.sin(LIGHT_ANGLE) * stoneSize * 0.2);

        RadialGradientPaint stonePaint = new RadialGradientPaint(
                lightX, lightY, stoneSize,
                new float[]{0.0f, 0.7f},
                new Color[]{highlightColor, baseColor});

        g2d.setPaint(stonePaint);
        g2d.fillOval(x, y, stoneSize, stoneSize);

        // Üst parlaklık
        if (!isBlack) {
            // Beyaz taşlar için gümüşümsü parlaklık
            int shineSize = stoneSize / 3;
            int shineX = (int) (lightX - shineSize / 2);
            int shineY = (int) (lightY - shineSize / 2);

            RadialGradientPaint shinePaint = new RadialGradientPaint(
                    lightX, lightY, shineSize,
                    new float[]{0.0f, 1.0f},
                    new Color[]{new Color(255, 255, 255, 180),
                        new Color(255, 255, 255, 0)});

            g2d.setPaint(shinePaint);
            g2d.fillOval(shineX, shineY, shineSize, shineSize);
        }

        g2d.setPaint(oldPaint);

        // Kenar çizgisi
        g2d.setColor(isBlack ? new Color(0, 0, 0, 100) : new Color(0, 0, 0, 50));
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawOval(x, y, stoneSize, stoneSize);

        // Ölü taş işareti çiz
        if (isDead) {
            g2d.setColor(isBlack ? Color.WHITE : Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            int margin = stoneSize / 4;
            g2d.drawLine(x + margin, y + margin, x + stoneSize - margin, y + stoneSize - margin);
            g2d.drawLine(x + stoneSize - margin, y + margin, x + margin, y + stoneSize - margin);
        }
    }

    /**
     * Fare imlecinin üzerinde olduğu pozisyonda yarı saydam taş gösterir.
     *
     * @param g2d Grafik bağlamı
     */
    private void drawHoverHighlight(Graphics2D g2d) {
        if (hoverPoint != null) {
            int x = hoverPoint.x();
            int y = hoverPoint.y();

            if (y < boardSize && x < boardSize && board[y][x] == '.') { // Sadece boş kesişimlerde göster
                int stoneSize = (int) (cellSize * 0.95);
                int centerX = boardX + x * cellSize;
                int centerY = boardY + y * cellSize;

                // Yarı saydam taş
                Color hoverBaseColor;
                Color hoverHighlight;

                // Fare üzerindeki taş rengi (hoverColor'a göre)
                boolean isHoverBlack = hoverColor.getRed() < 100;

                if (isHoverBlack) {
                    hoverBaseColor = new Color(0, 0, 0, 80);
                    hoverHighlight = new Color(50, 50, 50, 80);
                } else {
                    hoverBaseColor = new Color(255, 255, 255, 100);
                    hoverHighlight = new Color(255, 255, 255, 120);
                }

                // Yarı saydam taş çiz
                int x1 = centerX - stoneSize / 2;
                int y1 = centerY - stoneSize / 2;

                g2d.setColor(hoverBaseColor);
                g2d.fillOval(x1, y1, stoneSize, stoneSize);

                // Taş kenarı
                if (!isHoverBlack) {
                    g2d.setColor(new Color(0, 0, 0, 50));
                    g2d.drawOval(x1, y1, stoneSize, stoneSize);
                }
            }
        }
    }

    /**
     * Ekran koordinatlarını tahta koordinatlarına dönüştürür.
     *
     * @param screenX Ekrandaki X koordinatı
     * @param screenY Ekrandaki Y koordinatı
     * @return Tahta üzerindeki nokta, tahtanın dışındaysa null
     */
    public Point getBoardCoordinates(int screenX, int screenY) {
        // Tahtanın dışında mı kontrol et
        if (screenX < boardX - cellSize / 2 || screenX > boardX + boardWidth + cellSize / 2
                || screenY > boardY + boardHeight + cellSize / 2) {
            return null;
        }

        // Ekran koordinatlarını tahta koordinatlarına çevir
        int x = Math.round((float) (screenX - boardX) / cellSize);
        int y = Math.round((float) (screenY - boardY) / cellSize);

        // Koordinatların tahta sınırları içinde olup olmadığını kontrol et
        if (x >= 0 && x < boardSize && y >= 0 && y < boardSize) {
            return new Point(x, y);
        }
        return null;
    }

    /**
     * Tahta boyutunu ayarlar
     *
     * @param newSize Yeni tahta boyutu
     */
    public void setBoardSize(int newSize) {
        if (this.boardSize != newSize) {
            this.boardSize = newSize;
            this.boardSizeChanged = true;
            calculateBoardDimensions();
            repaint();
        }
    }

    /**
     * Tahta verisini günceller ve tekrar çizim isteği gönderir.
     *
     * @param boardData Yeni tahta verisi
     */
    public void setBoard(char[][] boardData) {
        if (boardData == null || boardData.length == 0) {
            return;
        }

        // Tahta boyutu değiştiyse yapıları güncelle
        if (boardData.length != boardSize) {
            boardSize = boardData.length;
            board = new char[boardSize][boardSize];
            calculateBoardDimensions();
        }

        // Yeni hamleleri belirle (animasyon için)
        findNewStones(boardData);

        // Tahta verisini kopyala
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (x < boardData[y].length) {
                    board[y][x] = boardData[y][x];
                } else {
                    board[y][x] = '.';
                }
            }
        }

        repaint();
    }

    /**
     * Yeni tahta durumundaki yeni taşları belirler ve animasyon başlatır.
     *
     * @param newBoard Yeni tahta durumu
     */
    private void findNewStones(char[][] newBoard) {
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                // Eski board boş ama yeni board'da taş varsa, yeni hamle yapılmış
                if (board[y][x] == '.' && (newBoard[y][x] == 'B' || newBoard[y][x] == 'W')) {
                    startStoneAnimation(new Point(x, y));
                    return; // Sadece bir taş için animasyon başlat
                }
            }
        }
    }

    /**
     * Belirtilen noktada taş yerleştirme animasyonu başlatır.
     *
     * @param p Animasyon başlatılacak nokta
     */
    private void startStoneAnimation(Point p) {
        animatedStone = p;
        animationProgress = 0f;
        animationTimer.start();
    }

    
    /**
 * Belirtilen konuma taş yerleştirmenin intihar hamlesi olup olmadığını kontrol eder
 *
 * @param point Taş yerleştirilecek konum
 * @param stoneChar Yerleştirilecek taşın karakteri ('B' veya 'W')
 * @return İntihar hamlesi ise true
 */
public boolean isSuicideMove(game.go.model.Point point, char stoneChar) {
    // Geçerli tahta durumunu al
    char[][] currentBoard = getBoard();
    
    // Geçici bir tahta oluştur (mevcut durumu kopyala)
    char[][] tempBoard = new char[currentBoard.length][currentBoard[0].length];
    for (int y = 0; y < currentBoard.length; y++) {
        for (int x = 0; x < currentBoard[y].length; x++) {
            tempBoard[y][x] = currentBoard[y][x];
        }
    }
    
    // Taşı geçici olarak yerleştir
    tempBoard[point.y()][point.x()] = stoneChar;
    
    // Taşın etrafındaki özgürlükleri kontrol et
    return !hasLiberties(tempBoard, point.x(), point.y(), stoneChar);
}
    
    /**
     * Son hamleyi belirler ve vurgular.
     *
     * @param p Son hamle noktası
     */
    public void setLastMove(Point p) {
        this.lastMove = p;
        repaint();
    }

    /**
     * Taş yerleştirme animasyonu uygular.
     *
     * @param p Taşın konumu
     * @param stoneType Taşın tipi ('B' veya 'W')
     */
    public void animateStonePlace(Point p, char stoneType) {
        if (p == null || !isValidPoint(p)) {
            return;
        }

        // Animasyon noktasını ayarla
        animatedStone = p;
        animationProgress = 0f;
        animationType = 1; // 1: Yerleştirme animasyonu

        // Ses efekti
        if (soundEnabled) {
            playSound("stone_place");
        }

        // Animasyonu başlat
        animationTimer.start();
    }

    /**
     * Taş yakalama (esir alma) animasyonu uygular.
     *
     * @param p Taşın konumu
     * @param stoneType Taşın tipi ('B' veya 'W')
     */
    public void animateStoneCapture(Point p, char stoneType) {
        if (p == null || !isValidPoint(p)) {
            return;
        }

        // Animasyon noktasını ayarla
        animatedStone = p;
        animationProgress = 0f;
        animationType = 2; // 2: Yakalama animasyonu

        // Ses efekti
        if (soundEnabled) {
            playSound("stone_capture");
        }

        // Animasyonu başlat
        captureAnimationTimer.start();
    }

    /**
     * Son hamleyi vurgular ve animasyon uygular.
     *
     * @param p Son hamle noktası
     */
    public void highlightLastMove(Point p) {
        lastMove = p;
        repaint();
    }

    /**
     * Bir noktanın tahtada geçerli olup olmadığını kontrol eder.
     *
     * @param p Kontrol edilecek nokta
     * @return Nokta geçerliyse true
     */
    private boolean isValidPoint(Point p) {
        return p.x() >= 0 && p.x() < boardSize && p.y() >= 0 && p.y() < boardSize;
    }

    /**
     * Ses efekti çalma işlemi
     *
     * @param soundName Çalınacak ses adı
     */
    private void playSound(String soundName) {
        // SoundEffects sınıfına bağlan veya dahili bir ses sistemi kullan
        try {
            SoundEffects.play(soundName);
        } catch (Exception e) {
            // Ses çalınamazsa sessizce devam et
        }
    }

    /**
     * Fare imleci ile taş önizlemesi için renk tipini ayarlar.
     *
     * @param playerColor Oyuncu taş rengi ('B' siyah, 'W' beyaz)
     */
    public void addHoverEffect(char playerColor) {
        // Oyuncu rengine göre önizleme rengini ayarla
        hoverColor = playerColor == 'B'
                ? new Color(0, 0, 0, 80) : new Color(255, 255, 255, 150);

        // Fare hareketi dinleyicisi ekle
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                Point p = getBoardCoordinates(e.getX(), e.getY());
                if (p != null) {
                    hoverPoint = p;
                    repaint();
                } else if (hoverPoint != null) {
                    hoverPoint = null;
                    repaint();
                }
            }
        });

        // Fare çıkış olayında önizlemeyi temizle
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverPoint = null;
                repaint();
            }
        });
    }

    /**
     * Hücre boyutunu döndürür.
     *
     * @return Piksel olarak hücre boyutu
     */
    public int getCellSize() {
        return cellSize;
    }

    /**
     * Tahtanın X başlangıç koordinatını döndürür.
     *
     * @return Piksel olarak X koordinatı
     */
    public int getBoardX() {
        return boardX;
    }

    /**
     * Tahtanın Y başlangıç koordinatını döndürür.
     *
     * @return Piksel olarak Y koordinatı
     */
    public int getBoardY() {
        return boardY;
    }

    /**
     * Tahtanın kenarını vurgular ya da normal haline döndürür
     *
     * @param highlight Kenarı vurgula (true) veya normal haline döndür (false)
     */
    public void highlightBorder(boolean highlight) {
        if (highlight) {
            // Parlak yeşil kenar
            this.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 0), 3));
        } else {
            // Normal duruma dön (kenar yok)
            this.setBorder(null);
        }

        // Hemen yeniden çiz
        this.repaint();
    }

    /**
     * Animasyonları durdurur ve kaynakları temizler
     */
    public void stopAllAnimations() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        if (captureAnimationTimer != null && captureAnimationTimer.isRunning()) {
            captureAnimationTimer.stop();
        }

        if (blinkTimer != null && blinkTimer.isRunning()) {
            blinkTimer.stop();
        }

        if (blinkStopTimer != null && blinkStopTimer.isRunning()) {
            blinkStopTimer.stop();
        }

        animatedStone = null;
    }
}

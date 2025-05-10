package client.ui;

import game.go.model.Point;
import game.go.model.Stone;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 * Go tahtası görselleştirme paneli
 * İyileştirilmiş görünüm ve kullanıcı deneyimi özellikleri içerir
 */
public class GoBoardPanel extends JPanel {
    
    // Logger
    private static final Logger LOGGER = Logger.getLogger(GoBoardPanel.class.getName());
    
    // Sabitler
    private static final Color BOARD_COLOR = new Color(220, 179, 92); // Bambu rengi
    private static final Color GRID_COLOR = Color.BLACK;
    private static final Color LAST_MOVE_INDICATOR = new Color(255, 0, 0, 150);
    private static final int MARGIN = 30; // Kenar boşluğu
    private static final int MIN_CELL_SIZE = 30; // Minimum hücre boyutu
    
    // Tahta durumu
    private char[][] board;
    private int boardSize = 19; // Varsayılan tahta boyutu
    private Point lastMove = null;
    private String role = ""; // Oyuncunun rolü (BLACK veya WHITE)
    
    // UI değişkenleri
    private int cellSize; // Her hücrenin boyutu
    private int stoneSize; // Taş boyutu (cellSize'ın yüzdesi)
    private Point hoverPoint = null; // Fare ile üzerinde durulan konum
    
    // Olay işleyicileri için callback
    private MoveListener moveListener;
    
    /**
     * Hamle olayı dinleyici arayüzü
     */
    public interface MoveListener {
        void onMove(Point p);
    }

    /**
     * Panel oluşturur ve fare olaylarını dinler
     */
    public GoBoardPanel() {
        board = new char[boardSize][boardSize];
        clearBoard();
        
        setPreferredSize(new Dimension(600, 600));
        setBackground(BOARD_COLOR);
        
        // Fare olayları
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverPoint(e.getX(), e.getY());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hoverPoint = null;
                repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (hoverPoint != null && isValidMove(hoverPoint)) {
                    if (moveListener != null) {
                        moveListener.onMove(hoverPoint);
                    }
                }
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    /**
     * Tahtayı temizler (tüm hücreleri boşaltır)
     */
    public void clearBoard() {
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                board[y][x] = '.';
            }
        }
        repaint();
    }
    
    /**
     * Tahta durumunu günceller
     * 
     * @param newBoard Yeni tahta durumu
     */
    public void setBoard(char[][] newBoard) {
        if (newBoard == null) {
            LOGGER.warning("Null board passed to setBoard");
            return;
        }
        
        this.boardSize = newBoard.length;
        this.board = new char[boardSize][boardSize];
        
        for (int y = 0; y < boardSize; y++) {
            if (y < newBoard.length && newBoard[y] != null) {
                System.arraycopy(newBoard[y], 0, board[y], 0, Math.min(boardSize, newBoard[y].length));
                
                // Fill remaining cells if source row is shorter
                for (int x = newBoard[y].length; x < boardSize; x++) {
                    board[y][x] = '.';
                }
            } else {
                // Fill empty rows
                for (int x = 0; x < boardSize; x++) {
                    board[y][x] = '.';
                }
            }
        }
        repaint();
    }
    
    /**
     * Son hamleyi ayarlar (kırmızı işaretçi için)
     * 
     * @param p Son hamle noktası
     */
    public void setLastMove(Point p) {
        this.lastMove = p;
        repaint();
    }
    
    /**
     * Oyuncunun rolünü ayarlar (BLACK veya WHITE)
     * 
     * @param role Oyuncu rolü
     */
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * Hamle dinleyici ayarlar
     * 
     * @param listener Hamle dinleyici
     */
    public void setMoveListener(MoveListener listener) {
        this.moveListener = listener;
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
     * Hücre boyutunu döndürür
     * 
     * @return Hücre boyutu
     */
    public int getCellSize() {
        return cellSize;
    }
    
    /**
     * Fare konumundan tahta koordinatlarını hesaplar
     * 
     * @param mouseX Fare X koordinatı
     * @param mouseY Fare Y koordinatı
     */
    private void updateHoverPoint(int mouseX, int mouseY) {
        int gridX = (mouseX - MARGIN + cellSize/2) / cellSize;
        int gridY = (mouseY - MARGIN + cellSize/2) / cellSize;
        
        Point newHover = null;
        if (gridX >= 0 && gridX < boardSize && gridY >= 0 && gridY < boardSize) {
            newHover = new Point(gridX, gridY);
        }
        
        if ((hoverPoint == null && newHover != null) || 
            (hoverPoint != null && !hoverPoint.equals(newHover))) {
            hoverPoint = newHover;
            repaint();
        }
    }
    
    /**
     * Bir noktada hamle yapılabilir mi kontrol eder
     * 
     * @param p Kontrol edilecek nokta
     * @return Hamle yapılabilirse true
     */
    private boolean isValidMove(Point p) {
        if (p == null || p.x() < 0 || p.x() >= boardSize || p.y() < 0 || p.y() >= boardSize) {
            return false;
        }
        return board[p.y()][p.x()] == '.';
    }
    
    /**
     * Bir noktanın star point olup olmadığını kontrol eder
     * 
     * @param x X koordinatı
     * @param y Y koordinatı
     * @return Star point ise true
     */
    private boolean isStarPoint(int x, int y) {
        if (boardSize == 19) {
            // 19x19 tahta için star points
            int[] stars = {3, 9, 15};
            for (int i : stars) {
                for (int j : stars) {
                    if (x == i && y == j) return true;
                }
            }
        } else if (boardSize == 13) {
            // 13x13 tahta için star points
            int[] stars = {3, 6, 9};
            for (int i : stars) {
                for (int j : stars) {
                    if (x == i && y == j) return true;
                }
            }
        } else if (boardSize == 9) {
            // 9x9 tahta için star points
            int[] stars = {2, 4, 6};
            for (int i : stars) {
                for (int j : stars) {
                    if (x == i && y == j) return true;
                }
            }
            return (x == 4 && y == 4); // Ortadaki star point
        }
        return false;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int boardDim = Math.min(width, height) - 2 * MARGIN;
        cellSize = Math.max(boardDim / (boardSize - 1), MIN_CELL_SIZE);
        stoneSize = (int)(cellSize * 0.9);
        
        // Tahta arka planı
        g2.setColor(BOARD_COLOR);
        g2.fillRect(0, 0, width, height);
        
        // Grid çizgileri
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(1.0f));
        
        for (int i = 0; i < boardSize; i++) {
            // Yatay çizgiler
            g2.drawLine(
                MARGIN, 
                MARGIN + i * cellSize, 
                MARGIN + (boardSize - 1) * cellSize, 
                MARGIN + i * cellSize
            );
            
            // Dikey çizgiler
            g2.drawLine(
                MARGIN + i * cellSize, 
                MARGIN, 
                MARGIN + i * cellSize, 
                MARGIN + (boardSize - 1) * cellSize
            );
        }
        
        // Star noktaları (hoshi)
        g2.setColor(Color.BLACK);
        int starSize = 6;
        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                if (isStarPoint(x, y)) {
                    g2.fillOval(
                        MARGIN + x * cellSize - starSize/2, 
                        MARGIN + y * cellSize - starSize/2, 
                        starSize, 
                        starSize
                    );
                }
            }
        }
        
        // Koordinat etiketleri
        drawCoordinates(g2);
        
        // Taşları çiz
        drawStones(g2);
        
        // Son hamle göstergesi
        if (lastMove != null) {
            g2.setColor(LAST_MOVE_INDICATOR);
            g2.drawOval(
                MARGIN + lastMove.x() * cellSize - stoneSize/4, 
                MARGIN + lastMove.y() * cellSize - stoneSize/4, 
                stoneSize/2, 
                stoneSize/2
            );
        }
        
        // Hover efekti
        if (hoverPoint != null && isValidMove(hoverPoint)) {
            Stone hoverStone = role.equals("BLACK") ? Stone.BLACK : Stone.WHITE;
            float alpha = 0.5f;
            drawStone(g2, hoverPoint.x(), hoverPoint.y(), hoverStone, alpha);
        }
        
        g2.dispose();
    }
    
    /**
     * Koordinat etiketlerini çizer (A-T, 1-19)
     * 
     * @param g2 Graphics2D nesnesi
     */
    private void drawCoordinates(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        
        // Yatay koordinatlar (A-T, I hariç)
        for (int i = 0; i < boardSize; i++) {
            char label = (char)('A' + (i >= 8 ? i + 1 : i)); // I harfini atla
            String s = Character.toString(label);
            int textWidth = fm.stringWidth(s);
            
            // Üst etiketler
            g2.drawString(s, MARGIN + i * cellSize - textWidth/2, MARGIN - 10);
            
            // Alt etiketler
            g2.drawString(s, MARGIN + i * cellSize - textWidth/2, 
                          MARGIN + (boardSize - 1) * cellSize + 20);
        }
        
        // Dikey koordinatlar (1-19)
        for (int i = 0; i < boardSize; i++) {
            String s = Integer.toString(boardSize - i);
            int textWidth = fm.stringWidth(s);
            
            // Sol etiketler
            g2.drawString(s, MARGIN - 15 - textWidth, MARGIN + i * cellSize + 5);
            
            // Sağ etiketler
            g2.drawString(s, MARGIN + (boardSize - 1) * cellSize + 15, 
                          MARGIN + i * cellSize + 5);
        }
    }
    
    /**
     * Tüm taşları çizer
     * 
     * @param g2 Graphics2D nesnesi
     */
    private void drawStones(Graphics2D g2) {
        if (board == null) return;
        
        for (int y = 0; y < boardSize; y++) {
            if (y >= board.length) continue;
            
            for (int x = 0; x < boardSize; x++) {
                if (x >= board[y].length) continue;
                
                Stone stone = null;
                if (board[y][x] == 'B') {
                    stone = Stone.BLACK;
                } else if (board[y][x] == 'W') {
                    stone = Stone.WHITE;
                }
                
                if (stone != null) {
                    drawStone(g2, x, y, stone, 1.0f);
                }
            }
        }
    }
    
    /**
     * Tek bir taşı çizer
     * 
     * @param g2 Graphics2D nesnesi
     * @param x X koordinatı
     * @param y Y koordinatı
     * @param stone Taş rengi
     * @param alpha Opaklık (0.0-1.0)
     */
    private void drawStone(Graphics2D g2, int x, int y, Stone stone, float alpha) {
        int centerX = MARGIN + x * cellSize;
        int centerY = MARGIN + y * cellSize;
        
        if (stone == Stone.BLACK) {
            // Siyah taş (gradient efekt ile)
            Color darkBlack = new Color(0, 0, 0, (int)(255 * alpha));
            Color lightBlack = new Color(80, 80, 80, (int)(255 * alpha));
            
            GradientPaint gp = new GradientPaint(
                centerX - stoneSize/4, centerY - stoneSize/4, 
                lightBlack, 
                centerX + stoneSize/4, centerY + stoneSize/4, 
                darkBlack
            );
            
            g2.setPaint(gp);
            g2.fill(new Ellipse2D.Double(
                centerX - stoneSize/2, 
                centerY - stoneSize/2, 
                stoneSize, 
                stoneSize
            ));
            
        } else if (stone == Stone.WHITE) {
            // Beyaz taş (gradient efekt ile)
            Color lightWhite = new Color(255, 255, 255, (int)(255 * alpha));
            Color shellWhite = new Color(220, 220, 210, (int)(255 * alpha));
            
            GradientPaint gp = new GradientPaint(
                centerX - stoneSize/3, centerY - stoneSize/3, 
                lightWhite, 
                centerX + stoneSize/3, centerY + stoneSize/3, 
                shellWhite
            );
            
            g2.setPaint(gp);
            g2.fill(new Ellipse2D.Double(
                centerX - stoneSize/2, 
                centerY - stoneSize/2, 
                stoneSize, 
                stoneSize
            ));
            
            // Beyaz taşın kenar çizgisi
            g2.setColor(new Color(160, 160, 160, (int)(180 * alpha)));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new Ellipse2D.Double(
                centerX - stoneSize/2, 
                centerY - stoneSize/2, 
                stoneSize, 
                stoneSize
            ));
        }
    }
}
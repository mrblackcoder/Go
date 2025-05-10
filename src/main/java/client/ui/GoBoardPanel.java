package client.ui;

import game.go.model.Point;
import java.awt.*;
import javax.swing.JPanel;

/**
 * Go board panel for displaying the game board and handling user interactions.
 */
public class GoBoardPanel extends JPanel {
    private static final int MIN_CELL_SIZE = 20; // Minimum cell size in pixels
    private static final int DEFAULT_BOARD_SIZE = 19; // Default Go board size
    private static final int BOARD_MARGIN = 30; // Margin around the board for coordinates
    
    private final Color BOARD_COLOR = new Color(219, 176, 102); // Traditional Go board color
    private final Color LINE_COLOR = Color.BLACK;
    private final Color BLACK_STONE_COLOR = Color.BLACK;
    private final Color WHITE_STONE_COLOR = Color.WHITE;
    private final Color LAST_MOVE_COLOR = new Color(255, 0, 0, 128); // Semi-transparent red
    
    private int boardSize = DEFAULT_BOARD_SIZE;
    private int cellSize = MIN_CELL_SIZE;
    private int boardX = BOARD_MARGIN;
    private int boardY = BOARD_MARGIN;
    private int boardWidth;
    private int boardHeight;
    
    private char[][] board = new char[DEFAULT_BOARD_SIZE][DEFAULT_BOARD_SIZE];
    private Point lastMove = null;
    private Point hoverPoint = null;
    private Color hoverColor = new Color(0, 0, 0, 64); // Semi-transparent black
    
    /**
     * Creates a new Go board panel.
     */
    public GoBoardPanel() {
        setBackground(new Color(240, 230, 200));
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j] = '.';
            }
        }
        // Force initial sizing
        calculateBoardDimensions();
    }
    
    /**
     * Calculate board dimensions based on current component size.
     */
    private void calculateBoardDimensions() {
        // Calculate board dimensions based on current component size
        Dimension size = getSize();
        if (size.width == 0 || size.height == 0) {
            // Use default sizes if component hasn't been sized yet
            size = new Dimension(600, 600);
        }
        
        // Calculate cell size to fit within the component
        int availableWidth = size.width - 2 * BOARD_MARGIN;
        int availableHeight = size.height - 2 * BOARD_MARGIN;
        int maxCellSize = Math.min(availableWidth, availableHeight) / boardSize;
        cellSize = Math.max(MIN_CELL_SIZE, maxCellSize);
        
        // Calculate board dimensions
        boardWidth = boardSize * cellSize;
        boardHeight = boardSize * cellSize;
        
        // Center the board within the component
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
        
        // Draw hover highlight
        drawHoverHighlight(g2d);
        
        // Highlight last move
        highlightLastMove(g2d);
    }
    
    /**
     * Draw the board grid and background.
     */
    private void drawBoard(Graphics2D g2d) {
        // Draw board background
        g2d.setColor(BOARD_COLOR);
        g2d.fillRect(boardX, boardY, boardWidth, boardHeight);
        
        // Draw grid lines
        g2d.setColor(LINE_COLOR);
        for (int i = 0; i < boardSize; i++) {
            // Draw horizontal lines
            g2d.drawLine(boardX, boardY + i * cellSize, 
                         boardX + boardWidth, boardY + i * cellSize);
            
            // Draw vertical lines
            g2d.drawLine(boardX + i * cellSize, boardY, 
                         boardX + i * cellSize, boardY + boardHeight);
        }
        
        // Draw star points (hoshi)
        drawStarPoints(g2d);
    }
    
    /**
     * Draw the star points (hoshi) on the board.
     */
    private void drawStarPoints(Graphics2D g2d) {
        g2d.setColor(LINE_COLOR);
        
        // Define star points based on board size
        int[] starPoints;
        if (boardSize == 19) {
            starPoints = new int[]{3, 9, 15};
        } else if (boardSize == 13) {
            starPoints = new int[]{3, 6, 9};
        } else if (boardSize == 9) {
            starPoints = new int[]{2, 4, 6};
        } else {
            return; // No star points for other board sizes
        }
        
        // Draw star points
        int dotSize = Math.max(4, cellSize / 6);
        for (int x : starPoints) {
            for (int y : starPoints) {
                g2d.fillOval(boardX + x * cellSize - dotSize/2, 
                             boardY + y * cellSize - dotSize/2, 
                             dotSize, dotSize);
            }
        }
    }
    
    /**
     * Draw the coordinate labels around the board.
     */
    private void drawCoordinates(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        Font coordFont = new Font("SansSerif", Font.BOLD, Math.max(10, cellSize / 3));
        g2d.setFont(coordFont);
        
        // Skip "I" as per traditional Go notation
        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'};
        
        // Draw letter coordinates for columns
        for (int i = 0; i < Math.min(boardSize, letters.length); i++) {
            String colLabel = String.valueOf(letters[i]);
            
            // Draw column letter at top
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(colLabel);
            g2d.drawString(colLabel, 
                       boardX + i * cellSize + (cellSize - labelWidth) / 2, 
                       boardY - 5);
            
            // Draw column letter at bottom
            g2d.drawString(colLabel,
                       boardX + i * cellSize + (cellSize - labelWidth) / 2,
                       boardY + boardHeight + fm.getAscent() + 2);
        }
        
        // Draw number coordinates for rows (from top to bottom: 19, 18, ..., 1)
        for (int i = 0; i < boardSize; i++) {
            String rowLabel = String.valueOf(boardSize - i);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(rowLabel);
            int labelHeight = fm.getAscent();
            
            // Draw row number at left
            g2d.drawString(rowLabel,
                       boardX - labelWidth - 5,
                       boardY + i * cellSize + (cellSize + labelHeight) / 2 - 2);
            
            // Draw row number at right
            g2d.drawString(rowLabel,
                       boardX + boardWidth + 5,
                       boardY + i * cellSize + (cellSize + labelHeight) / 2 - 2);
        }
    }
    
    /**
     * Draw all stones on the board.
     */
    private void drawStones(Graphics2D g2d) {
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                char stone = board[y][x];
                if (stone == '.') {
                    continue; // Empty intersection
                }
                
                int stoneSize = (int)(cellSize * 0.9); // Slightly smaller than cell
                int x1 = boardX + x * cellSize - stoneSize / 2;
                int y1 = boardY + y * cellSize - stoneSize / 2;
                
                if (stone == 'B' || stone == 'b') {
                    // Draw black stone
                    g2d.setColor(BLACK_STONE_COLOR);
                    g2d.fillOval(x1, y1, stoneSize, stoneSize);
                    
                    // If it's a dead stone (lowercase), add an 'X' mark
                    if (stone == 'b') {
                        g2d.setColor(Color.WHITE);
                        g2d.setStroke(new BasicStroke(2));
                        int margin = stoneSize / 4;
                        g2d.drawLine(x1 + margin, y1 + margin, x1 + stoneSize - margin, y1 + stoneSize - margin);
                        g2d.drawLine(x1 + stoneSize - margin, y1 + margin, x1 + margin, y1 + stoneSize - margin);
                    }
                } else if (stone == 'W' || stone == 'w') {
                    // Draw white stone with black outline
                    g2d.setColor(WHITE_STONE_COLOR);
                    g2d.fillOval(x1, y1, stoneSize, stoneSize);
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval(x1, y1, stoneSize, stoneSize);
                    
                    // If it's a dead stone (lowercase), add an 'X' mark
                    if (stone == 'w') {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(2));
                        int margin = stoneSize / 4;
                        g2d.drawLine(x1 + margin, y1 + margin, x1 + stoneSize - margin, y1 + stoneSize - margin);
                        g2d.drawLine(x1 + stoneSize - margin, y1 + margin, x1 + margin, y1 + stoneSize - margin);
                    }
                }
            }
        }
    }
    
    /**
     * Draw a highlight around the last move.
     */
    private void highlightLastMove(Graphics2D g2d) {
        if (lastMove != null) {
            int x = lastMove.x();
            int y = lastMove.y();
            
            // Draw a red circle around the last move
            int highlightSize = (int)(cellSize * 0.7);
            int x1 = boardX + x * cellSize - highlightSize / 2;
            int y1 = boardY + y * cellSize - highlightSize / 2;
            
            g2d.setColor(LAST_MOVE_COLOR);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(x1, y1, highlightSize, highlightSize);
        }
    }
    
    /**
     * Draw a semi-transparent stone at hover position.
     */
    private void drawHoverHighlight(Graphics2D g2d) {
        if (hoverPoint != null) {
            int x = hoverPoint.x();
            int y = hoverPoint.y();
            
            if (y < boardSize && x < boardSize && board[y][x] == '.') { // Only show if intersection is empty
                int stoneSize = (int)(cellSize * 0.9);
                int x1 = boardX + x * cellSize - stoneSize / 2;
                int y1 = boardY + y * cellSize - stoneSize / 2;
                
                // Draw semi-transparent stone
                g2d.setColor(hoverColor);
                g2d.fillOval(x1, y1, stoneSize, stoneSize);
                
                // If hover stone is white, add a thin border
                if (hoverColor.getRed() > 200) { // Rough check if it's white-ish
                    g2d.setColor(new Color(0, 0, 0, 128));
                    g2d.drawOval(x1, y1, stoneSize, stoneSize);
                }
            }
        }
    }
    
    /**
     * Convert screen coordinates to board coordinates.
     */
    public Point getBoardCoordinates(int screenX, int screenY) {
        int x = (screenX - boardX + cellSize/2) / cellSize;
        int y = (screenY - boardY + cellSize/2) / cellSize;
        
        if (x >= 0 && x < boardSize && y >= 0 && y < boardSize) {
            return new Point(x, y);
        }
        return null; // Outside the board
    }
    
    /**
     * Set the board data.
     */
    public void setBoard(char[][] boardData) {
        if (boardData == null || boardData.length == 0) {
            return;
        }
        
        // Update board size if necessary
        if (boardData.length != boardSize) {
            boardSize = boardData.length;
            board = new char[boardSize][boardSize];
            calculateBoardDimensions();
        }
        
        // Copy the board data
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
     * Set the last move.
     */
    public void setLastMove(Point p) {
        this.lastMove = p;
        repaint();
    }
    
    /**
     * Add hover effect for the given player color.
     */
    public void addHoverEffect(char playerColor) {
        // Set hover color based on player color
        hoverColor = playerColor == 'B' ? 
                     new Color(0, 0, 0, 64) : new Color(255, 255, 255, 128);
        
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
        
        // Clear hover when mouse exits the component
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverPoint = null;
                repaint();
            }
        });
    }
    
    // Getters
    public int getBoardSize() {
        return boardSize;
    }
    
    public int getCellSize() {
        return cellSize;
    }
    
    public int getBoardX() {
        return boardX;
    }
    
    public int getBoardY() {
        return boardY;
    }
}
package client;

import common.IOUtil;
import common.Message;
import game.go.model.Stone;
import game.go.model.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Client connection: connects to server and handles messaging
 */
public class CClient extends Thread {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final MainFrm ui; // Optional (can be null)
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Logger LOGGER = Logger.getLogger(CClient.class.getName());

    /**
     * Creates a new client connection.
     *
     * @param host Server address
     * @param port Server port
     * @param ui UI reference (optional, can be null)
     * @throws IOException If connection fails
     */
    public CClient(String host, int port, MainFrm ui) throws IOException {
        this.socket = new Socket(host, port);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.ui = ui;
        LOGGER.log(Level.INFO, "Connected to {0}:{1}", new Object[]{host, port});
    }

    /**
     * Sends a message to the server.
     *
     * @param msg Message to send
     */
    public void send(Message msg) {
        try {
            if (socket == null || socket.isClosed() || !socket.isConnected()) {
                LOGGER.warning("Cannot send message: connection is closed");
                if (ui != null) {
                    SwingUtilities.invokeLater(() -> ui.showConnectionError("Connection is closed"));
                }
                return;
            }
            
            IOUtil.writeMessage(out, msg);
            LOGGER.log(Level.INFO, "Sent: {0}#{1}", new Object[]{msg.type(), msg.payload()});
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending message", e);
            if (ui != null) {
                SwingUtilities.invokeLater(() -> ui.showConnectionError("Failed to send message: " + e.getMessage()));
            }
        }
    }

    /**
     * Listens for and processes messages from the server.
     */
    @Override
    public void run() {
        try {
            while (running.get()) {
                Message msg = IOUtil.readMessage(in);
                if (msg == null) {
                    LOGGER.info("Connection closed by server");
                    break;
                }

                LOGGER.log(Level.INFO, "Received: {0}#{1}", new Object[]{msg.type(), msg.payload()});

                // Process message
                processMessage(msg);
            }
        } catch (IOException e) {
            if (running.get()) {
                LOGGER.log(Level.WARNING, "Connection error", e);
                if (ui != null) {
                    SwingUtilities.invokeLater(() -> ui.showConnectionError("Connection error: " + e.getMessage()));
                }
            }
        } finally {
            close();
        }
    }

    /**
     * Processes incoming messages and updates the UI.
     *
     * @param msg Message to process
     */
    private void processMessage(Message msg) {
        // If no UI, just log messages
        if (ui == null) {
            System.out.printf("RECV ▶ %s#%s%n", msg.type(), msg.payload());
            return;
        }

        if (msg == null || msg.type() == null) {
            LOGGER.warning("Received null message or message type");
            return;
        }

        try {
            switch (msg.type()) {
                case ROLE:
                    // Role info: BLACK or WHITE
                    ui.setRole(msg.payload());
                    break;

                case BOARD_STATE:
                    // Board state: in JSON format
                    processBoardState(msg.payload());
                    break;

                case SCORE:
                    // Score info: "me,opponent,turn" format
                    processScore(msg.payload());
                    break;

                case MSG_FROM_CLIENT:
                    // Chat message
                    ui.showChat(msg.payload());
                    break;

                case GAME_OVER:
                    // Game over: "myScore,oppScore,reason" format
                    processGameOver(msg.payload());
                    break;

                case ERROR:
                    // Error message
                    showError(msg.payload());
                    break;

                default:
                    // Other messages
                    LOGGER.log(Level.INFO, "Unhandled message type: {0}", msg.type());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing message: " + msg.type(), e);
            if (ui != null) {
                SwingUtilities.invokeLater(() -> ui.showError("Error processing message: " + e.getMessage()));
            }
        }
    }

    /**
     * Processes board state and updates the UI.
     * Uses a simpler but robust parsing approach that doesn't rely on external JSON libraries
     *
     * @param jsonBoard Board data in JSON format
     */
    private void processBoardState(String jsonBoard) {
        if (ui == null || jsonBoard == null || jsonBoard.isEmpty()) {
            return;
        }

        try {
            // Parse JSON string: [[".",".","."],[".","B","."],[".","W","."]]
            char[][] board = parseJsonBoardManually(jsonBoard);
            if (board != null) {
                // Update UI board
                SwingUtilities.invokeLater(() -> ui.getBoard().setBoard(board));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing board state: " + jsonBoard, e);
            showError("Failed to process board state: " + e.getMessage());
        }
    }

    /**
     * Manually parses JSON format board data to char[][] array without external libraries.
     * 
     * This method handles the specific board format: [[".",".","."],[".","B","."],[".","W","."]]
     * 
     * @param json Board data in JSON format
     * @return Board as character array
     */
    private char[][] parseJsonBoardManually(String json) {
        if (json == null || json.isEmpty()) {
            LOGGER.warning("Empty JSON board data");
            return null;
        }
        
        json = json.trim();
        
        // Quick validation to ensure it's an array
        if (!json.startsWith("[") || !json.endsWith("]")) {
            LOGGER.warning("Invalid JSON board format: not an array");
            return null;
        }
        
        // Remove outer brackets and split into rows
        String content = json.substring(1, json.length() - 1).trim();
        
        // Split the content into rows by finding complete [...] sections
        List<String> rowStrings = new ArrayList<>();
        int depth = 0;
        StringBuilder currentRow = new StringBuilder();
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '[') {
                depth++;
                currentRow.append(c);
            } else if (c == ']') {
                depth--;
                currentRow.append(c);
                
                if (depth == 0) {
                    rowStrings.add(currentRow.toString());
                    currentRow = new StringBuilder();
                    
                    // Skip comma and whitespace after row
                    while (i + 1 < content.length() && (content.charAt(i + 1) == ',' || Character.isWhitespace(content.charAt(i + 1)))) {
                        i++;
                    }
                }
            } else if (depth > 0) {
                currentRow.append(c);
            }
        }
        
        int size = rowStrings.size();
        if (size == 0) {
            LOGGER.warning("Empty board data");
            return null;
        }
        
        char[][] board = new char[size][size];
        
        // Process each row
        for (int y = 0; y < size; y++) {
            String rowStr = rowStrings.get(y);
            
            // Remove brackets from row
            if (rowStr.startsWith("[") && rowStr.endsWith("]")) {
                rowStr = rowStr.substring(1, rowStr.length() - 1);
            } else {
                LOGGER.warning("Invalid row format at row " + y);
                continue;
            }
            
            // Split by commas, handling quoted values
            List<String> cellValues = new ArrayList<>();
            StringBuilder currentCell = new StringBuilder();
            boolean inQuotes = false;
            
            for (int i = 0; i < rowStr.length(); i++) {
                char c = rowStr.charAt(i);
                
                if (c == '"') {
                    inQuotes = !inQuotes;
                    currentCell.append(c);
                } else if (c == ',' && !inQuotes) {
                    cellValues.add(currentCell.toString().trim());
                    currentCell = new StringBuilder();
                } else {
                    currentCell.append(c);
                }
            }
            
            // Add the last cell
            if (currentCell.length() > 0) {
                cellValues.add(currentCell.toString().trim());
            }
            
            // Process each cell in the row
            for (int x = 0; x < Math.min(size, cellValues.size()); x++) {
                String cellValue = cellValues.get(x).trim();
                
                // Remove quotes if present
                if (cellValue.startsWith("\"") && cellValue.endsWith("\"")) {
                    cellValue = cellValue.substring(1, cellValue.length() - 1);
                }
                
                // Set the cell value
                board[y][x] = cellValue.isEmpty() ? '.' : cellValue.charAt(0);
            }
            
            // Fill remaining cells if row is shorter than expected
            for (int x = cellValues.size(); x < size; x++) {
                board[y][x] = '.';
            }
        }
        
        return board;
    }

    /**
     * Processes score information and updates the UI.
     *
     * @param payload Score and turn info in "me,opponent,turn" format
     */
    private void processScore(String payload) {
        if (ui == null || payload == null) {
            return;
        }

        try {
            String[] parts = payload.split(",");
            if (parts.length < 2) {
                LOGGER.warning("Invalid score format: " + payload);
                return;
            }
            
            int myScore = Integer.parseInt(parts[0].trim());
            int oppScore = Integer.parseInt(parts[1].trim());
            String turn = parts.length > 2 ? parts[2].trim() : "";

            // Update UI
            ui.updateStatus(myScore, oppScore, turn);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error processing score: " + payload, e);
        }
    }

    /**
     * Processes game over state.
     *
     * @param payload Game result in "myScore,oppScore,reason" format
     */
    private void processGameOver(String payload) {
        if (ui == null || payload == null) {
            return;
        }

        try {
            String[] parts = payload.split(",", 3);
            if (parts.length < 2) {
                LOGGER.warning("Invalid game over format: " + payload);
                return;
            }
            
            int myScore = Integer.parseInt(parts[0].trim());
            int oppScore = Integer.parseInt(parts[1].trim());
            String reason = parts.length > 2 ? parts[2].trim() : "Game over";

            // Update score and show game over notification
            ui.updateStatus(myScore, oppScore, "");

            // Show game over dialog
            SwingUtilities.invokeLater(() -> {
                String result;
                if (myScore > oppScore) {
                    result = "You won!";
                } else if (myScore < oppScore) {
                    result = "You lost!";
                } else {
                    result = "Draw!";
                }

                ui.showGameOverDialog(result, myScore, oppScore, reason);
            });
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error processing game over: " + payload, e);
        }
    }

    /**
     * Shows error message.
     *
     * @param error Error message
     */
    private void showError(String error) {
        if (error == null) {
            error = "Unknown error";
        }
        
        if (ui == null) {
            System.err.println("ERROR: " + error);
            return;
        }

        final String errorMsg = error;
        SwingUtilities.invokeLater(() -> ui.showError(errorMsg));
    }

    /**
     * Closes the connection.
     */
    public void close() {
        if (!running.compareAndSet(true, false)) {
            // Already closed
            return;
        }
        
        LOGGER.info("Closing connection");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing socket", e);
        }
        
        if (ui != null) {
            SwingUtilities.invokeLater(() -> ui.handleDisconnect());
        }
    }
    
    /**
     * Checks if the connection is still active
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && running.get();
    }
}
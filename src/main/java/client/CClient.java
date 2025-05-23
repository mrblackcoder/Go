package client;

import common.IOUtil;
import common.Message;
import game.go.model.Stone;
import game.go.model.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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
    private final MainFrm ui; 
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Logger LOGGER = Logger.getLogger(CClient.class.getName());
    private static final int CONNECTION_TIMEOUT = 15000; 
    private String pendingGameOver = null;  
    private int lastBlackScore = 0;
    private int lastWhiteScore = 0;
    private boolean justMadeMove = false;

    /**
     * Creates a new client connection.
     *
     * @param host Server address
     * @param port Server port
     * @param ui UI reference (optional, can be null)
     * @throws IOException If connection fails
     */
    public CClient(String host, int port, MainFrm ui) throws IOException {
        try {
            System.out.println("Attempting connection to: " + host + ":" + port);

            // Print local IP address
            try {
                InetAddress localAddress = InetAddress.getLocalHost();
                System.out.println("Local IP address: " + localAddress.getHostAddress());
            } catch (Exception e) {
                System.out.println("Could not determine local IP address");
            }

            // DNS resolution check
            try {
                InetAddress serverAddress = InetAddress.getByName(host);
                System.out.println("Server IP resolved to: " + serverAddress.getHostAddress());
            } catch (UnknownHostException e) {
                System.out.println("WARNING: Could not resolve host: " + host);
            }

            // Create socket and connect
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            this.socket = s;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            this.ui = ui;
            LOGGER.log(Level.INFO, "Connected to {0}:{1}", new Object[]{host, port});
        } catch (SocketException se) {
            LOGGER.log(Level.SEVERE, "Connection error to " + host + ":" + port + ": " + se.getMessage(), se);
            throw new IOException("Connection failed: " + se.getMessage() + " (Host: " + host + ", Port: " + port + ")", se);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to " + host + ":" + port + ": " + e.getMessage(), e);
            throw e;
        }
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

  
    public void close() {
        if (!running.compareAndSet(true, false)) {
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
            if (ui.isNewGameInProgress()) {
                // Call new game reconnect if new game in progress
                SwingUtilities.invokeLater(() -> ui.handleNewGameReconnect());
            } else {
                // Normal disconnect handling
                SwingUtilities.invokeLater(() -> ui.handleDisconnect());
            }
        }
    }
    private boolean selfResigned = false;

    public boolean isSelfResigned() {
        return selfResigned;
    }

    public void markResigned() {
        this.selfResigned = true;
        LOGGER.log(Level.INFO, "Client marked as resigned");
    }

    public void setJustMadeMove(boolean value) {
        justMadeMove = value;
    }

    private void processRole(String payload) {
        if (payload == null || payload.isEmpty()) {
            System.err.println("Hata: Rol payload'u boş veya null.");
            return;
        }

        try {
            String role = payload.trim().toUpperCase();
            if (!role.equals("BLACK") && !role.equals("WHITE")) {
                System.err.println("Hata: Geçersiz rol: " + role);
                return;
            }

            SwingUtilities.invokeLater(() -> {
                ui.setRole(role); // MainFrm'deki setRole metodunu çağır
            });
        } catch (Exception e) {
            System.err.println("Hata: Rol işlenirken hata: " + e.getMessage());
        }
    }

    private boolean gameStarted = false;

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
        LOGGER.log(Level.INFO, "Game started flag set to: {0}", gameStarted);
    }

    /**
     * Skor bilgisini işler ve UI'ı günceller.
     *
     * @param payload Skor ve sıra bilgisi
     */
private void processScore(String payload) {
    if (payload == null || payload.isEmpty()) {
        System.err.println("Hata: Skor payload'u boş veya null.");
        return;
    }

    try {
        String[] parts = payload.split(",", 3);
        if (parts.length < 2) {
            System.err.println("Hata: Geçersiz skor formatı: " + payload);
            return;
        }

        int blackScore = Integer.parseInt(parts[0].trim());
        int whiteScore = Integer.parseInt(parts[1].trim());
        String turn = (parts.length > 2) ? parts[2].trim() : "";

        String myRole = ui.getRole();
        if (myRole == null || myRole.equalsIgnoreCase("Unknown")) {
            System.err.println("Hata: Rol bilinmiyor: " + myRole);
            return;
        }

        if (!gameStarted) {
            // Go oyununda başlangıçta hiçbir oyuncunun puanı olmamalı
            if (blackScore != 0 || whiteScore != 0) {
                LOGGER.log(Level.WARNING, "Anormal başlangıç puanları: siyah={0}, beyaz={1} - sıfırlanıyor", 
                          new Object[]{blackScore, whiteScore});
                blackScore = 0;
                whiteScore = 0;
            }
        }

        if (selfResigned) {
            // Rolüme göre kendi skorumu -1 (istifa) yap
            if (myRole.equalsIgnoreCase("BLACK")) {
                blackScore = -1;
            } else { // WHITE
                whiteScore = -1;
            }
        }

        int myScore, opponentScore;
        if (myRole.equalsIgnoreCase("BLACK")) {
            myScore = blackScore;
            opponentScore = whiteScore;
        } else { // WHITE
            myScore = whiteScore;
            opponentScore = blackScore;
        }

        final int finalMyScore = myScore;
        final int finalOpponentScore = opponentScore;
        SwingUtilities.invokeLater(() -> {
            ui.updateStatus(finalMyScore, finalOpponentScore, turn);
        });

    } catch (NumberFormatException e) {
        System.err.println("Hata: Skor sayıya çevrilemedi: " + payload);
    } catch (Exception e) {
        System.err.println("Hata: Skor işlenirken beklenmeyen hata: " + payload);
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
                    ui.setRole(msg.payload());

                    // Process pending GAME_OVER if it exists
                    if (pendingGameOver != null) {
                        handleGameOverPayload(pendingGameOver);
                        pendingGameOver = null;
                    }
                    break;

                case BOARD_STATE:
                    // Board state: in JSON format
                    processBoardState(msg.payload());
                    break;

                case SCORE:
                    // Score info: directly use format from server "myScore,oppScore,turn"
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

                case TIMER_UPDATE:
                    // Timer info: "myTime,opponentTime" format
                    processTimerUpdate(msg.payload());
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
     * Tahta durumunu işler ve UI'ı günceller. JSON formatındaki tahta verisini
     * parse eder ve tahtanın görsel temsilini günceller.
     *
     * @param jsonBoard JSON formatındaki tahta verisi
     */
    /**
     * Tahta durumunu işler ve ilk hamleleri tespit eder
     *
     * @param jsonBoard Board data in JSON format
     */
    /**
     * Tahta durumunu işler ve gerçek oyun başlangıcını tespit eder
     *
     * @param jsonBoard Board data in JSON format
     */
    private void processBoardState(String jsonBoard) {
        if (ui == null || jsonBoard == null || jsonBoard.isEmpty()) {
            return;
        }

        try {
            char[][] board = parseJsonBoardManually(jsonBoard);
            if (board != null) {
                // Tahtada taş olup olmadığını kontrol et
                boolean hasStones = false;
                int blackCount = 0;
                int whiteCount = 0;

                // Tahtadaki taşları say
                for (int y = 0; y < board.length; y++) {
                    for (int x = 0; x < board[y].length; x++) {
                        if (board[y][x] == 'B') {
                            hasStones = true;
                            blackCount++;
                        } else if (board[y][x] == 'W') {
                            hasStones = true;
                            whiteCount++;
                        }
                    }
                }

                if (hasStones && !gameStarted) {
                    gameStarted = true;
                    LOGGER.log(Level.INFO, "Tahtada taş tespit edildi - Oyun başlatılıyor. "
                            + "Siyah taş: {0}, Beyaz taş: {1}", new Object[]{blackCount, whiteCount});

                    if (ui != null) {
                        try {
                            ui.startGame();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "startGame çağrılamadı", e);
                        }
                    }
                }

                SwingUtilities.invokeLater(() -> ui.getBoard().setBoard(board));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Tahta durumu işlenirken hata: " + jsonBoard, e);
            showError("Tahta durumu işlenemedi: " + e.getMessage());
        }
    }

    /**
     * Manually parses JSON format board data to char[][] array without external
     * libraries.
     *
     * This method handles the specific board format:
     * [[".",".","."],[".","B","."],[".","W","."]]
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

    private void processGameOver(String payload) {
        if (ui == null || payload == null) {
            return;
        }

        // Save GAME_OVER payload if role not set yet
        if ("Unknown".equalsIgnoreCase(ui.getRole())) {
            pendingGameOver = payload;
            LOGGER.info("Role not set – deferring GAME_OVER payload");
            return;
        }

        handleGameOverPayload(payload);
    }

    /**
     * Processes timer update
     *
     * @param payload "myTime,opponentTime" format timer info
     */
    private void processTimerUpdate(String payload) {
        if (ui == null || payload == null) {
            return;
        }

        try {
            String[] parts = payload.split(",");
            if (parts.length < 2) {
                LOGGER.warning("Invalid timer format: " + payload);
                return;
            }

            String myTime = parts[0].trim();
            String opponentTime = parts[1].trim();

            // Update time labels in UI
            ui.updateTimers(myTime, opponentTime);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing timer update: " + payload, e);
        }
    }

    /**
     * Oyun sonu durumunu işler (istifa durumu eklendi)
     *
     * @param payload Oyun sonucu bilgisi
     */
    private void handleGameOverPayload(String payload) {
        if (ui == null || payload == null || payload.isEmpty()) {
            LOGGER.warning("handleGameOverPayload çağrıldı ancak UI null veya payload boş.");
            return;
        }
        LOGGER.fine("Oyun sonu işleniyor (handleGameOverPayload): " + payload);

        try {
            String[] parts = payload.split(",", 3);
            if (parts.length < 2) {
                LOGGER.warning("Geçersiz GAME_OVER formatı (handleGameOverPayload): " + payload);
                showError("Sunucudan geçersiz oyun sonu formatı alındı.");
                return;
            }

            int blackScore = Integer.parseInt(parts[0].trim());
            int whiteScore = Integer.parseInt(parts[1].trim());
            String reason = (parts.length > 2 && parts[2] != null) ? parts[2].trim() : "Oyun bitti";

            // İstifa durumunu kontrol et
            if (reason.toUpperCase().contains("RESIGN")) {
                // Kimin istifa ettiğini belirle
                boolean blackResigned = reason.toUpperCase().contains("BLACK");
                boolean whiteResigned = reason.toUpperCase().contains("WHITE");

                // İstifa eden oyuncunun skorunu -1 olarak ayarla
                if (blackResigned) {
                    blackScore = -1;
                } else if (whiteResigned) {
                    whiteScore = -1;
                } else if (selfResigned) {
                    // Kendi rolüme göre istifa etmiş olabilirim
                    if (ui.getRole().equalsIgnoreCase("BLACK")) {
                        blackScore = -1;
                    } else if (ui.getRole().equalsIgnoreCase("WHITE")) {
                        whiteScore = -1;
                    }
                }
            }
            // Sunucu bazen test amaçlı 0,0,SCORE gönderebilir, bunları yok sayalım
            if (blackScore == 0 && whiteScore == 0 && "SCORE".equalsIgnoreCase(reason)) {
                LOGGER.info("İçi boş GAME_OVER (0,0,SCORE) mesajı (handleGameOverPayload) yok sayılıyor.");
                return;
            }

            String myRole = ui.getRole();
            if (myRole == null || myRole.equalsIgnoreCase("Unknown") || myRole.equalsIgnoreCase("Connecting...")) {
                LOGGER.severe("KRİTİK HATA (handleGameOverPayload): Oyuncu rolü hala '" + myRole + "'! Payload: " + payload);
                showError("Kritik hata: Oyun sonu işlenirken oyuncu rolü belirlenemedi.");
                return;
            }

            int finalMyScore = 0;
            int finalOpponentScore = 0;

            if (myRole.equalsIgnoreCase("BLACK")) {
                finalMyScore = blackScore;
                finalOpponentScore = whiteScore;
            } else if (myRole.equalsIgnoreCase("WHITE")) {
                finalMyScore = whiteScore;
                finalOpponentScore = blackScore;
            } else {
                LOGGER.severe("handleGameOverPayload içinde geçersiz oyuncu rolü: '" + myRole + "'. Skorlar 0 olarak ayarlandı.");
                showError("İç hata: Oyuncu rolü geçersiz ('" + myRole + "'). Oyun sonu skorları güncellenemedi.");
                return;
            }

            // İstenen loglama formatı
            LOGGER.info("CClient (handleGameOverPayload) -> Rol: " + myRole + ", finalMyScore: " + finalMyScore
                    + ", finalOpponentScore: " + finalOpponentScore + ", reason: '" + reason
                    + "'. ui.updateStatus ve showGameOverDialog çağrılacak.");

            // UI'daki skorları ve durumu güncelle
            final int effectivelyFinalMyScore = finalMyScore;
            final int effectivelyFinalOpponentScore = finalOpponentScore;
            ui.updateStatus(effectivelyFinalMyScore, effectivelyFinalOpponentScore, "");

            // Oyun bitti diyaloğunu göster
            final String finalReasonForLambda = reason;
            SwingUtilities.invokeLater(() -> {
                ui.showGameOverDialog("", effectivelyFinalMyScore, effectivelyFinalOpponentScore, finalReasonForLambda);
            });

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "GAME_OVER mesajındaki (handleGameOverPayload) skorlar sayıya dönüştürülürken hata: " + payload, e);
            showError("Sunucudan gelen oyun sonu skor bilgisi sayıya dönüştürülemedi.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "GAME_OVER mesajı (handleGameOverPayload) işlenirken genel bir hata oluştu: " + payload, e);
            showError("Oyun sonu bilgileri işlenirken beklenmedik bir hata meydana geldi.");
        }
    }

// showError metodunun CClient içinde olduğundan emin olun:
    private void showError(String error) {
        if (error == null) {
            error = "Bilinmeyen bir hata oluştu.";
        }
        // LOGGER burada da kullanılabilir, ancak System.err genellikle konsolda hemen fark edilir.
        // UI varsa, kullanıcıya göstermek daha önemlidir.
        if (ui != null) {
            final String errorMsg = error; // Lambda için final kopya
            SwingUtilities.invokeLater(() -> ui.showError(errorMsg));
        } else {
            System.err.println("CLIENT HATA (UI YOK): " + error);
            LOGGER.severe("CLIENT HATA (UI YOK): " + error); // Ayrıca logla
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

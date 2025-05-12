package server;

import common.IOUtil;
import common.Message;
import game.go.model.GameState;
import game.go.model.Point;
import game.go.model.Stone;
import game.go.model.Board.MoveResult;
import game.go.util.GameTimer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameSession {

    private final SClient black, white;
    private final GameState state;
    private final Server server; // Server referansı eklendi
    private final static Logger LOGGER = Logger.getLogger(GameSession.class.getName());
    private boolean sessionActive = true; // Oturumun aktif olup olmadığını takip et
    
    // Süre sayaçları
    private final GameTimer blackTimer;
    private final GameTimer whiteTimer;
    private static final int DEFAULT_TIME_MINUTES = 30; // Varsayılan süre (dakika)
    private static final int TIMER_UPDATE_INTERVAL = 1000; // 1 saniye

    public GameSession(SClient black, SClient white, Server server) throws IOException {
        this.black = black;
        this.white = white;
        this.server = server;
        this.state = new GameState(19); // Tahta boyutu (örn: 19)
        
        // Zamanlayıcıları başlat
        this.blackTimer = new GameTimer(DEFAULT_TIME_MINUTES, null);
        this.whiteTimer = new GameTimer(DEFAULT_TIME_MINUTES, null);
        
        // Zaman bittiğinde oyunu bitirme aksiyonlarını ayarla
        this.blackTimer.setTimeoutAction(() -> {
            try {
                LOGGER.info("Black's time is up!");
                timeOut(Stone.BLACK);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error handling black timeout", e);
            }
        });
        
        this.whiteTimer.setTimeoutAction(() -> {
            try {
                LOGGER.info("White's time is up!");
                timeOut(Stone.WHITE);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error handling white timeout", e);
            }
        });

        black.bindSession(this);
        white.bindSession(this); // Bind before sending initial messages

        black.send(new Message(Message.Type.ROLE, "BLACK"));
        white.send(new Message(Message.Type.ROLE, "WHITE"));

        // Skor ve tahtayı başlangıçta gönder
        broadcastScore();
        broadcastBoard();
        
        // Zamanlayıcı durumunu gönder
        sendTimerStatus();
        
        // İlk oyuncunun süresini başlat (siyah)
        blackTimer.start();

        System.out.println("GameSession started between client " + black.id + " (BLACK) and " + white.id + " (WHITE)");
        LOGGER.log(Level.INFO, "GameSession started between client {0} (BLACK) and {1} (WHITE)", new Object[]{black.id, white.id});
        
        // Zamanlayıcı güncellemelerini başlat
        startTimerUpdates();
    }
    
    // Zamanı güncellemek için periyodik timer
    private void startTimerUpdates() {
        new Thread(() -> {
            while (sessionActive) {
                try {
                    sendTimerStatus();
                    Thread.sleep(TIMER_UPDATE_INTERVAL);
                } catch (InterruptedException | IOException e) {
                    LOGGER.log(Level.WARNING, "Timer update interrupted", e);
                    break;
                }
            }
        }).start();
    }
    
    // Zamanlayıcı durumunu istemcilere gönder
    private void sendTimerStatus() throws IOException {
        if (!sessionActive) return;
        
        String blackTime = blackTimer.getTimeText();
        String whiteTime = whiteTimer.getTimeText();
        
        // Siyah oyuncuya zaman bilgisini gönder
        sendToClient(black, new Message(Message.Type.TIMER_UPDATE, blackTime + "," + whiteTime), "timer to black");
        
        // Beyaz oyuncuya zaman bilgisini gönder
        sendToClient(white, new Message(Message.Type.TIMER_UPDATE, whiteTime + "," + blackTime), "timer to white");
    }
    
    // Süre bittiğinde çağrılacak metod
    private synchronized void timeOut(Stone player) throws IOException {
        if (!sessionActive || state.isOver()) return;
        
        // Oyunu bitir
        state.resign(); // İlgili oyuncu teslim olmuş sayılır
        
        String timeoutPlayer = (player == Stone.BLACK) ? "BLACK" : "WHITE";
        finish(timeoutPlayer + " süre dolduğu için oyunu kaybetti.");
    }
    
    public synchronized void handleMove(SClient from, String payload) throws IOException {
        if (!sessionActive || state.isOver()) { 
            handleInactiveSession(from, "Hamle"); 
            return; 
        }
        
        // Sıra kontrolü - doğru oyuncu mu hamle yapıyor?
        Stone fromColor = (from == black) ? Stone.BLACK : Stone.WHITE;
        if (fromColor != state.toPlay()) {
            LOGGER.log(Level.WARNING, "Client {0} tried to move out of turn", from.id);
            sendToClient(from, new Message(Message.Type.ERROR, "Hamle sırası sizde değil!"), "turn error");
            return;
        }
        
        try {
            // x,y formatındaki payload'ı Point'e çevir
            Point p = Point.fromCsv(payload);
            
            // Hamleyi yap
            MoveResult result = state.play(p);
            
            if (result.valid) {
                LOGGER.log(Level.INFO, "Client {0} moved to {1}", new Object[]{from.id, payload});
                
                // Hamle geçişinde, mevcut oyuncunun zamanını durdur ve diğer oyuncunun zamanını başlat
                if (fromColor == Stone.BLACK) {
                    blackTimer.stop();
                    whiteTimer.start();
                } else {
                    whiteTimer.stop();
                    blackTimer.start();
                }
                
                // Başarılı hamle sonrası tahta ve skor güncelleme
                broadcastBoard();
                broadcastScore();
                
                // Süre durumunu güncelle
                sendTimerStatus();
            } else {
                // Geçersiz hamle - hata mesajını sadece hamleyi yapan oyuncuya ilet
                LOGGER.log(Level.INFO, "Client {0} made invalid move to {1}: {2}", 
                          new Object[]{from.id, payload, result.message});
                sendToClient(from, new Message(Message.Type.ERROR, "Geçersiz hamle: " + result.message), "move error");
                return; // Hamle geçersizse devam etme
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing move from client " + from.id, e);
            sendToClient(from, new Message(Message.Type.ERROR, "Hamle işlenemedi: " + e.getMessage()), "move error");
            return;
        }
        
        if (state.isOver()) {
            finish("Oyun bitti (Hamle sonrası durum).");
        }
    }

    public synchronized void handlePass(SClient from) throws IOException {
        if (!sessionActive || state.isOver()) { 
            handleInactiveSession(from, "Pas"); 
            return; 
        }
        
        // Sıra kontrolü
        Stone fromColor = (from == black) ? Stone.BLACK : Stone.WHITE;
        if (fromColor != state.toPlay()) {
            LOGGER.log(Level.WARNING, "Client {0} tried to pass out of turn", from.id);
            sendToClient(from, new Message(Message.Type.ERROR, "Hamle sırası sizde değil!"), "turn error");
            return;
        }
        
        // Pas geç
        MoveResult result = state.pass();
        
        if (result.valid) {
            LOGGER.log(Level.INFO, "Client {0} passed.", from.id);
            
            // Hamle geçişinde, mevcut oyuncunun zamanını durdur ve diğer oyuncunun zamanını başlat
            if (fromColor == Stone.BLACK) {
                blackTimer.stop();
                whiteTimer.start();
            } else {
                whiteTimer.stop();
                blackTimer.start();
            }
            
            // Pass hamlesinden sonra tahtayı ve skoru güncelle
            broadcastBoard();
            broadcastScore();
            
            // Süre durumunu güncelle
            sendTimerStatus();
            
            if (state.isOver()) {
                finish("İki oyuncu da pas geçti.");
            }
        } else {
            sendToClient(from, new Message(Message.Type.ERROR, "Pas geçilemedi: " + result.message), "pass error");
        }
    }

    public synchronized void handleResign(SClient from) throws IOException {
        if (!sessionActive || state.isOver()) { 
            handleInactiveSession(from, "Pes"); 
            return; 
        }
        
        Stone resignerColor = (from == black) ? Stone.BLACK : Stone.WHITE;
        LOGGER.log(Level.INFO, "Client {0} ({1}) resigned.", new Object[]{from.id, resignerColor});
        
        // Zamanlayıcıları durdur
        blackTimer.stop();
        whiteTimer.stop();
        
        state.resign(); // Oyunu bitirir
        finish(resignerColor + " pes etti.");
    }

    public synchronized void handleChat(SClient from, String message) throws IOException {
        if (!sessionActive) { 
            handleInactiveSession(from, "Sohbet"); 
            return; 
        }

        Stone senderColor = (from == black) ? Stone.BLACK : Stone.WHITE;
        String formattedMessage = senderColor + ": " + message;
        Message chatMsg = new Message(Message.Type.MSG_FROM_CLIENT, formattedMessage);

        // Her iki oyuncuya da gönder
        sendToClient(black, chatMsg, "chat to black");
        sendToClient(white, chatMsg, "chat to white");
        LOGGER.log(Level.INFO, "Chat relayed from {0}: {1}", new Object[]{from.id, message});
    }

// GameSession.java - finish ve handleDisconnect metodları

private void finish(String reason) throws IOException {
    if (!sessionActive) return; // Zaten bittiyse tekrar bitirme mesajı gönderme

    // Oturumu pasif yap
    sessionActive = false; 
    
    // Zamanlayıcıları durdur
    blackTimer.stop();
    whiteTimer.stop();
    
    int sb = state.scoreFor(Stone.BLACK);
    int sw = state.scoreFor(Stone.WHITE);
    String result = sb + "," + sw + "," + reason;
    var m = new Message(Message.Type.GAME_OVER, result);
    
    LOGGER.log(Level.INFO, "Game Over. Reason: {0}. Score B/W: {1}/{2}", new Object[]{reason, sb, sw});
    
    // Sadece bağlı olan istemcilere mesaj gönder
    try {
        if (black.isConnected()) {
            black.send(m);
        }
    } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Cannot send game over to black", e);
    }
    
    try {
        if (white.isConnected()) {
            white.send(m);
        }
    } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Cannot send game over to white", e);
    }

    // Sunucuya oyunun bittiğini bildir
    server.gameEnded(black, white);
}

public synchronized void handleDisconnect(SClient disconnectedClient) throws IOException {
    if (!sessionActive) return; // Oturum zaten bitmişse tekrar bitirme

    Stone disconnectedColor = (disconnectedClient == black) ? Stone.BLACK : Stone.WHITE;
    SClient opponent = (disconnectedClient == black) ? white : black;
    
    LOGGER.log(Level.WARNING, "Client {0} ({1}) disconnected during active game.", 
              new Object[]{disconnectedClient.id, disconnectedColor});

    // Zamanlayıcıları durdur
    blackTimer.stop();
    whiteTimer.stop();

    // Oturumu bitir
    sessionActive = false; 
    
    // Oyunu bitir
    if (!state.isOver()) { 
        state.resign(); // Bağlantısı kopan oyuncu pes etmiş sayılır
    }
    
    // Sadece hala bağlı olan rakibe bilgi gönder
    if (opponent != null && opponent.isConnected()) {
        try {
            int sb = state.scoreFor(Stone.BLACK);
            int sw = state.scoreFor(Stone.WHITE);
            String result = sb + "," + sw + "," + disconnectedColor + " bağlantısı koptu.";
            opponent.send(new Message(Message.Type.GAME_OVER, result));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error sending disconnect notification to opponent", e);
        }
    }

    // Sunucuya oyunun bittiğini bildir
    server.gameEnded(black, white);
}
    private void broadcastBoard() throws IOException {
        if (!sessionActive) return;
        var json = BoardSerializer.toJson(state.board());
        var m = new Message(Message.Type.BOARD_STATE, json);
        sendToClient(black, m, "board to black");
        sendToClient(white, m, "board to white");
    }

    private void broadcastScore() throws IOException {
        if (!sessionActive) return;
        int sb = state.scoreFor(Stone.BLACK);
        int sw = state.scoreFor(Stone.WHITE);
        String turn = state.toPlay().toString();

        // Skor mesajı: "kendi_skor,rakip_skor,sıradaki_renk"
        Message blackMsg = new Message(Message.Type.SCORE, sb + "," + sw + "," + turn);
        Message whiteMsg = new Message(Message.Type.SCORE, sw + "," + sb + "," + turn);
        sendToClient(black, blackMsg, "score to black");
        sendToClient(white, whiteMsg, "score to white");
    }


    // Mesaj gönderme için yardımcı metod (hata yakalama ile)
private void sendToClient(SClient client, Message message, String description) throws IOException {
    if (client == null || !sessionActive) return;
    
    // İstemcinin hala bağlı olup olmadığını kontrol et
    if (!client.isConnected()) {
        LOGGER.log(Level.WARNING, "Cannot send {0}, client {1} seems disconnected.", 
                  new Object[]{description, client.id});
        return;
    }
    
    try {
        client.send(message);
    } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "IOException sending " + description + " to client " + client.id, e);
        throw e; // Yeniden fırlat, böylece üst metot gerekirse handleDisconnect'i çağırabilir
    }
}
    // Aktif olmayan oturumda gelen istekleri ele almak için
    private void handleInactiveSession(SClient from, String action) {
        LOGGER.log(Level.WARNING, "Client {0} attempted action '{1}' on an inactive/finished session.", 
                  new Object[]{from.id, action});
        try {
            // İstemciye oyunun bittiğini tekrar bildirebiliriz
            sendToClient(from, new Message(Message.Type.ERROR, "Oyun aktif değil veya bitti."), "inactive session error");
        } catch (Exception e) { /* Ignore */}
    }
}
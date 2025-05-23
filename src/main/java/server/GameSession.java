
package server;

import common.IOUtil;
import common.Message;
import game.go.model.GameState;
import game.go.model.Point;
import game.go.model.Stone;
import game.go.model.Board.MoveResult;
import game.go.util.GameTimer;
import game.go.util.GameRecorder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * İki oyuncu arasındaki oyun oturumunu yöneten sınıf. Hamle işleme, skor
 * takibi, saat yönetimi ve oyun kaydı işlevlerini içerir.
 */
public class GameSession {

    private static final Logger LOGGER = Logger.getLogger(GameSession.class.getName());

    // Oyuncular ve bağlantılar
    private final SClient black, white;
    private final GameState state;
    private final Server server;
    private final GameRecorder recorder;

    // Oyun durumu
    private boolean sessionActive = true;
    private final List<Point> moveHistory = new ArrayList<>();
    private int blackCaptureCount = 0;
    private int whiteCaptureCount = 0;

    // Süre yönetimi
    private final GameTimer blackTimer;
    private final GameTimer whiteTimer;
    private static final int DEFAULT_TIME_MINUTES = 30;
    private static final int TIMER_UPDATE_INTERVAL = 1000; // 1 saniye

    /**
     * Yeni bir oyun oturumu oluşturur
     *
     * @param black Siyah oyuncu
     * @param white Beyaz oyuncu
     * @param server Sunucu referansı
     * @param config Oyun konfigürasyonu
     * @throws IOException İletişim hatası olursa
     */
    public GameSession(SClient black, SClient white, Server server, Server.GameConfig config) throws IOException {
        this.black = black;
        this.white = white;
        this.server = server;

        // GameState'i oluştur ve konfigüre et
        this.state = new GameState(config.getBoardSize());
        state.setKomi(config.getKomi());

        // Oyun kaydedicisini oluştur
        this.recorder = new GameRecorder(config.getBoardSize(),
                "Client_" + black.id,
                "Client_" + white.id);
        recorder.setKomi(config.getKomi());
        state.setRecorder(recorder);

        // Zamanlayıcıları oluştur
        this.blackTimer = new GameTimer(DEFAULT_TIME_MINUTES, null);
        this.whiteTimer = new GameTimer(DEFAULT_TIME_MINUTES, null);

        // Handikap taşlarını yerleştir (eğer varsa)
        if (config.getHandicap() > 0) {
            placeHandicapStones(config.getHandicap());
        }

        // Zaman bittiğinde oyunu bitirme aksiyonlarını ayarla
        setupTimerActions();

        // Oyuncuları oturuma bağla
        black.bindSession(this);
        white.bindSession(this);

        // Oyunculara rollerini bildir
        black.send(new Message(Message.Type.ROLE, "BLACK"));
        white.send(new Message(Message.Type.ROLE, "WHITE"));

        // Skor, tahta ve zaman durumunu gönder
        broadcastScore();
        broadcastBoard();
        sendTimerStatus();

        // İlk oyuncunun (siyah) süresini başlat
        blackTimer.start();

        LOGGER.log(Level.INFO, "GameSession started between client {0} (BLACK) and {1} (WHITE) with config: {2}x{2}, Handicap: {3}, Komi: {4}",
                new Object[]{black.id, white.id, config.getBoardSize(), config.getHandicap(), config.getKomi()});

        // Zamanlayıcı güncellemelerini başlat
        startTimerUpdates();
    }

    /**
     * Handikap taşlarını yerleştirir
     *
     * @param handicap Handikap sayısı
     */
    private void placeHandicapStones(int handicap) {
        if (handicap <= 0) {
            return;
        }

        // Tahta boyutuna göre handikap pozisyonlarını al
        List<Point> handicapPoints = getHandicapPoints(state.board().getSize(), handicap);

        // Handikap taşlarını yerleştir
        for (Point p : handicapPoints) {
            MoveResult result = state.board().placeStone(p, Stone.BLACK);
            if (result.valid) {
                recorder.recordMove(p, Stone.BLACK);
                moveHistory.add(p);
                LOGGER.log(Level.INFO, "Placed handicap stone at ({0},{1})", new Object[]{p.x(), p.y()});
            }
        }
    }

    /**
     * Tahta boyutu ve handikap sayısına göre handikap noktalarını döndürür
     *
     * @param boardSize Tahta boyutu
     * @param handicap Handikap sayısı
     * @return Handikap noktaları listesi
     */
    private List<Point> getHandicapPoints(int boardSize, int handicap) {
        List<Point> points = new ArrayList<>();

        // Standart handikap pozisyonları - gerçek Go kurallarına göre ayarlanmalı
        if (boardSize == 19) {
            // 19x19 tahta için standart pozisyonlar
            int[] hoshiPoints = {3, 9, 15}; // Geleneksel hoshi noktaları

            // Handicap taşları için standart pozisyonlar
            if (handicap >= 2) {
                points.add(new Point(15, 3));
            }
            if (handicap >= 2) {
                points.add(new Point(3, 15));
            }
            if (handicap >= 3) {
                points.add(new Point(15, 15));
            }
            if (handicap >= 4) {
                points.add(new Point(3, 3));
            }
            if (handicap >= 5) {
                points.add(new Point(9, 9));
            }
            if (handicap >= 6) {
                points.add(new Point(3, 9));
            }
            if (handicap >= 7) {
                points.add(new Point(15, 9));
            }
            if (handicap >= 8) {
                points.add(new Point(9, 3));
            }
            if (handicap >= 9) {
                points.add(new Point(9, 15));
            }
        } else if (boardSize == 13) {
            // 13x13 tahta için
            int[] hoshiPoints = {3, 6, 9};

            if (handicap >= 2) {
                points.add(new Point(9, 3));
            }
            if (handicap >= 2) {
                points.add(new Point(3, 9));
            }
            if (handicap >= 3) {
                points.add(new Point(9, 9));
            }
            if (handicap >= 4) {
                points.add(new Point(3, 3));
            }
            if (handicap >= 5) {
                points.add(new Point(6, 6));
            }
            // Diğer pozisyonlar...
        } else if (boardSize == 9) {
            // 9x9 tahta için
            int[] hoshiPoints = {2, 4, 6};

            if (handicap >= 2) {
                points.add(new Point(6, 2));
            }
            if (handicap >= 2) {
                points.add(new Point(2, 6));
            }
            if (handicap >= 3) {
                points.add(new Point(6, 6));
            }
            if (handicap >= 4) {
                points.add(new Point(2, 2));
            }
            if (handicap >= 5) {
                points.add(new Point(4, 4));
            }
            // Diğer pozisyonlar...
        }

        return points;
    }

    /**
     * Zamanlayıcı aksiyonlarını ayarlar
     */
    private void setupTimerActions() {
        // Siyah zamanlayıcı aksiyonları
        blackTimer.setTimeoutAction(() -> {
            try {
                LOGGER.info("Black's time is up!");
                timeOut(Stone.BLACK);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error handling black timeout", e);
            }
        });

        // Beyaz zamanlayıcı aksiyonları
        whiteTimer.setTimeoutAction(() -> {
            try {
                LOGGER.info("White's time is up!");
                timeOut(Stone.WHITE);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error handling white timeout", e);
            }
        });
    }

    /**
     * Zaman güncellemelerini periyodik olarak gönderir
     */
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

    /**
     * Zaman durumunu her iki oyuncuya da gönderir
     *
     * @throws IOException İletişim hatası olursa
     */
    private void sendTimerStatus() throws IOException {
        if (!sessionActive) {
            return;
        }

        String blackTime = blackTimer.getTimeText();
        String whiteTime = whiteTimer.getTimeText();

        // Siyah oyuncuya zaman bilgisini gönder
        sendToClient(black, new Message(Message.Type.TIMER_UPDATE, blackTime + "," + whiteTime), "timer to black");

        // Beyaz oyuncuya zaman bilgisini gönder
        sendToClient(white, new Message(Message.Type.TIMER_UPDATE, whiteTime + "," + blackTime), "timer to white");
    }

    /**
     * Zaman aşımında oyunu bitirir
     *
     * @param player Süresi biten oyuncu
     * @throws IOException İletişim hatası olursa
     */
    private synchronized void timeOut(Stone player) throws IOException {
        if (!sessionActive || state.isOver()) {
            return;
        }

        // Oyunu bitir
        state.resign();

        String timeoutPlayer = (player == Stone.BLACK) ? "BLACK" : "WHITE";
        finish(timeoutPlayer + " süre dolduğu için oyunu kaybetti.");
    }

    /**
     * Oyuncu hamlesini işler
     *
     * @param from Hamleyi yapan oyuncu
     * @param payload Hamle bilgisi (x,y formatında)
     * @throws IOException İletişim hatası olursa
     */
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
        
        // Hamle yapılacak pozisyonda taş var mı kontrol et
        if (state.board().get(p) != Stone.EMPTY) {
            LOGGER.log(Level.INFO, "Client {0} tried to place stone on occupied position {1}", 
                      new Object[]{from.id, payload});
            sendToClient(from, new Message(Message.Type.ERROR, "Bu pozisyonda zaten bir taş var!"), "invalid move");
            return;
        }
        
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
    /**
     * Oyuncunun undo (geri alma) talebini işler
     *
     * @param from Talepte bulunan oyuncu
     * @throws IOException İletişim hatası olursa
     */
    public synchronized void handleUndoRequest(SClient from) throws IOException {
        if (!sessionActive || state.isOver()) {
            handleInactiveSession(from, "Geri alma");
            return;
        }

        // Sıra kontrolü - sadece kendi sırasında undo talep edebilir
        Stone fromColor = (from == black) ? Stone.BLACK : Stone.WHITE;
        if (fromColor != state.toPlay()) {
            LOGGER.log(Level.WARNING, "Client {0} tried to undo out of turn", from.id);
            sendToClient(from, new Message(Message.Type.ERROR, "Geri alma talebini sadece kendi sıranızda yapabilirsiniz!"), "undo error");
            return;
        }

        // Geri alınacak hamle var mı?
        if (moveHistory.isEmpty()) {
            sendToClient(from, new Message(Message.Type.ERROR, "Geri alınacak hamle bulunamadı!"), "undo error");
            return;
        }

        // Diğer oyuncuya talep gönder
        SClient opponent = (from == black) ? white : black;
        sendToClient(opponent, new Message(Message.Type.MSG_FROM_CLIENT,
                "System: Rakibiniz son hamleyi geri alma talebinde bulundu. Kabul ediyor musunuz? (UNDO_ACCEPT veya UNDO_REJECT)"),
                "undo request");

        sendToClient(from, new Message(Message.Type.MSG_FROM_CLIENT,
                "System: Geri alma talebiniz rakibinize iletildi, yanıt bekleniyor..."),
                "undo request sent");
    }

    /**
     * Undo talebini kabul etme işlemi
     *
     * @param from Kabul eden oyuncu
     * @throws IOException İletişim hatası olursa
     */
    public synchronized void handleUndoAccept(SClient from) throws IOException {
        if (!sessionActive || state.isOver()) {
            handleInactiveSession(from, "Undo kabul");
            return;
        }

        // Talepte bulunan oyuncu değilse
        Stone fromColor = (from == black) ? Stone.BLACK : Stone.WHITE;
        if (fromColor == state.toPlay()) {
            sendToClient(from, new Message(Message.Type.ERROR, "Kendi talebinizi kabul edemezsiniz!"), "undo error");
            return;
        }

        // 2 hamle geri al (her iki oyuncunun da son hamlesini)
        if (moveHistory.size() >= 2) {
            // NOT: Gerçek uygulamada, hamleleri geri almak için özel bir mekanizma gerekir.
            // Bu örnek kodu basitleştirmek için sadece tahta durumunu yeniden oluşturacağız.
            LOGGER.log(Level.INFO, "Undo accepted: Removing last 2 moves");

            // Son 2 hamleyi sil
            moveHistory.remove(moveHistory.size() - 1);
            moveHistory.remove(moveHistory.size() - 1);

            // TODO: Tahtayı yeniden oluştur (gerçek uygulamada)
            // Tüm oyunculara bildir
            sendToClient(black, new Message(Message.Type.MSG_FROM_CLIENT,
                    "System: Son iki hamle geri alındı."),
                    "undo notification");
            sendToClient(white, new Message(Message.Type.MSG_FROM_CLIENT,
                    "System: Son iki hamle geri alındı."),
                    "undo notification");

            // Tahta ve skor güncelleme
            broadcastBoard();
            broadcastScore();
        } else {
            sendToClient(from, new Message(Message.Type.ERROR, "Geri alınacak yeterli hamle yok!"), "undo error");
        }
    }

    /**
     * Undo talebini reddetme işlemi
     *
     * @param from Reddeden oyuncu
     * @throws IOException İletişim hatası olursa
     */
    public synchronized void handleUndoReject(SClient from) throws IOException {
        if (!sessionActive || state.isOver()) {
            handleInactiveSession(from, "Undo red");
            return;
        }

        // Talepte bulunan oyuncu değilse
        Stone fromColor = (from == black) ? Stone.BLACK : Stone.WHITE;
        if (fromColor == state.toPlay()) {
            sendToClient(from, new Message(Message.Type.ERROR, "Kendi talebinizi reddedemezsiniz!"), "undo error");
            return;
        }

        // Talep eden oyuncuya bildir
        SClient requester = (from == black) ? white : black;
        sendToClient(requester, new Message(Message.Type.MSG_FROM_CLIENT,
                "System: Geri alma talebiniz reddedildi."),
                "undo rejected");

        // Reddeden oyuncuya onay
        sendToClient(from, new Message(Message.Type.MSG_FROM_CLIENT,
                "System: Geri alma talebini reddettiniz."),
                "undo rejection confirmed");
    }

    /**
     * Oyuncunun pas geçme hamlesini işler
     *
     * @param from Pas geçen oyuncu
     * @throws IOException İletişim hatası olursa
     */
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

            // Hamle geçişinde zamanlayıcıları değiştir
            if (fromColor == Stone.BLACK) {
                blackTimer.stop();
                whiteTimer.start();
            } else {
                whiteTimer.stop();
                blackTimer.start();
            }

            // Pas sonrası tahta ve skor güncelleme
            broadcastBoard();
            broadcastScore();

            // Süre durumunu güncelle
            sendTimerStatus();

            // Oyun bitti mi kontrol et (iki pas üst üste)
            if (state.isOver()) {
                finish("İki oyuncu da pas geçti.");
            }
        } else {
            sendToClient(from, new Message(Message.Type.ERROR, "Pas geçilemedi: " + result.message), "pass error");
        }
    }

    /**
     * Oyuncunun istifa (teslim olma) hamlesini işler
     *
     * @param from İstifa eden oyuncu
     * @throws IOException İletişim hatası olursa
     */
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

        state.resign();
        finish(resignerColor + " pes etti.");
    }

    /**
     * Oyuncunun sohbet mesajını işler
     *
     * @param from Mesajı gönderen oyuncu
     * @param message Mesaj içeriği
     * @throws IOException İletişim hatası olursa
     */
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

    /**
     * Oyunu bitirir ve sonuçları bildirir
     *
     * @param reason Bitiş sebebi
     * @throws IOException İletişim hatası olursa
     */
    private void finish(String reason) throws IOException {
        if (!sessionActive) {
            return;
        }

        // Oturumu pasif yap
        sessionActive = false;

        // Zamanlayıcıları durdur
        blackTimer.stop();
        whiteTimer.stop();

        // Skor ve bitiş mesajı
        int sb = state.scoreFor(Stone.BLACK);
        int sw = state.scoreFor(Stone.WHITE);
        String result = sb + "," + sw + "," + reason;
        var endMessage = new Message(Message.Type.GAME_OVER, result);

        LOGGER.log(Level.INFO, "Game Over. Reason: {0}. Score B/W: {1}/{2}", new Object[]{reason, sb, sw});

        // Sadece bağlı olan istemcilere mesaj gönder
        try {
            if (black.isConnected()) {
                black.send(endMessage);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot send game over to black", e);
        }

        try {
            if (white.isConnected()) {
                white.send(endMessage);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot send game over to white", e);
        }

        // Oyun kaydını tamamla
        if (recorder != null) {
            recorder.markGameFinished();
            recorder.verifyRecordedMoves();

            // İsteğe bağlı: Oyun kayıtlarını kaydet
            String sgfPath = "games/game_" + black.id + "_vs_" + white.id + "_"
                    + System.currentTimeMillis() + ".sgf";
            recorder.saveToSgf(sgfPath);
            LOGGER.log(Level.INFO, "Game record saved to {0}", sgfPath);
        }

        // Sunucuya oyunun bittiğini bildir
        server.gameEnded(black, white);
    }

    /**
     * Bir oyuncunun bağlantısının koptuğunu işler
     *
     * @param disconnectedClient Bağlantısı kopan oyuncu
     * @throws IOException İletişim hatası olursa
     */
    public synchronized void handleDisconnect(SClient disconnectedClient) throws IOException {
        if (!sessionActive) {
            return;
        }

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
            state.resign();
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

        // Oyun kaydını tamamla
        if (recorder != null) {
            recorder.markGameFinished();
        }

        // Sunucuya oyunun bittiğini bildir
        server.gameEnded(black, white);
    }

    /**
     * Tahta durumunu her iki oyuncuya da gönderir
     *
     * @throws IOException İletişim hatası olursa
     */
    private void broadcastBoard() throws IOException {
        if (!sessionActive) {
            return;
        }

        var json = BoardSerializer.toJson(state.board());
        var boardMessage = new Message(Message.Type.BOARD_STATE, json);

        sendToClient(black, boardMessage, "board to black");
        sendToClient(white, boardMessage, "board to white");
    }

    /**
     * Skor durumunu her iki oyuncuya da gönderir
     *
     * @throws IOException İletişim hatası olursa
     */
private void broadcastScore() throws IOException {
    if (!sessionActive) return;
    
    int blackPoints = state.scoreFor(Stone.BLACK);
    int whitePoints = state.scoreFor(Stone.WHITE);
    String turn = state.toPlay().toString();
    
    // Her iki oyuncu için de tutarlı bir formatta skor gönder
    // Format: "BLACK_SCORE,WHITE_SCORE,TURN"
    String scoreMessage = blackPoints + "," + whitePoints + "," + turn;
    Message scoreMsg = new Message(Message.Type.SCORE, scoreMessage);
    
    sendToClient(black, scoreMsg, "score to black");
    sendToClient(white, scoreMsg, "score to white");
}
/**
     * Mesaj gönderme için yardımcı metod (hata yakalama ile)
     *
     * @param client Mesajın gönderileceği istemci
     * @param message Gönderilecek mesaj
     * @param description İşlem açıklaması (log için)
     * @throws IOException Mesaj gönderme hatası olursa
     */
    private void sendToClient(SClient client, Message message, String description) throws IOException {
        if (client == null || !sessionActive) {
            return;
        }

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

    /**
     * Aktif olmayan oturumda gelen istekleri ele almak için
     *
     * @param from İstek gönderen istemci
     * @param action İşlem adı
     */
    private void handleInactiveSession(SClient from, String action) {
        LOGGER.log(Level.WARNING, "Client {0} attempted action '{1}' on an inactive/finished session.",
                new Object[]{from.id, action});
        try {
            // İstemciye oyunun bittiğini tekrar bildirebiliriz
            sendToClient(from, new Message(Message.Type.ERROR, "Oyun aktif değil veya bitti."), "inactive session error");
        } catch (Exception e) {
            /* Yoksay */ }
    }
}

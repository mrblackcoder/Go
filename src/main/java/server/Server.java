package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import common.Message;

/**
 * Go oyunu sunucusu - Oyuncuları eşleştirir ve oyun oturumlarını yönetir.
 * Geliştirilmiş sürüm: Oyun konfigürasyonu ve eşleştirme sistemi.
 */
public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    
    // Temel sunucu bileşenleri
    private final ServerSocket serverSocket;
    private final List<SClient> clients = Collections.synchronizedList(new ArrayList<>());
    private int idSequence = 0;
    private boolean isRunning = true;
    
    // Eşleştirme sistemi
    private final Object matchingLock = new Object();
    private final Map<Integer, List<SClient>> waitingClientsByConfig = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    /**
     * Oyun konfigürasyon sınıfı - tahta boyutu, handikap ve komi değerlerini içerir
     */
    public static class GameConfig {
        private final int boardSize;
        private final int handicap;
        private final double komi;
        
        /**
         * Yeni bir oyun konfigürasyonu oluşturur
         * 
         * @param boardSize Tahta boyutu (9, 13 veya 19)
         * @param handicap Handikap değeri (0-9)
         * @param komi Komi değeri (genellikle 0.5, 5.5, 6.5, 7.5)
         */
        public GameConfig(int boardSize, int handicap, double komi) {
            this.boardSize = boardSize;
            this.handicap = handicap;
            this.komi = komi;
        }
        
        /**
         * Konfigürasyon ID'sini döndürür, eşleştirme için kullanılır
         * @return Konfigürasyon ID
         */
        public int getConfigId() {
            return boardSize == 9 ? 1 : (boardSize == 13 ? 2 : 3);
        }
        
        public int getBoardSize() {
            return boardSize;
        }
        
        public int getHandicap() {
            return handicap;
        }
        
        public double getKomi() {
            return komi;
        }
        
        @Override
        public String toString() {
            return boardSize + "x" + boardSize + ", Handicap: " + handicap + ", Komi: " + komi;
        }
    }
    
    /**
     * Yeni bir Go sunucusu oluşturur
     * 
     * @param port Dinlenecek port
     * @throws IOException Sunucu soketi oluşturulurken hata olursa
     */
    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        LOGGER.info("Server started on port " + port);
        
        // Varsayılan oyun konfigürasyonları için bekleyen listeler oluştur
        waitingClientsByConfig.put(1, new ArrayList<>()); // 9x9
        waitingClientsByConfig.put(2, new ArrayList<>()); // 13x13
        waitingClientsByConfig.put(3, new ArrayList<>()); // 19x19
    }
    
    /**
     * Sunucuyu başlatır ve istemci bağlantılarını kabul eder
     */
    public void start() {
        Thread acceptThread = new Thread(() -> {
            try {
                LOGGER.info("Server is ready to accept connections");
                
                while (isRunning) {
                    try {
                        Socket socket = serverSocket.accept();
                        handleNewClient(socket);
                    } catch (IOException e) {
                        if (isRunning) {
                            LOGGER.log(Level.WARNING, "Error accepting client connection", e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Server thread error", e);
            }
        });
        
        acceptThread.setDaemon(true);
        acceptThread.start();
    }
    
    /**
     * Yeni bir istemci bağlantısını işler
     * 
     * @param socket İstemci soketi
     */
    private void handleNewClient(Socket socket) {
        try {
            SClient client = new SClient(socket, this);
            
            // İstemciyi listeye ekle ve başlat
            clients.add(client);
            client.start();
            
            // Bağlı istemci listesini tüm istemcilere güncelle
            broadcastClientIds();
            
            LOGGER.log(Level.INFO, "New client connected: {0} from {1}", 
                      new Object[]{client.id, socket.getRemoteSocketAddress()});
            
            // Hoş geldin mesajı gönder
            client.send(new Message(Message.Type.MSG_FROM_CLIENT, 
                                   "System: Hoş geldiniz! Oyun konfigürasyonunuzu seçerek eşleşme bekleyebilirsiniz."));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error handling new client", e);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * Sunucuyu kapatır
     * 
     * @throws IOException Soketler kapatılırken hata olursa
     */
    public void shutdown() throws IOException {
        isRunning = false;
        
        // Tüm istemcilere bildir
        for (SClient client : new ArrayList<>(clients)) {
            try {
                client.send(new Message(Message.Type.MSG_FROM_CLIENT, 
                                       "System: Sunucu kapatılıyor..."));
                client.closeConnection();
            } catch (IOException ignored) {}
        }
        
        // Sunucu soketini kapat
        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
        
        LOGGER.info("Server has been shut down");
    }
    
    /**
     * Bir sonraki istemci ID'sini döndürür
     * 
     * @return Bir sonraki istemci ID'si
     */
    public synchronized int nextId() {
        return idSequence++;
    }
    
    /**
     * İstemciyi belirtilen konfigürasyon ile bekleme sırasına ekler
     * 
     * @param client Eklenecek istemci
     * @param configId Oyun konfigürasyon ID'si
     */
    public synchronized void addToWaitingQueue(SClient client, int configId) {
        // Geçersiz configId ise, varsayılan olarak 19x19 kullan
        if (!waitingClientsByConfig.containsKey(configId)) {
            configId = 3; // 19x19
        }
        
        List<SClient> waitingList = waitingClientsByConfig.get(configId);
        
        // İstemci bağlıysa ve oyunda değilse ve zaten listede yoksa ekle
        if (!waitingList.contains(client) && client.isConnected() && !client.isInGame()) {
            waitingList.add(client);
            
            // Konfigürasyon bilgisini oluştur
            GameConfig config = getConfigById(configId);
            
            LOGGER.log(Level.INFO, "Client {0} added to waiting queue with config: {1}. Queue size: {2}", 
                      new Object[]{client.id, config, waitingList.size()});
            
            try {
                // İstemciye bekleme durumunu bildir
                client.send(new Message(Message.Type.MSG_FROM_CLIENT, 
                                       "System: Eşleşme bekleniyor... " + 
                                       "Seçilen konfigürasyon: " + config + ". " +
                                       "Sırada bekleyen: " + waitingList.size()));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not send waiting message to client " + client.id, e);
                waitingList.remove(client);
            }
        }
    }
    
    /**
     * İstemci belirtilmezse, varsayılan olarak 19x19 konfigürasyonu için sıraya ekle
     * 
     * @param client Eklenecek istemci
     */
    public void addToWaitingQueue(SClient client) {
        addToWaitingQueue(client, 3); // Varsayılan: 19x19
    }
    
    /**
     * ID'ye göre oyun konfigürasyonunu döndürür
     * 
     * @param configId Konfigürasyon ID'si
     * @return Oyun konfigürasyonu
     */
    private GameConfig getConfigById(int configId) {
        switch (configId) {
            case 1: return new GameConfig(9, 0, 5.5);  // 9x9
            case 2: return new GameConfig(13, 0, 6.5); // 13x13
            default: return new GameConfig(19, 0, 6.5); // 19x19
        }
    }
    
    /**
     * Tüm bekleyen eşleştirmeleri kontrol eder ve mümkünse oyun oturumları oluşturur
     */
    public void checkAndCreateMatch() {
        synchronized (matchingLock) {
            // Her konfigürasyon için kontrol et
            for (Map.Entry<Integer, List<SClient>> entry : waitingClientsByConfig.entrySet()) {
                List<SClient> waitingList = entry.getValue();
                int configId = entry.getKey();
                
                // En az iki istemci varsa, eşleştirme yap
                if (waitingList.size() >= 2) {
                    // İlk iki istemciyi al
                    SClient client1 = waitingList.get(0);
                    SClient client2 = waitingList.get(1);
                    
                    // Bağlantı kontrolü
                    if (!isClientUsable(client1)) {
                        waitingList.remove(client1);
                        checkAndCreateMatch(); // Tekrar kontrol et
                        return;
                    }
                    
                    if (!isClientUsable(client2)) {
                        waitingList.remove(client2);
                        checkAndCreateMatch(); // Tekrar kontrol et
                        return;
                    }
                    
                    // Listeden çıkar
                    waitingList.remove(client1);
                    waitingList.remove(client2);
                    
                    // Oyun konfigürasyonunu al
                    GameConfig config = getConfigById(configId);
                    
                    // Rastgele renk ataması
                    boolean client1IsBlack = random.nextBoolean();
                    SClient blackClient = client1IsBlack ? client1 : client2;
                    SClient whiteClient = client1IsBlack ? client2 : client1;
                    
                    LOGGER.log(Level.INFO, "Creating match with config {0}: Client {1} (BLACK) vs Client {2} (WHITE)", 
                              new Object[]{config, blackClient.id, whiteClient.id});
                    
                    createGameSession(blackClient, whiteClient, config);
                }
            }
        }
    }
    
    /**
     * İki istemci arasında yeni bir oyun oturumu oluşturur
     * 
     * @param blackClient Siyah oyuncu
     * @param whiteClient Beyaz oyuncu
     * @param config Oyun konfigürasyonu
     */
    private void createGameSession(SClient blackClient, SClient whiteClient, GameConfig config) {
        try {
            // Bildirim mesajları
            try {
                String configInfo = "Tahta: " + config.getBoardSize() + "x" + config.getBoardSize() + 
                                   ", Handicap: " + config.getHandicap() + 
                                   ", Komi: " + config.getKomi();
                
                blackClient.send(new Message(Message.Type.MSG_FROM_CLIENT, 
                                          "System: Rakip bulundu! Oyun başlıyor... (Siz BLACK oynuyorsunuz)\n" + configInfo));
                whiteClient.send(new Message(Message.Type.MSG_FROM_CLIENT, 
                                          "System: Rakip bulundu! Oyun başlıyor... (Siz WHITE oynuyorsunuz)\n" + configInfo));
            } catch (IOException e) {
                LOGGER.warning("Error sending match notification: " + e.getMessage());
                handleMatchingError(blackClient, whiteClient, config.getConfigId());
                return;
            }
            
            // Oyun oturumu oluştur
            GameSession session = new GameSession(blackClient, whiteClient, this, config);
            
            // Oyuncuları oyunda olarak işaretle
            blackClient.setInGame(true);
            whiteClient.setInGame(true);
            blackClient.bindSession(session);
            whiteClient.bindSession(session);
            
            LOGGER.info("Game session started successfully");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating game session", e);
            handleMatchingError(blackClient, whiteClient, config.getConfigId());
        }
    }
    
    /**
     * İstemcinin kullanılabilir olup olmadığını kontrol eder
     * 
     * @param client Kontrol edilecek istemci
     * @return true ise istemci kullanılabilir
     */
    private boolean isClientUsable(SClient client) {
        return client != null && client.isConnected() && !client.isInGame();
    }
    
    /**
     * Eşleştirme hatası durumunda istemcileri tekrar sıraya ekler
     * 
     * @param client1 Birinci istemci
     * @param client2 İkinci istemci
     * @param configId Konfigürasyon ID'si
     */
    private void handleMatchingError(SClient client1, SClient client2, int configId) {
        LOGGER.warning("Match creation failed, returning clients to waiting queue");
        
        // Bağlantı durumlarını kontrol et ve bekleme listesine geri ekle
        if (isClientUsable(client1)) {
            client1.clearSession();
            client1.setInGame(false);
            addToWaitingQueue(client1, configId);
        }
        
        if (isClientUsable(client2)) {
            client2.clearSession();
            client2.setInGame(false);
            addToWaitingQueue(client2, configId);
        }
        
        // Yeni eşleştirme kontrolü yap
        checkAndCreateMatch();
    }
    
    /**
     * Oyun bittiğinde çağrılır, oyuncuları oyun durumundan çıkarır
     * 
     * @param player1 Birinci oyuncu
     * @param player2 İkinci oyuncu
     */
    public synchronized void gameEnded(SClient player1, SClient player2) {
        LOGGER.log(Level.INFO, "Game ended between clients {0} and {1}", 
                  new Object[]{player1.id, player2.id});
        
        // Sadece bağlantısı aktif olan istemcileri bekleme sırasına al
        if (isClientUsable(player1)) {
            player1.clearSession();
            player1.setInGame(false);
        }
        
        if (isClientUsable(player2)) {
            player2.clearSession();
            player2.setInGame(false);
        }
    }
    
    /**
     * Tüm istemcilere bağlı istemci listesini gönderir
     * 
     * @throws IOException Mesaj gönderilirken hata olursa
     */
    private void broadcastClientIds() throws IOException {
        String ids = clients.stream()
            .map(c -> String.valueOf(c.id))
            .collect(Collectors.joining(","));
        
        Message message = new Message(Message.Type.CLIENT_IDS, ids);
        
        for (SClient client : clients) {
            try {
                if (client.isConnected()) {
                    client.send(message);
                }
            } catch (IOException e) {
                LOGGER.warning("Error broadcasting client IDs to client " + client.id);
            }
        }
    }
    
    /**
     * Belirli bir istemciye mesaj gönderir
     * 
     * @param targetId Hedef istemci ID'si
     * @param text Mesaj metni
     * @throws IOException Mesaj gönderilirken hata olursa
     */
    void sendToClient(int targetId, String text) throws IOException {
        for (SClient client : clients) {
            if (client.id == targetId && client.isConnected()) {
                client.send(new Message(Message.Type.MSG_FROM_CLIENT, targetId + "," + text));
                break;
            }
        }
    }
    
    /**
     * İstemciyi sunucudan çıkarır
     * 
     * @param client Çıkarılacak istemci
     */
    synchronized void removeClient(SClient client) {
        if (clients.remove(client)) {
            LOGGER.log(Level.INFO, "Client {0} removed from server. Remaining clients: {1}", 
                      new Object[]{client.id, clients.size()});
            
            // Tüm bekleme listelerinden çıkar
            for (List<SClient> waitingList : waitingClientsByConfig.values()) {
                waitingList.remove(client);
            }
            
            try {
                broadcastClientIds();
            } catch (IOException ignored) {}
            
            // Eşleştirme kontrolü yap
            checkAndCreateMatch();
        }
    }
    
    /**
     * İstatistik bilgilerini döndürür
     * 
     * @return İstemci sayısı
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * Bekleyen istemci sayısını döndürür
     * 
     * @return Bekleyen istemci sayısı
     */
    public int getWaitingClientCount() {
        return waitingClientsByConfig.values().stream()
            .mapToInt(List::size)
            .sum();
    }
}
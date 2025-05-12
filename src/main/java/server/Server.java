package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import common.Message;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Server {
    private final ServerSocket serverSock;
    private final List<SClient> clients = Collections.synchronizedList(new ArrayList<>());
    // Aktif oyunda olmayan ve eşleşmeye hazır bekleyen müşteriler
    private final List<SClient> waitingClients = Collections.synchronizedList(new ArrayList<>());
    private int idSeq = 0;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    // Eşleştirme kilidi - aynı anda tek bir eşleştirme işleminin çalışmasını sağlar
    private final Object matchingLock = new Object();
    // Rastgele sayı üreteci
    private final Random random = new Random();

    public Server(int port) throws IOException { 
        serverSock = new ServerSocket(port); 
        LOGGER.info("Server started on port " + port);
    }
    
    public void shutdown() throws IOException { 
        serverSock.close(); 
        LOGGER.info("Server shutting down");
    }
    
    public int nextId() { 
        return idSeq++; 
    }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    Socket s = serverSock.accept();
                    SClient c = new SClient(s, this);
                    
                    // Yeni istemciyi listeye ekle ve başlat
                    clients.add(c);
                    c.start();
                    
                    // Bağlı client listesini güncelle
                    broadcastClientIds();
                    
                    // Yeni client'ı bekleme listesine ekle
                    addToWaitingQueue(c);
                    
                    // Eşleştirme kontrolü yap
                    checkAndCreateMatch();
                }
            } catch (IOException e) {
                LOGGER.info("Server shut down");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Unexpected error", ex);
            }
        }).start();
    }

    // Bekleme listesine client ekleme
    public synchronized void addToWaitingQueue(SClient client) {
        // Eğer istemci zaten listede değilse ve bağlı ise
        if (!waitingClients.contains(client) && client.isConnected() && !client.isInGame()) {
            waitingClients.add(client);
            LOGGER.info("Client " + client.id + " added to waiting queue. Current queue size: " + waitingClients.size());
            
            try {
                // İstemciye bekleme durumunu bildir
                client.send(new Message(Message.Type.MSG_FROM_CLIENT, "System: Eşleşme bekleniyor... Sırada " + waitingClients.size() + " oyuncu var."));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not send waiting message to client " + client.id, e);
                // İstemciyi listeden çıkar (bağlantı sorunu var)
                waitingClients.remove(client);
            }
        }
    }
    
    // Eşleştirme kontrolü ve oyun başlatma
    public void checkAndCreateMatch() {
        synchronized (matchingLock) {
            if (waitingClients.size() >= 2) {
                // Listedeki ilk iki istemciyi al
                SClient client1 = waitingClients.get(0);
                SClient client2 = waitingClients.get(1);
                
                // Bağlantı kontrolü
                if (!isClientUsable(client1)) {
                    waitingClients.remove(client1);
                    checkAndCreateMatch(); // Tekrar kontrol et
                    return;
                }
                
                if (!isClientUsable(client2)) {
                    waitingClients.remove(client2);
                    checkAndCreateMatch(); // Tekrar kontrol et
                    return;
                }
                
                // Listeden çıkar
                waitingClients.remove(client1);
                waitingClients.remove(client2);
                
                // Rastgele renk ataması yap (50% şans)
                boolean client1IsBlack = random.nextBoolean();
                
                SClient blackClient = client1IsBlack ? client1 : client2;
                SClient whiteClient = client1IsBlack ? client2 : client1;
                
                LOGGER.info("Creating match: Client " + blackClient.id + " (BLACK) vs Client " + whiteClient.id + " (WHITE)");
                
                try {
                    // Bildirim mesajları
                    try {
                        blackClient.send(new Message(Message.Type.MSG_FROM_CLIENT, "System: Rakip bulundu! Oyun başlıyor... (Siz BLACK oynuyorsunuz)"));
                        whiteClient.send(new Message(Message.Type.MSG_FROM_CLIENT, "System: Rakip bulundu! Oyun başlıyor... (Siz WHITE oynuyorsunuz)"));
                    } catch (IOException e) {
                        LOGGER.warning("Error sending match notification: " + e.getMessage());
                        // Hata durumunda tekrar bekleme listesine ekle
                        handleMatchingError(blackClient, whiteClient);
                        return;
                    }
                    
                    // Oyun oturumu oluştur
                    GameSession session = new GameSession(blackClient, whiteClient, this);
                    
                    // Oyuncuları oyunda olarak işaretle
                    blackClient.setInGame(true);
                    whiteClient.setInGame(true);
                    blackClient.bindSession(session);
                    whiteClient.bindSession(session);
                    
                    LOGGER.info("Game session started successfully");
                    
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error creating game session", e);
                    handleMatchingError(blackClient, whiteClient);
                }
            }
        }
    }
    
    // İstemcinin kullanılabilir olup olmadığını kontrol et
    private boolean isClientUsable(SClient client) {
        return client != null && client.isConnected() && !client.isInGame();
    }
    
    // Eşleştirme hatası durumunda
    private void handleMatchingError(SClient client1, SClient client2) {
        LOGGER.warning("Match creation failed, returning clients to waiting queue");
        
        // Bağlantı durumlarını kontrol et ve bekleme listesine geri ekle
        if (isClientUsable(client1)) {
            client1.clearSession();
            client1.setInGame(false);
            addToWaitingQueue(client1);
        }
        
        if (isClientUsable(client2)) {
            client2.clearSession();
            client2.setInGame(false);
            addToWaitingQueue(client2);
        }
        
        // Yeni eşleştirme kontrolü yap
        checkAndCreateMatch();
    }
    
    // Oyun bittiğinde çağrılır
    public synchronized void gameEnded(SClient player1, SClient player2) {
        LOGGER.info("Game ended between clients " + player1.id + " and " + player2.id);
        
        // Sadece bağlantısı aktif olan istemcileri bekleme sırasına al
        if (isClientUsable(player1)) {
            player1.clearSession();
            player1.setInGame(false);
            addToWaitingQueue(player1);
        }
        
        if (isClientUsable(player2)) {
            player2.clearSession();
            player2.setInGame(false);
            addToWaitingQueue(player2);
        }
        
        // Eşleştirme kontrolü yap
        checkAndCreateMatch();
    }

    private void broadcastClientIds() throws IOException {
        String ids = clients.stream().map(c -> String.valueOf(c.id)).collect(Collectors.joining(","));
        Message m = new Message(Message.Type.CLIENT_IDS, ids);
        for (SClient c : clients) {
            try {
                if (c.isConnected()) {
                    c.send(m);
                }
            } catch (IOException e) {
                LOGGER.warning("Error broadcasting client IDs to client " + c.id);
                // Burada bir şey yapmıyoruz - client zaten kendisi bağlantı kopukluğunu algılayacak
            }
        }
    }

    void sendToClient(int targetId, String text) throws IOException {
        for (SClient c : clients) {
            if (c.id == targetId && c.isConnected()) { 
                c.send(new Message(Message.Type.MSG_FROM_CLIENT, targetId+","+text));
                break;
            }
        }
    }

    synchronized void removeClient(SClient c) {
        if (clients.remove(c)) {
            LOGGER.info("Client " + c.id + " removed from server. Remaining clients: " + clients.size());
            
            // Bekleme listesinden de çıkar
            waitingClients.remove(c);
            
            try { 
                broadcastClientIds(); 
            } catch (IOException ignored) {}
            
            // Eşleştirme kontrolü yap
            checkAndCreateMatch();
        }
    }
    
    // İstatistik bilgisi
    public int getClientCount() {
        return clients.size();
    }
    
    public int getWaitingClientCount() {
        return waitingClients.size();
    }
}
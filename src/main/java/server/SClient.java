package server;

import common.IOUtil;
import common.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SClient extends Thread {
    final int id;
    public final Socket sock;
    private final InputStream in;
    private final OutputStream out;
    private final Server hub;
    private GameSession session;
    private boolean running = true;
    private boolean inGame = false; // Oyuncu şu an oyunda mı
    private static final Logger LOGGER = Logger.getLogger(SClient.class.getName());

    public SClient(Socket s, Server hub) throws Exception {
        this.sock = s; 
        this.hub = hub;
        this.id = hub.nextId();
        this.in = s.getInputStream(); 
        this.out = s.getOutputStream();
    }

    public void bindSession(GameSession gs) { 
        this.session = gs; 
        this.inGame = true;
    }
    
    public void clearSession() {
        this.session = null;
        this.inGame = false;
    }
    
    public boolean isInGame() {
        return inGame;
    }
    
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }
    
    /**
     * Bağlantı durumunu kontrol eder.
     * 
     * @return true bağlantı aktifse, false değilse
     */
    public boolean isConnected() {
        return running && sock != null && !sock.isClosed() && sock.isConnected();
    }
    
    public void send(Message m) throws IOException { 
        if (!isConnected()) {
            throw new IOException("Connection is closed");
        }
        
        try {
            IOUtil.writeMessage(out, m); 
            LOGGER.log(Level.INFO, "Sent to client {0}: {1}#{2}", new Object[]{id, m.type(), m.payload()});
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error sending to client " + id, e);
            throw e; // Yeniden fırlat, böylece çağıran metot uygun şekilde ele alabilir
        }
    }
    
    /**
     * Bağlantıyı kapatır ve istemciyi sunucudan çıkarır.
     */
    public void closeConnection() {
        try {
            running = false;
            if (sock != null && !sock.isClosed()) {
                sock.close();
            }
            hub.removeClient(this);
            LOGGER.log(Level.INFO, "Client {0} connection closed", id);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing client connection", e);
        }
    }
    
    @Override
    public void run() {
        LOGGER.log(Level.INFO, "Client {0} connected from {1}", new Object[]{id, sock.getRemoteSocketAddress()});
        
        try {
            while (running) {
                // Gelen mesajı bekle
                Message msg = IOUtil.readMessage(in);
                
                // Bağlantı kapatıldıysa çık
                if (msg == null) {
                    LOGGER.log(Level.INFO, "Client {0} disconnected (EOF)", id);
                    break;
                }
                
                LOGGER.log(Level.INFO, "Received from client {0}: {1}#{2}", new Object[]{id, msg.type(), msg.payload()});
                
                // Mesaj tipine göre işle
                processMessage(msg);
            }
        } catch (SocketException se) {
            LOGGER.log(Level.INFO, "Client {0} connection reset: {1}", new Object[]{id, se.getMessage()});
        } catch (IOException e) {
            if (running) {
                LOGGER.log(Level.WARNING, "Client " + id + " I/O error", e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing client " + id + " message", e);
        } finally {
            try {
                // İstemci oturuma bağlıysa, bağlantı kopma bilgisi ilet
                if (session != null) {
                    session.handleDisconnect(this);
                }
                
                // İstemciyi sunucudan çıkar
                hub.removeClient(this);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during client cleanup", e);
            }
            
            // Soket bağlantısını kapat
            closeConnection();
        }
    }
    
    /**
     * Gelen mesajları işler
     */
    private void processMessage(Message msg) throws IOException {
        switch (msg.type()) {
            case MOVE:
                if (session != null) {
                    session.handleMove(this, msg.payload());
                } else {
                    send(new Message(Message.Type.ERROR, "Aktif bir oyun oturumunda değilsiniz"));
                }
                break;
                
            case PASS:
                if (session != null) {
                    session.handlePass(this);
                } else {
                    send(new Message(Message.Type.ERROR, "Aktif bir oyun oturumunda değilsiniz"));
                }
                break;
                
            case RESIGN:
                if (session != null) {
                    session.handleResign(this);
                } else {
                    send(new Message(Message.Type.ERROR, "Aktif bir oyun oturumunda değilsiniz"));
                }
                break;
                
            case READY_FOR_GAME:
                // Yeni oyun için hazır işareti
                if (!inGame) {
                    hub.addToWaitingQueue(this);
                    hub.checkAndCreateMatch();
                }
                break;
                
            case CHAT_BROADCAST:
            case TO_CLIENT:
                // Sohbet mesajı
                if (session != null) {
                    session.handleChat(this, msg.payload());
                } else if (msg.payload().contains(",")) {
                    // Direkt mesaj formatı: "targetId,message"
                    String[] parts = msg.payload().split(",", 2);
                    try {
                        int targetId = Integer.parseInt(parts[0]);
                        String text = parts.length > 1 ? parts[1] : "";
                        hub.sendToClient(targetId, this.id + ": " + text);
                    } catch (NumberFormatException e) {
                        send(new Message(Message.Type.ERROR, "Geçersiz hedef ID"));
                    }
                } else {
                    // Genel sohbet mesajı veya hatalı format
                    send(new Message(Message.Type.ERROR, "Mesaj iletilemedi. Aktif bir oyunda değilsiniz."));
                }
                break;
                
            default:
                LOGGER.log(Level.WARNING, "Unsupported message type from client {0}: {1}", new Object[]{id, msg.type()});
                send(new Message(Message.Type.ERROR, "Desteklenmeyen mesaj tipi: " + msg.type()));
        }
    }
}
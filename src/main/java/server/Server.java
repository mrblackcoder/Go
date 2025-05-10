
// --- File: server/Server.java ---
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
    private int idSeq = 0;
    private SClient waiting = null;

    public Server(int port) throws IOException { serverSock = new ServerSocket(port); }
    public void shutdown() throws IOException { serverSock.close(); }
    public int nextId() { return idSeq++; }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    Socket s = serverSock.accept();
                    SClient c = new SClient(s, this);
                    clients.add(c); c.start();
                    broadcastClientIds();
                    if (waiting == null) waiting = c;
                    else {
                        new GameSession(waiting, c);
                        waiting = null;
                    }
                }
            } catch (IOException e) {
                System.out.println("Server shut down");
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    private void broadcastClientIds() throws IOException {
        String ids = clients.stream().map(c -> String.valueOf(c.id)).collect(Collectors.joining(","));
        Message m = new Message(Message.Type.CLIENT_IDS, ids);
        for (SClient c : clients) c.send(m);
    }

    void sendToClient(int targetId, String text) throws IOException {
        for (SClient c : clients) if (c.id == targetId) { c.send(new Message(Message.Type.MSG_FROM_CLIENT, targetId+","+text)); break; }
    }

    void removeClient(SClient c) {
        clients.remove(c);
        try { broadcastClientIds(); } catch (IOException ignored) {}
    }
}

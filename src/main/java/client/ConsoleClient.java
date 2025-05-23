package client;

import common.IOUtil;
import common.Message;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ConsoleClient {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        Socket socket = new Socket(host, port);
        System.out.println("Connected to " + host + ":" + port);

        InputStream in = socket.getInputStream();
        new Thread(() -> {
            try {
                while (true) {
                    Message m = IOUtil.readMessage(in);
                    if (m == null) {
                        break;
                    }
                    System.out.printf("RECV ▶ %s#%s%n", m.type(), m.payload());
                }
            } catch (Exception e) {
                System.out.println("Connection closed.");
            }
        }).start();

        OutputStream out = socket.getOutputStream();
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter commands like MOVE#3,3 or PASS#");
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("#", 2);
            try {
                Message.Type t = Message.Type.valueOf(parts[0]);
                String payload = parts.length > 1 ? parts[1] : "";
                IOUtil.writeMessage(out, new Message(t, payload));
            } catch (IllegalArgumentException e) {
                System.out.println("Geçersiz komut tipi");
            }
        }
    }
}

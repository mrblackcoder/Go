package client;

import common.Message;

public class TestClient {
    public static void main(String[] args) throws Exception {
        CClient c = new CClient(args.length>0?args[0]:"51.20.252.52", // Varsayılan sunucu adresi güncellendi
                               args.length>1?Integer.parseInt(args[1]):5000, // Port 5000 olarak güncellendi
                               null);
        c.start();
        java.util.Scanner sc = new java.util.Scanner(System.in);
        System.out.println("Komut girin: MOVE#x,y  PASS# veya TO_CLIENT#id,mesaj");
        while (sc.hasNextLine()) {
            var line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            var parts = line.split("#",2);
            try {
                var t = Message.Type.valueOf(parts[0]);
                var payload = parts.length>1?parts[1]:"";
                c.send(new Message(t, payload));
            } catch(Exception e){ System.out.println("Geçersiz komut"); }
        }
    }
}
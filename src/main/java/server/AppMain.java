package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AppMain {
    public static void main(String[] args) throws Exception {
        int port = args.length>0?Integer.parseInt(args[0]):5000; // Port 5000 olarak güncellendi
        Server srv = new Server(port);
        srv.start();
        System.out.printf("Go sunucusu %d portunda dinliyor…%n", port);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (!"q".equalsIgnoreCase(br.readLine())) {}
        srv.shutdown();
        System.out.println("Sunucu kapatıldı.");
    }
}
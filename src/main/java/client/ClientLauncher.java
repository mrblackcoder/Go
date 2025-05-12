package client;

import javax.swing.SwingUtilities;

public class ClientLauncher {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "51.20.252.52";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        SwingUtilities.invokeLater(() ->
            new MainFrm(host, port).setVisible(true)
        );
    }
}
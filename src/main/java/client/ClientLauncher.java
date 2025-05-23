package client;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class ClientLauncher {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        
        int boardSize = 19;
        String playerName = "Player";
        
        if (args.length > 2) {
            try {
                boardSize = Integer.parseInt(args[2]);
                if (boardSize != 9 && boardSize != 13 && boardSize != 19) {
                    System.out.println("Invalid board size. Using default (19).");
                    boardSize = 19;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid board size format. Using default (19).");
            }
        }
        
        if (args.length > 3) {
            playerName = args[3];
        } else {
            String input = JOptionPane.showInputDialog(null, 
                "Enter your name:", "Player Name", JOptionPane.QUESTION_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                playerName = input.trim();
            }
        }
        
        final String finalHost = host;
        final int finalPort = port;
        final int finalBoardSize = boardSize;
        final String finalPlayerName = playerName;
        
        SwingUtilities.invokeLater(() ->
            new MainFrm(finalHost, finalPort).setVisible(true)
        );
    }
}
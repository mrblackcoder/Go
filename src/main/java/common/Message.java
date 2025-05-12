package common;

import java.io.Serializable;

public record Message(Type type, String payload) implements Serializable {
    public enum Type {
        MOVE, PASS, RESIGN, CHAT_BROADCAST, TO_CLIENT, ERROR, 
        CLIENT_IDS, MSG_FROM_CLIENT, ROLE, BOARD_STATE, SCORE, GAME_OVER,
        // Yeni mesaj tipleri
        TIMER_UPDATE,   // Zamanı güncellemek için (payload: "blackTime,whiteTime")
        
        READY_FOR_GAME  // Oyuncunun yeni oyuna hazır olduğunu belirtmek için
    }
}
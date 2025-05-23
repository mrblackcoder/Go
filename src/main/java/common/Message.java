package common;

import java.io.Serializable;

public record Message(Type type, String payload) implements Serializable {
    public enum Type {
        MOVE, PASS, RESIGN, CHAT_BROADCAST, TO_CLIENT, ERROR, 
        CLIENT_IDS, MSG_FROM_CLIENT, ROLE, BOARD_STATE, SCORE, GAME_OVER,
        TIMER_UPDATE, READY_FOR_GAME,
        PING, PONG,           // Bağlantı kontrol mesajları
        UNDO_MOVE,            // Hamle geri alma (payload: "")
        TO_SERVER,
        GAME_CONFIG           // Oyun konfigürasyonu (payload: "boardSize,handicap,komi")
    }
}
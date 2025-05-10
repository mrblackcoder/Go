package common;

import java.io.Serializable;

public record Message(Type type, String payload) implements Serializable {
    public enum Type {
        CLIENT_IDS,
        TO_CLIENT,       // payload: "targetId,message" (Direkt mesaj için?)
        MSG_FROM_CLIENT, // payload: sunucudan istemciye gelen mesaj ("fromId,message" veya sadece "message")
        BOARD_STATE,
        ROLE,
        SCORE,
        GAME_OVER,
        MOVE,
        PASS,
        RESIGN,
        CHAT_BROADCAST, // İstemciden sunucuya genel sohbet mesajı
        ERROR
    }
}
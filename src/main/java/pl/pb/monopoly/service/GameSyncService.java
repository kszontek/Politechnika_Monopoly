package pl.pb.monopoly.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.pb.monopoly.dto.GameStateDto;

// Rozsylanie stanu gry przez WebSocket (STOMP) - dzieki temu po ruchu jednego gracza
// plansza odswieza sie u wszystkich w pokoju bez przeladowania strony.
@Service
public class GameSyncService {

    private final SimpMessagingTemplate messagingTemplate;

    public GameSyncService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // emotki/reakcje leca osobnym kanalem, zeby nie mieszac ich z normalnym stanem gry
    public void broadcastReaction(Long sessionId, Object payload) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId + "/reactions", payload);
    }
    // pelny stan gry na temat pokoju - kazdy subskrybent (gracz) dostaje swiezy GameStateDto
    public void broadcast(Long sessionId, GameStateDto state) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId, state);
    }
}

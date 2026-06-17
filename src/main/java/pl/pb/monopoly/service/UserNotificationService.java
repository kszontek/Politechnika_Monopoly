package pl.pb.monopoly.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.pb.monopoly.dto.GameInviteNotificationDto;

// Powiadomienia "do konkretnego usera" przez WebSocket - np. wyskakujacy toast z zaproszeniem do gry.
// Inaczej niz GameSyncService (ten leci do calego pokoju), tu kanal jest per login.
@Service
public class UserNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public UserNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // wysylka zaproszenia do gry na prywatny kanal zapraszanego
    public void sendGameInvite(String inviteeUsername, GameInviteNotificationDto notification) {
        messagingTemplate.convertAndSend("/topic/user/" + inviteeUsername, notification);
    }
}

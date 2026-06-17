package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.Achievement;
import pl.pb.monopoly.domain.AchievementType;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.AchievementRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

// Osiagniecia - sprawdza progi (rozegrane gry, wygrane, seria) i przyznaje nowe odznaki + monety za nie.
@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;

    public AchievementService(AchievementRepository achievementRepository, UserRepository userRepository) {
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
    }

    // lecimy po wszystkich typach osiagniec i dajemy te, ktore user wlasnie odblokowal a jeszcze ich nie ma.
    // wolane np. przy wejsciu na dashboard, wiec musi pomijac juz przyznane (existsBy...).
    @Transactional
    public List<AchievementType> checkAndAward(Long userId) {
        if (userId == null) return List.of();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        PlayerStatistics s = user.getStatistics();
        if (s == null) return List.of();
        List<AchievementType> newly = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            if (type.isUnlocked(s.getGamesPlayed(), s.getGamesWon(), s.getWinStreak())
                    && !achievementRepository.existsByUserIdAndCode(user.getId(), type)) {
                achievementRepository.save(new Achievement(user, type));
                user.addCoins(type.coinReward);
                newly.add(type);
            }
        }
        return newly;
    }

    @Transactional(readOnly = true)
    public List<Achievement> forUser(Long userId) {
        return achievementRepository.findByUserIdOrderByUnlockedAtDesc(userId);
    }
}

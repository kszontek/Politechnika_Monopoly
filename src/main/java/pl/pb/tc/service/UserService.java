package pl.pb.tc.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.tc.domain.Role;
import pl.pb.tc.domain.User;
import pl.pb.tc.domain.UserProfile;
import pl.pb.tc.dto.RankingEntryDto;
import pl.pb.tc.dto.RegistrationForm;
import pl.pb.tc.repository.UserRepository;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User register(RegistrationForm form) {
        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setAge(form.getAge());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.USER);
        user.attachProfile(new UserProfile());
        return userRepository.save(user);
    }

    public List<RankingEntryDto> topPlayers() {
        return List.of(
                new RankingEntryDto(1, "admin", 22, 2400, 41),
                new RankingEntryDto(2, "moderator", 16, 1850, 28),
                new RankingEntryDto(3, "gracz", 12, 1420, 23),
                new RankingEntryDto(4, "kowalski", 10, 1280, 17),
                new RankingEntryDto(5, "nowak", 8, 1130, 12),
                new RankingEntryDto(6, "wisniewski", 6, 980, 7)
        );
    }
}

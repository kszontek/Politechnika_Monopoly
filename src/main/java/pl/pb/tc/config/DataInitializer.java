package pl.pb.tc.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.pb.tc.domain.*;
import pl.pb.tc.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                users.save(buildUser("admin", "admin@pb.edu.pl", 30, Role.ADMIN, encoder));
                users.save(buildUser("moderator", "mod@pb.edu.pl", 28, Role.MODERATOR, encoder));
                users.save(buildUser("gracz", "gracz@pb.edu.pl", 21, Role.USER, encoder));
            }
        };
    }

    private User buildUser(String login, String email, int age, Role role, PasswordEncoder encoder) {
        User u = new User();
        u.setUsername(login);
        u.setEmail(email);
        u.setAge(age);
        u.setRole(role);
        u.setPassword(encoder.encode(login + "123"));
        UserProfile profile = new UserProfile();
        profile.setLevel(role == Role.ADMIN ? 22 : role == Role.MODERATOR ? 16 : 12);
        profile.setEloPoints(role == Role.ADMIN ? 2400 : role == Role.MODERATOR ? 1850 : 1420);
        profile.setGamesWon(role == Role.ADMIN ? 41 : role == Role.MODERATOR ? 28 : 23);
        u.attachProfile(profile);
        return u;
    }
}

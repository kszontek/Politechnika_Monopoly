package pl.pb.monopoly.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.repository.UserRepository;

import java.util.List;

// Most miedzy naszym User z bazy a Spring Security - mowi frameworkowi jak zalogowac usera.
// Spring sam porownuje hasla (BCrypt), my tylko dostarczamy login, haslo, role i info czy konto zablokowane.
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ModerationService moderationService;

    public CustomUserDetailsService(UserRepository userRepository,
                                    ModerationService moderationService) {
        this.userRepository = userRepository;
        this.moderationService = moderationService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        pl.pb.monopoly.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono uzytkownika: " + username));

        // accountLocked = zawieszony user nie zaloguje sie, dopoki ban nie minie
        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().authority())))
                .accountLocked(moderationService.isUserSuspended(user))
                .build();
    }
}

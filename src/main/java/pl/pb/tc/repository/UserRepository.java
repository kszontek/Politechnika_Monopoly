package pl.pb.tc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.tc.domain.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}

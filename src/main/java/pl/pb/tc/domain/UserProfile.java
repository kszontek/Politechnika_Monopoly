package pl.pb.tc.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(1)
    @Column(nullable = false)
    private int level = 1;

    @Min(0)
    @Column(name = "elo_points", nullable = false)
    private int eloPoints = 1000;

    @Min(0)
    @Column(name = "games_won", nullable = false)
    private int gamesWon = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getEloPoints() { return eloPoints; }
    public void setEloPoints(int eloPoints) { this.eloPoints = eloPoints; }
    public int getGamesWon() { return gamesWon; }
    public void setGamesWon(int gamesWon) { this.gamesWon = gamesWon; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}

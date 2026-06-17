package pl.pb.monopoly.domain;

// Etap zycia partii: WAITING (lobby, czekamy na graczy) -> ACTIVE (gramy) -> FINISHED (koniec).
public enum GameStatus {

    WAITING,

    ACTIVE,

    FINISHED
}

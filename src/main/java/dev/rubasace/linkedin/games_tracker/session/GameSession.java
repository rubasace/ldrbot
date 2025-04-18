package dev.rubasace.linkedin.games_tracker.session;

import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@Setter
@Getter
@Entity
public class GameSession {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    private TelegramUser telegramUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType game;

    @Column(nullable = false)
    private Duration duration;

    @Column(nullable = false)
    private LocalDate gameDay;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GameSession that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
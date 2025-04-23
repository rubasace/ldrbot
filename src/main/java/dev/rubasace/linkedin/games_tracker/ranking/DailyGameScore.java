package dev.rubasace.linkedin.games_tracker.ranking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.rubasace.linkedin.games_tracker.group.TelegramGroup;
import dev.rubasace.linkedin.games_tracker.session.GameSession;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Setter
@Getter
@Table
@Entity
public class DailyGameScore {

    @GeneratedValue
    @Id
    private UUID id;

    @JsonIgnoreProperties({"scores", "members"})
    @ManyToOne(optional = false)
    private TelegramGroup group;

    @ManyToOne(optional = false)
    private TelegramUser user;

    @Enumerated(EnumType.STRING)
    private GameType game;

    private LocalDate gameDay;

    @JsonIgnoreProperties("dailyGameScore")
    @OneToOne(optional = false)
    @JoinColumn(name = "game_session_id", nullable = false, unique = true)
    private GameSession gameSession;

    private int position;

    private int points;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DailyGameScore that = (DailyGameScore) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(user, that.user) &&
                game == that.game &&
                Objects.equals(gameDay, that.gameDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, user, game, gameDay);
    }

}

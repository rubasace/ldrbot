package dev.rubasace.linkedin.games.ldrbot.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
public class TelegramGroup {

    @Id
    private Long chatId;

    private String groupName;

    @Column(nullable = false)
    private ZoneId timezone;

    //TODO when we allow to change them, we will delete the associated gameScores to the non-tracked games for the given day, so the leaderboard check at the end of the day is correct
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "tracked_game")
    @CollectionTable(
            name = "telegram_group_tracked_games",
            joinColumns = @JoinColumn(name = "group_id")
    )
    private Set<GameType> trackedGames = EnumSet.allOf(GameType.class);

    @JsonIgnoreProperties({"group", "gameSession"})
    @OneToMany(mappedBy = "group")
    private Set<DailyGameScore> scores;

    @JsonIgnoreProperties({"groups", "sessions"})
    @ManyToMany
    Set<TelegramUser> members = new HashSet<>();

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TelegramGroup that = (TelegramGroup) o;
        return Objects.equals(chatId, that.chatId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(chatId);
    }
}

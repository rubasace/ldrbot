package dev.rubasace.linkedin.games_tracker.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;
import java.util.EnumSet;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name = "tracked_game")
    @CollectionTable(name = "telegram_group_tracked_games", joinColumns = @JoinColumn(name = "group_id"))
    private Set<GameType> trackedGames = EnumSet.allOf(GameType.class);

    @JsonIgnoreProperties("groups")
    @ManyToMany
    Set<TelegramUser> members;

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

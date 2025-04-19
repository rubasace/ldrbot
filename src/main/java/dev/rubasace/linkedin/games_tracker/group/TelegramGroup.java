package dev.rubasace.linkedin.games_tracker.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.rubasace.linkedin.games_tracker.session.GameType;
import dev.rubasace.linkedin.games_tracker.user.TelegramUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;
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

    //TODO make configurable
    @Transient
    private final Set<GameType> trackedGames = Set.of(GameType.values());

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

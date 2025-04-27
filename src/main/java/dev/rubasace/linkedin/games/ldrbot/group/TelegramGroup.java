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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@Setter
@Getter
@Entity
public class TelegramGroup {

    private static final String DEFAULT_ZONE = "Europe/Madrid";

    @Id
    private Long chatId;

    private String groupName;

    @Column(nullable = false)
    private ZoneId timezone = ZoneId.of(DEFAULT_ZONE);

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
    private Set<DailyGameScore> scores = new HashSet<>();

    @JsonIgnoreProperties({"groups", "sessions"})
    @ManyToMany
    Set<TelegramUser> members = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    public TelegramGroup(final Long chatId, final String groupName) {
        this.chatId = chatId;
        this.groupName = groupName;
    }

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

package dev.rubasace.linkedin.games.ldrbot.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.rubasace.linkedin.games.ldrbot.ranking.DailyGameScore;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import jakarta.persistence.CascadeType;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor
@Setter
@Getter
@Entity
public class TelegramGroup {

    private static final String DEFAULT_ZONE = "Europe/Madrid";

    @Id
    private Long chatId;

    private String uuid;

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

    // LAZY (the default) is load-bearing: the group is loaded on every inbound group message, while a participation
    // question is only asked on the ranking, reminder and toggle paths. The cascade is deliberately narrow — PERSIST
    // and MERGE are what carry a new period through telegramGroupRepository.save(...) on this inverse association, and
    // the ABSENCE of orphanRemoval (and of REMOVE) is what makes "no path deletes a period" a property of the mapping
    // rather than a matter of discipline.
    @JsonIgnoreProperties({"group"})
    @OneToMany(mappedBy = "group", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<ParkedPeriod> parkedPeriods = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, name = "read_from_messages")
    private boolean readFromMessages = true;

    public TelegramGroup(final Long chatId, final String groupName) {
        this.chatId = chatId;
        this.groupName = groupName;
    }

    /**
     * Was {@code userId} parked on {@code gameDay}? Start inclusive, end exclusive, an open period unbounded above, and
     * existential — one matching period is enough, so overlapping periods are indistinguishable from one.
     * <p>
     * This is the only question any calculation is allowed to ask about participation. "Is this player parked now?" is
     * the same question asked at {@code todayGameDay()}, never a second rule.
     */
    boolean isParkedOn(final Long userId, final LocalDate gameDay) {
        return parkedPeriods.stream()
                            .anyMatch(parkedPeriod -> parkedPeriod.getUserId().equals(userId) && parkedPeriod.covers(gameDay));
    }

    /**
     * The members taking part on {@code gameDay}. {@link #getMembers()} keeps its meaning — the full membership — and
     * every caller that wants the day-scoped set opts in by name <em>and</em> by day. There is deliberately no
     * no-argument overload: it would make "now" the default and let a past day be computed against today's set by
     * omission.
     */
    public Set<TelegramUser> getParticipatingMembers(final LocalDate gameDay) {
        return members.stream()
                      .filter(member -> !isParkedOn(member.getId(), gameDay))
                      .collect(Collectors.toSet());
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

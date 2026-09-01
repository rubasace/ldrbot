package dev.rubasace.linkedin.games.ldrbot.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One stretch of game days during which a player did not take part in a group's daily competition.
 * <p>
 * {@code startGameDay} is inclusive, {@code endGameDay} is exclusive, and a null {@code endGameDay} means the period is
 * still open. Both boundaries are LinkedIn-zone game days
 * ({@link dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils#todayGameDay()}), never a group-local date, because
 * they are compared against {@link dev.rubasace.linkedin.games.ldrbot.session.GameSession#getGameDay()}.
 * <p>
 * Deliberately carries no {@code equals}/{@code hashCode}: the generated id is only assigned at flush, so keying either
 * on it would mutate the hash of an element already inside {@link TelegramGroup#getParkedPeriods()}. Identity semantics
 * are correct for a row that is never compared, looked up or de-duplicated by content.
 * <p>
 * There is deliberately no repository for this entity. Periods live inside the group aggregate and are reached only
 * through {@link TelegramGroup#getParkedPeriods()}; the {@code PERSIST}/{@code MERGE} cascade declared there is what
 * writes them.
 */
@NoArgsConstructor
@Setter
@Getter
@Table
@Entity
public class ParkedPeriod {

    @GeneratedValue
    @Id
    private UUID id;

    @JsonIgnoreProperties({"scores", "members", "parkedPeriods"})
    @ManyToOne(optional = false)
    @JoinColumn(name = "group_chat_id", nullable = false)
    private TelegramGroup group;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_game_day", nullable = false)
    private LocalDate startGameDay;

    // Nullable on purpose: null is the "still open" marker the day-scoped predicate reads. It must never gain a default.
    @Column(name = "end_game_day")
    private LocalDate endGameDay;

    public ParkedPeriod(final TelegramGroup group, final Long userId, final LocalDate startGameDay) {
        this.group = group;
        this.userId = userId;
        this.startGameDay = startGameDay;
    }

    /**
     * The day-scoped predicate, and its only home in Java: start inclusive, end exclusive, an open period unbounded
     * above. {@link TelegramGroup#isParkedOn(Long, java.time.LocalDate)} delegates here and these two comparisons appear
     * nowhere else in Java.
     */
    boolean covers(final LocalDate gameDay) {
        return !startGameDay.isAfter(gameDay) && (endGameDay == null || gameDay.isBefore(endGameDay));
    }

    boolean isOpen() {
        return endGameDay == null;
    }
}

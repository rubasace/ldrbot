package dev.rubasace.linkedin.games_tracker.ranking;

import dev.rubasace.linkedin.games_tracker.summary.GroupDailyScore;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GroupDailyScoreCreatedEvent extends ApplicationEvent {

    private final GroupDailyScore groupDailyScore;

    public GroupDailyScoreCreatedEvent(final Object source, GroupDailyScore groupDailyScore) {
        super(source);
        this.groupDailyScore = groupDailyScore;
    }
}

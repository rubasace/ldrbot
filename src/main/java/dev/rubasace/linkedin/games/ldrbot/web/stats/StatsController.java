package dev.rubasace.linkedin.games.ldrbot.web.stats;

import dev.rubasace.linkedin.games.ldrbot.group.GroupNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/stats")
@RestController
class StatsController {

    private final StatsService statsService;

    StatsController(final StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/{groupId}")
    GroupStats getStats(@PathVariable String groupId) throws GroupNotFoundException {
        return statsService.getStats(groupId);
    }

}

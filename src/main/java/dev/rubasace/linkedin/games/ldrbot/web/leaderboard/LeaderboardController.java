package dev.rubasace.linkedin.games.ldrbot.web.leaderboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/leaderboard")
@RestController
class LeaderboardController {

    private final LeaderboardService leaderboardService;

    LeaderboardController(final LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }


    @GetMapping("/{groupId}")
    Leaderboard getDashboard(@PathVariable String groupId) {
        return leaderboardService.getLeaderboard(groupId, null, null);
    }
}

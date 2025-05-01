package dev.rubasace.linkedin.games.ldrbot.web.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/dashboard")
@RestController()
class DashboardController {

    private final DashboardService dashboardService;

    DashboardController(final DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{groupId}")
    GroupData getDashboard(@PathVariable Long groupId) {
        return dashboardService.getGroupData(groupId);
    }
}

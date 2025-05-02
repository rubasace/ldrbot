package dev.rubasace.linkedin.games.ldrbot.web.group;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/group")
@RestController()
class GroupController {

    private final GroupService groupService;

    GroupController(final GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/{groupId}")
    GroupData getDashboard(@PathVariable Long groupId) {
        return groupService.getGroupData(groupId);
    }
}

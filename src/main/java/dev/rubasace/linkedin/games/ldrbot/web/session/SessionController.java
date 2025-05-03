package dev.rubasace.linkedin.games.ldrbot.web.session;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/sessions")
@RestController
class SessionController {

    private final SessionService sessionService;

    SessionController(final SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/{groupId}")
    List<Session> getSessions(@PathVariable String groupId) {
        return sessionService.getSessions(groupId);
    }
}

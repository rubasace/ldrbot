package dev.rubasace.linkedin.games.ldrbot.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
@Controller
public class WebController {

    @GetMapping({
            "/{path:[^\\.]*}",
            "/{path:^(?!api$).*$}/**/{subpath:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
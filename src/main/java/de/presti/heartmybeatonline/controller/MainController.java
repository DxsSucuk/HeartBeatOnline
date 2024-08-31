package de.presti.heartmybeatonline.controller;

import de.presti.heartmybeatonline.Server;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    /**
     * Redirect to the main HTML page
     *
     * @return the HTML page
     */
    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("last", Server.getInstance().getLastBeat());
        model.addAttribute("board", Server.getInstance().getLeaderboard());
        model.addAttribute("boardAllTime", Server.getInstance().getLeaderboardAllTime());
        return "index";
    }
}

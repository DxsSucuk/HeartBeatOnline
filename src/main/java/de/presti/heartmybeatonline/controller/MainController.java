package de.presti.heartmybeatonline.controller;

import de.presti.heartmybeatonline.Server;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    /**
     * Redirect to the main HTML page
     *
     * @return the HTML page
     */
    @RequestMapping("/")
    public String index(Model model, @CookieValue(value = "userId", defaultValue = "RAWRXD") String userId, HttpServletResponse response) {
        model.addAttribute("last", Server.getInstance().getLastBeat());
        model.addAttribute("board", Server.getInstance().getLeaderboardOfToday());
        model.addAttribute("boardAllTime", Server.getInstance().getLeaderboardOfAllTime());
        model.addAttribute("nextPull", Server.getInstance().getNextPull());
        model.addAttribute("gambler", Server.getInstance().getGambler(userId, response));
        return "index";
    }
}

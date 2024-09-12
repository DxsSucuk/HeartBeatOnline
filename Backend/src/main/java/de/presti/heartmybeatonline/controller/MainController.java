package de.presti.heartmybeatonline.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.presti.heartmybeatonline.Server;
import de.presti.heartmybeatonline.controller.response.*;
import de.presti.heartmybeatonline.dto.Gambler;
import de.presti.heartmybeatonline.dto.GamblerSafe;
import de.presti.heartmybeatonline.dto.HeartBeat;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
public class MainController {

    /**
     * Redirect to the main HTML page
     *
     * @return the HTML page
     */
    @RequestMapping("/")
    public String index(Model model, @CookieValue(value = "userId", defaultValue = "RAWRXD") String userId) {
        model.addAttribute("last", Server.getInstance().getLastBeat());
        model.addAttribute("board", Server.getInstance().getLeaderboardOfToday());
        model.addAttribute("boardAllTime", Server.getInstance().getLeaderboardOfAllTime());
        model.addAttribute("nextPull", Server.getInstance().getNextPull());
        model.addAttribute("gambler", Server.getInstance().getGambler(userId));
        return "index";
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RequestMapping("/leaderboard")
    public HeartLeaderboardList getLeaderboard(@RequestParam(value = "typ", defaultValue = "day") String typ) {
        HeartLeaderboardList list = new HeartLeaderboardList();
        if (typ.equals("day")) {
            list.data = Server.getInstance().getLeaderboardOfToday();
        } else {
            list.data = Server.getInstance().getLeaderboardOfAllTime();
        }
        list.success = true;
        return list;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RequestMapping("/next")
    public ResponseBase<String> getNext() {
        ResponseBase<String> response = new ResponseBase<>();
        response.data = Server.getInstance().getNextPull();
        response.success = true;
        return response;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RequestMapping("/heartbeat")
    public HeartBeatResponse getHeartbeat() {
        HeartBeatResponse heartBeat = new HeartBeatResponse();
        heartBeat.data = Server.getInstance().getLastBeat();
        heartBeat.success = Server.getInstance().getLastBeat() != null;
        return heartBeat;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RequestMapping("/gambleboard")
    public GambleLeaderboardList getGamblers() {
        GambleLeaderboardList gamblerResponse = new GambleLeaderboardList();
        gamblerResponse.data = Server.getInstance().getGamblers();
        gamblerResponse.success = true;
        return gamblerResponse;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RequestMapping("/gambler")
    public GamblerResponse getGambler(@RequestParam(value = "id", defaultValue = "RAWRXD") String userId, HttpServletResponse response) {
        GamblerResponse gamblerResponse = new GamblerResponse();
        Gambler gambler = Server.getInstance().getGambler(userId);
        gamblerResponse.success = gambler != null;
        gamblerResponse.data = GamblerSafe.fromGambler(gambler);
        return gamblerResponse;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RequestMapping("/gambler/create")
    public GamblerUnsafeResponse createGambler(@CookieValue(value = "AuthenticKDasDAS_W", required = false) String cookie, HttpServletResponse response) {
        GamblerUnsafeResponse gamblerResponse = new GamblerUnsafeResponse();
        gamblerResponse.success = false;
        /*try {
            if (cookie == null) {
                response.addCookie(new Cookie("AuthenticKDasDAS_W", "RAWRWARAFWAAGWWTwafdwagagawgaGAWGWAHAWHSD"));
            } else {
                return gamblerResponse;
            }
        } catch (Exception e) {
            return gamblerResponse;
        }*/

        gamblerResponse.success = true;

        gamblerResponse.data = Server.getInstance().createGambler();
        return gamblerResponse;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RequestMapping("/gamble")
    public GambleResponse gamble(@RequestParam(value = "id", defaultValue = "RAWRXD") String userId, @RequestParam(value = "beat", defaultValue = "60") int beat, @RequestParam(value = "amount", defaultValue = "0") int amount, @RequestParam(value ="token", defaultValue = "RAWRXD") String token) {
        GambleResponse gambleResponse = new GambleResponse();
        gambleResponse.success = Server.getInstance().gambleMoney(userId, beat, amount, token);
        gambleResponse.message = "Broke bitch!";
        return gambleResponse;
    }
}

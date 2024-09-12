package de.presti.heartmybeatonline.controller;

import de.presti.heartmybeatonline.Server;
import de.presti.heartmybeatonline.controller.response.*;
import de.presti.heartmybeatonline.dto.Gambler;
import de.presti.heartmybeatonline.dto.GamblerSafe;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class MainController {

    /**
     * Redirect to the main HTML page
     *
     * @return the HTML page
     */
    @RequestMapping("/")
    public ResponseBase<String> index() {
        ResponseBase<String> response = new ResponseBase<>();
        response.success = true;
        response.message = "Error feet pics soon or something dunno.";
        return response;
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
    public GamblerUnsafeResponse createGambler(@CookieValue(value = "AuthenticKDasDAS_W", required = false) String cookie, HttpServletResponse response, HttpServletRequest request) {
        GamblerUnsafeResponse gamblerResponse = new GamblerUnsafeResponse();
        gamblerResponse.success = false;
        String ip = getClientIpAddress(request);
        String ipHash = Server.getInstance().createHash(ip);
        log.info("Creating gambler");
        log.info("IP: {}", ip);
        log.info("Cookie: {}", cookie);

        if (Server.getInstance().getNoCreateList().contains(ipHash)) {
            gamblerResponse.message = "You are banned from creating a gambler.";
            return gamblerResponse;
        } else {
            Server.getInstance().getNoCreateList().add(ipHash);
        }

        try {
            if (cookie == null) {
                response.addCookie(new Cookie("AuthenticKDasDAS_W", "RAWRWARAFWAAGWWTwafdwagagawgaGAWGWAHAWHSD"));
            } else {
                return gamblerResponse;
            }
        } catch (Exception e) {
            return gamblerResponse;
        }

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

    private static final String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "CF-Connecting-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"};

    public static String getClientIpAddress(HttpServletRequest request) {
        for (String header : HEADERS_TO_TRY) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        log.warn("Couldn't find IP from header, will instead use remote address.");
        return request.getRemoteAddr();
    }
}

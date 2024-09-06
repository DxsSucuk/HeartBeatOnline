package de.presti.heartmybeatonline;

import com.google.gson.*;
import de.presti.heartmybeatonline.dto.Gambler;
import de.presti.heartmybeatonline.dto.HeartBeat;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
public class Server {

    @Getter
    private static Server instance;

    @Setter
    @Getter
    private String authToken;

    @Getter
    private String lastToken;

    @Getter
    private final int minutesToWait = 5, leaderboardSize = 3, defaultMoney = 1000, minimumBet = 100;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    private final List<HeartBeat> beatsOfToday = new java.util.ArrayList<>();

    @Getter
    private final List<HeartBeat> leaderboardOfToday = new java.util.ArrayList<>();

    @Getter
    private final List<HeartBeat> leaderboardOfAllTime = new java.util.ArrayList<>();

    private ZonedDateTime nextPull;

    private final Comparator<HeartBeat> comparator = (o1, o2) -> Double.compare(o2.beat, o1.beat);

    // Threads.
    Thread amIAliveOrSomething;
    Thread resetLeaderboard;

    public Server() {
        if (instance != null) {
            throw new IllegalStateException("Server already initialized");
        }

        instance = this;

        try {
            Files.createDirectory(Path.of("data", "gambler"));
        } catch (IOException e) {
            log.warn("Could not create directory gambler!");
        }

        loadAllTimeLeaderboard();

        resetLeaderboard = new Thread(() -> {
            while (resetLeaderboard != null && !resetLeaderboard.isInterrupted()) {
                ZonedDateTime now = ZonedDateTime.now();
                if (now.getHour() == 0 && now.getMinute() == 0) {
                    lastToken = null;
                    leaderboardOfToday.clear();
                }

                try {
                    Thread.sleep(Duration.ofMinutes(1).toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        resetLeaderboard.start();

        amIAliveOrSomething = new Thread(() -> {
            while (amIAliveOrSomething != null && !amIAliveOrSomething.isInterrupted()) {
                beatsOfToday.clear();
                loadAllBeatsOfToday(lastToken);

                nextPull = ZonedDateTime.now().plusMinutes(minutesToWait);
                try {
                    Thread.sleep(Duration.ofMinutes(minutesToWait).toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        amIAliveOrSomething.start();
    }

    public void loadAllBeatsOfToday(String token) {
        lastToken = token;
        log.info("Loading beats of today with token {}", token);
        JsonObject apiObject = requestBeats(token, ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0), ZonedDateTime.now()).getAsJsonObject();
        if (apiObject.isJsonNull()) return;
        if (!apiObject.has("data")) return;

        JsonElement dataArray = apiObject.get("data");

        if (dataArray.isJsonArray() && !dataArray.getAsJsonArray().isEmpty()) {
            log.info("Found {} entries with token {}.", dataArray.getAsJsonArray().size(), token);
            for (JsonElement jsonElement : dataArray.getAsJsonArray()) {
                if (!jsonElement.isJsonObject()) continue;
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                HeartBeat beat = new HeartBeat();
                beat.beat = jsonObject.get("bpm").getAsDouble();
                beat.timestamp = jsonObject.get("timestamp").getAsString();
                beat.source = jsonObject.get("source").getAsString();
                addToBeatsOfToday(beat);
            }
        }

        if (apiObject.has("next_token") && !apiObject.get("next_token").isJsonNull()) {
            loadAllBeatsOfToday(apiObject.get("next_token").getAsString());
        }
    }

    public void addToBeatsOfToday(HeartBeat beat) {
        if (beatsOfToday.stream().anyMatch(heartBeat -> heartBeat.timestamp.equals(beat.timestamp))) return;

        if (leaderboardOfToday.isEmpty() || leaderboardOfToday.size() < leaderboardSize) {
            addToLeaderboard(beat);
        } else {
            boolean hasHigherValue = false;
            for (HeartBeat beatEntry : leaderboardOfToday) {
                if (beatEntry.beat < beat.beat) {
                    hasHigherValue = true;
                    break;
                }
            }

            if (hasHigherValue) {
                ArrayList<HeartBeat> previous = new ArrayList<>(leaderboardOfToday);
                addToLeaderboard(beat);
                leaderboardOfToday.stream().min(Comparator.comparingDouble(o -> o.beat)).ifPresent(leaderboardOfToday::remove);
                leaderboardOfToday.sort(comparator);
                onDailyLeaderboardUpdate(previous, leaderboardOfToday, beat);
            }
        }

        beatsOfToday.add(beat);
    }


    public void addToLeaderboard(HeartBeat heartBeat) {
        if (leaderboardOfToday.stream().noneMatch(beat -> beat.timestamp.equals(heartBeat.timestamp))) {
            leaderboardOfToday.add(heartBeat);
        }

        if (leaderboardOfAllTime.stream().anyMatch(beat -> beat.timestamp.equals(heartBeat.timestamp))) return;

        if (leaderboardOfAllTime.isEmpty() || leaderboardOfAllTime.size() < leaderboardSize) {
            leaderboardOfAllTime.add(heartBeat);
            saveAllTimeLeaderboard();
        } else {
            boolean hasHigherValue = false;
            for (HeartBeat beatEntry : leaderboardOfAllTime) {
                if (beatEntry.beat < heartBeat.beat) {
                    hasHigherValue = true;
                    break;
                }
            }

            if (hasHigherValue) {
                ArrayList<HeartBeat> previous = new ArrayList<>(leaderboardOfAllTime);
                leaderboardOfAllTime.add(heartBeat);
                leaderboardOfAllTime.stream().min(Comparator.comparingDouble(o -> o.beat)).ifPresent(leaderboardOfToday::remove);
                leaderboardOfAllTime.sort(comparator);
                onAllTimeLeaderboardUpdate(previous, leaderboardOfAllTime, heartBeat);
                saveAllTimeLeaderboard();
            }
        }
    }

    public void loadAllTimeLeaderboard() {
        Path pathToLeaderboard = Path.of("data", "leaderboard.json");
        if (Files.exists(pathToLeaderboard)) {
            try {
                String jsonString = Files.readString(pathToLeaderboard);
                JsonArray jsonArray = JsonParser.parseString(jsonString).getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    if (!element.isJsonObject()) continue;
                    JsonObject jsonObject = element.getAsJsonObject();
                    HeartBeat heartBeat = new HeartBeat();
                    heartBeat.beat = jsonObject.get("beat").getAsDouble();
                    heartBeat.timestamp = jsonObject.get("timestamp").getAsString();
                    heartBeat.source = jsonObject.get("source").getAsString();
                    leaderboardOfAllTime.add(heartBeat);
                }

                leaderboardOfAllTime.sort(comparator);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAllTimeLeaderboard() {
        JsonArray jsonArray = new JsonArray();
        leaderboardOfAllTime.forEach(heartBeat -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("beat", heartBeat.beat);
            jsonObject.addProperty("timestamp", heartBeat.timestamp);
            jsonObject.addProperty("source", heartBeat.source);
            jsonArray.add(jsonObject);
        });
        try {
            Files.writeString(Path.of("data", "leaderboard.json"), gson.toJson(jsonArray), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonElement requestBeats(String token, ZonedDateTime start, ZonedDateTime end) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url("https://api.ouraring.com/v2/usercollection/heartrate?start_datetime=" + start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+", "%2B") +
                        "&end_datetime=" + end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+", "%2B") + (token != null ? "&next_token=" + token : ""))
                .method("GET", null)
                .addHeader("Authorization", "Bearer " + getAuthToken())
                .build();
        try {
            Response response = client.newCall(request).execute();
            JsonElement apiObject = JsonParser.parseString(response.body().string());
            if (apiObject.isJsonObject() && apiObject.getAsJsonObject().has("data")) {
                return apiObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new JsonObject();
    }

    public void onDailyLeaderboardUpdate(List<HeartBeat> previous, List<HeartBeat> current, HeartBeat newEntry) {

    }

    public void onAllTimeLeaderboardUpdate(List<HeartBeat> previous, List<HeartBeat> current, HeartBeat newEntry) {

    }

    public HeartBeat getLastBeat() {
        if (beatsOfToday.isEmpty()) return null;
        return beatsOfToday.get(beatsOfToday.size() - 1);
    }

    public String getNextPull() {
        return nextPull != null ? nextPull.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
    }

    public Gambler getGambler(String id, HttpServletResponse response) {
        UUID uuid = UUID.randomUUID();
        try {
            uuid = UUID.fromString(id);
            final Path pathToGambler = Path.of("data", "gambler", uuid.toString() + ".json");
            if (Files.exists(pathToGambler)) {
                String jsonString = Files.readString(pathToGambler);
                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                return new Gambler(uuid, jsonObject.get("name").getAsString(), jsonObject.get("money").getAsInt(), jsonObject.get("wins").getAsInt());
            }
        } catch (Exception e) {
            log.error("Could not load gambler with id {}", id);
        }

        response.addCookie(new Cookie("userId", uuid.toString()));

        Gambler gambler = new Gambler(uuid, generateName(), defaultMoney, 0);

        saveGambler(gambler);

        return gambler;
    }

    public void saveGambler(Gambler gambler) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", gambler.name);
            jsonObject.addProperty("money", gambler.money);
            jsonObject.addProperty("wins", gambler.wins);
            Files.writeString(Path.of("data", "gambler", gambler.id.toString() + ".json"), gson.toJson(jsonObject), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Could not save gambler with id {}", gambler.id, e);
        }
    }

    public String generateName() {
        return "Gambler#" + new SecureRandom().nextInt(99999);
    }
}

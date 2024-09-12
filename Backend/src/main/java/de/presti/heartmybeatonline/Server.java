package de.presti.heartmybeatonline;

import com.google.gson.*;
import de.presti.heartmybeatonline.dto.Gambler;
import de.presti.heartmybeatonline.dto.GamblerSafe;
import de.presti.heartmybeatonline.dto.Gambles;
import de.presti.heartmybeatonline.dto.HeartBeat;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final int minutesToWait = 5, leaderboardSize = 3, defaultMoney = 1000, minimumBet = 1;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    private final List<HeartBeat> beatsOfToday = new java.util.ArrayList<>();

    @Getter
    private final List<HeartBeat> leaderboardOfToday = new java.util.ArrayList<>();

    @Getter
    private final List<HeartBeat> leaderboardOfAllTime = new java.util.ArrayList<>();

    @Getter
    private final List<Gambles> gamblesOfToday = new ArrayList<>();

    @Getter
    private final List<String> noCreateList = new ArrayList<>();

    private ZonedDateTime nextPull;

    private final Comparator<HeartBeat> comparator = (o1, o2) -> Double.compare(o2.beat, o1.beat);
    private final Comparator<Gambles> comparatorGamble = (o1, o2) -> Double.compare(o2.gambleAmount, o1.gambleAmount);

    // Threads.
    Thread amIAliveOrSomething;
    Thread resetLeaderboard;

    public Server() {
        if (instance != null) {
            throw new IllegalStateException("Server already initialized");
        }

        instance = this;

        try {
            Files.createDirectory(Path.of("data"));
        } catch (IOException e) {
            log.warn("Could not create directory data!");
        }

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
                    log.info("Resetting leaderboard...");
                    lastToken = null;
                    double highestBeat = leaderboardOfToday.get(0).beat;
                    double prizePool = gamblesOfToday.stream().mapToDouble(Gambles::getGambleAmount).sum();
                    List<Gambles> winners = gamblesOfToday.stream().filter(gamble -> gamble.heartBeat == highestBeat).toList();
                    if (!winners.isEmpty()) {
                        int prize = (int) (prizePool / winners.size());
                        for (Gambles winner : winners) {
                            Gambler gambler = getGambler(winner.user.id.toString());
                            gambler.money += prize;
                            saveGambler(gambler);
                        }
                    }
                    leaderboardOfToday.clear();
                    gamblesOfToday.clear();
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
                addToBeatsOfToday(HeartBeat.fromJson(jsonObject));
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
                leaderboardOfToday.sort(comparator);
                leaderboardOfToday.remove(leaderboardOfToday.size()-1);
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
                leaderboardOfAllTime.sort(comparator);
                leaderboardOfAllTime.remove(leaderboardOfAllTime.size()-1);
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
                    leaderboardOfAllTime.add(HeartBeat.fromJson(jsonObject));
                }

                leaderboardOfAllTime.sort(comparator);
                if (leaderboardOfAllTime.size() > leaderboardSize) {
                    leaderboardOfAllTime.subList(leaderboardSize, leaderboardOfAllTime.size()).clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAllTimeLeaderboard() {
        JsonArray jsonArray = new JsonArray();
        leaderboardOfAllTime.forEach(heartBeat -> jsonArray.add(heartBeat.toJson()));
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

    public Gambler getGambler(String id) {
        return getGambler(id, false);
    }

    public Gambler createGambler() {
        Gambler gambler = new Gambler(UUID.randomUUID(), generateToken(), generateName(), defaultMoney, 0);

        saveGambler(gambler);

        return gambler;
    }

    public Gambler getGambler(String id, boolean create) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
            final Path pathToGambler = Path.of("data", "gambler", uuid.toString() + ".json");
            if (Files.exists(pathToGambler)) {
                String jsonString = Files.readString(pathToGambler);
                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
                return Gambler.fromJson(jsonObject);
            }
        } catch (Exception e) {
            log.error("Could not load gambler with id {}", id);
        }

        if (!create) return null;

        return createGambler();
    }

    public GamblerSafe getGamblerSafe(String id) {
        return GamblerSafe.fromGambler(getGambler(id));
    }

    public boolean existGambler(String id) {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(id);
            final Path pathToGambler = Path.of("data", "gambler", uuid.toString() + ".json");
            return Files.exists(pathToGambler);
        } catch (Exception e) {
            log.error("Could not load gambler with id {}", id);
        }

        return false;
    }

    public void saveGambler(Gambler gambler) {
        try {
            Files.writeString(Path.of("data", "gambler", gambler.id.toString() + ".json"), gson.toJson(gambler.toJson()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Could not save gambler with id {}", gambler.id, e);
        }
    }

    public List<Gambles> getGamblers() {
        return gamblesOfToday.stream().sorted(comparatorGamble).limit(5).toList();
    }

    public boolean gambleMoney(String userId, int beat, int amount, String token) {
        Gambler gambler = getGambler(userId);
        if (!gambler.authToken.equals(token)) return false;
        if (gambler.money < amount) return false;
        if (amount < minimumBet) return false;

        gambler.money -= amount;
        saveGambler(gambler);

        Gambles gamble = new Gambles(gambler, UUID.randomUUID(), amount, beat, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        gamblesOfToday.add(gamble);

        return true;
    }

    public String generateName() {
        return "Gambler#" + new SecureRandom().nextInt(99999);
    }

    public String generateToken() {
        final String characters = "0123456789abcdefghijklmnopqrstuvwxyz-_ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        final SecureRandom secureRandom;
        try {
            secureRandom = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        // 9 is the length of the string you want

        return secureRandom
                .ints(9, 0, characters.length()) // 9 is the length of the string you want
                .mapToObj(characters::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String createHash(String value) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] encodedhash = digest.digest(
                value.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
    }
}

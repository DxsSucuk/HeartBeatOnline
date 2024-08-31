package de.presti.heartmybeatonline;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.presti.heartmybeatonline.util.LastBeat;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class Server {

    private static Server instance;
    private String authToken;
    private String lastToken;

    private final List<LastBeat> beatsOfToday = new java.util.ArrayList<>();
    private final List<LastBeat> leaderboardOfToday = new java.util.ArrayList<>();
    private final List<LastBeat> leaderboardOfAllTime = new java.util.ArrayList<>();

    // Threads.
    Thread amIAliveOrSomething;
    Thread resetLeaderboard;

    public Server() {
        if (instance != null) {
            throw new IllegalStateException("Server already initialized");
        }

        instance = this;

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

                try {
                    Thread.sleep(Duration.ofMinutes(10).toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        amIAliveOrSomething.start();
    }

    public void loadAllBeatsOfToday(String token) {
        lastToken = token;
        JsonObject apiObject = requestBeats(token, ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0), ZonedDateTime.now()).getAsJsonObject();
        if (apiObject.isJsonNull()) return;
        if (!apiObject.has("data")) return;

        JsonElement dataArray = apiObject.get("data");

        if (dataArray.isJsonArray() && !dataArray.getAsJsonArray().isEmpty()) {
            System.out.println("Found " + dataArray.getAsJsonArray().size() + " entries.");
            for (JsonElement jsonElement : dataArray.getAsJsonArray()) {
                if (!jsonElement.isJsonObject()) continue;
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                LastBeat beat = new LastBeat();
                beat.beat = jsonObject.get("bpm").getAsDouble();
                beat.timestamp = jsonObject.get("timestamp").getAsString();
                addToList(beat);
            }
        }

        if (apiObject.has("next_token") && !apiObject.get("next_token").isJsonNull()) {
            loadAllBeatsOfToday(apiObject.get("next_token").getAsString());
        }
    }

    public void addToList(LastBeat beat) {
        if (beatsOfToday.stream().anyMatch(lastBeat -> lastBeat.timestamp.equals(beat.timestamp))) return;

        if (leaderboardOfToday.isEmpty() || leaderboardOfToday.size() < 3) {
            addToLeaderboard(beat);
        } else {
            boolean hasHigherValue = false;
            for (LastBeat beatEntry : leaderboardOfToday) {
                if (beatEntry.beat < beat.beat) {
                    hasHigherValue = true;
                    break;
                }
            }

            if (hasHigherValue) {
                leaderboardOfToday.remove(leaderboardOfToday.size() - 1);
                addToLeaderboard(beat);
                leaderboardOfToday.sort((o1, o2) -> Double.compare(o2.beat, o1.beat));
            }
        }

        beatsOfToday.add(beat);
    }

    public void addToLeaderboard(LastBeat lastBeat) {
        leaderboardOfToday.add(lastBeat);
        if (leaderboardOfAllTime.isEmpty() || leaderboardOfAllTime.size() < 3) {
            leaderboardOfAllTime.add(lastBeat);
        } else {
            boolean hasHigherValue = false;
            for (LastBeat beatEntry : leaderboardOfAllTime) {
                if (beatEntry.beat < lastBeat.beat) {
                    hasHigherValue = true;
                    break;
                }
            }

            if (hasHigherValue) {
                leaderboardOfAllTime.remove(leaderboardOfAllTime.size() - 1);
                leaderboardOfAllTime.add(lastBeat);
                leaderboardOfAllTime.sort((o1, o2) -> Double.compare(o2.beat, o1.beat));
            }
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

    public static Server getInstance() {
        return instance;
    }

    public LastBeat getLastBeat() {
        if (beatsOfToday.isEmpty()) return null;
        return beatsOfToday.get(beatsOfToday.size() - 1);
    }

    public List<LastBeat> getLeaderboard() {
        return leaderboardOfToday;
    }

    public List<LastBeat> getLeaderboardAllTime() {
        return leaderboardOfAllTime;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }
}

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
import java.util.List;

public class Server {

    private static Server instance;
    private LastBeat lastBeat;
    private String token;

    private List<LastBeat> leaderboard;

    // Threads.
    Thread amIAliveOrSomething;

    public Server() {
        if (instance != null) {
            throw new IllegalStateException("Server already initialized");
        }

        instance = this;
        lastBeat = new LastBeat();
        lastBeat.beat = 90;
        lastBeat.timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        setLastBeat(lastBeat);

        amIAliveOrSomething = new Thread(() -> {
            while (amIAliveOrSomething != null && !amIAliveOrSomething.isInterrupted()) {
                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime last = now.minusMinutes(10);

                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                Request request = new Request.Builder()
                        .url("https://api.ouraring.com/v2/usercollection/heartrate?start_datetime=" + last.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+", "%2B") +
                                "&end_datetime=" + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).replace("+", "%2B"))
                        .method("GET", null)
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    JsonElement apiObject = JsonParser.parseString(response.body().string());
                    if (apiObject.isJsonObject() && apiObject.getAsJsonObject().has("data")) {
                        JsonElement dataArray = apiObject.getAsJsonObject().get("data");

                        if (dataArray.isJsonArray() && !dataArray.getAsJsonArray().isEmpty()) {
                            JsonObject lastBeat = dataArray.getAsJsonArray().get(dataArray.getAsJsonArray().size() - 1).getAsJsonObject();
                            LastBeat beat = new LastBeat();
                            beat.beat = lastBeat.get("bpm").getAsDouble();
                            beat.timestamp = lastBeat.get("timestamp").getAsString();
                            System.out.println("Last beat: " + beat.beat + " at " + beat.timestamp);
                            setLastBeat(beat);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(Duration.ofMinutes(5).toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        amIAliveOrSomething.start();
    }

    public static Server getInstance() {
        return instance;
    }

    public LastBeat getLastBeat() {
        return lastBeat;
    }

    public void setLastBeat(LastBeat lastBeat) {
        if (leaderboard.isEmpty() || leaderboard.size() < 3) {
            leaderboard.add(lastBeat);
        } else {
            for (LastBeat beat : leaderboard) {
                if (beat.beat < lastBeat.beat) {
                    leaderboard.remove(beat);
                    leaderboard.add(lastBeat);
                    break;
                }
            }
            leaderboard.sort((o1, o2) -> Double.compare(o2.beat, o1.beat));
        }

        this.lastBeat = lastBeat;
    }

    public List<LastBeat> getLeaderboard() {
        return leaderboard;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

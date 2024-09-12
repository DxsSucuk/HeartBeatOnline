package de.presti.heartmybeatonline.dto;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class HeartBeat {
    public double beat;
    public String timestamp;
    public String source;

    public static HeartBeat fromJson(JsonObject json) {
        HeartBeat beat = new HeartBeat();
        beat.beat = json.has("beat") ? json.get("beat").getAsDouble() : json.get("bpm").getAsDouble();
        beat.timestamp = json.get("timestamp").getAsString();
        beat.source = json.get("source").getAsString();
        return beat;
    }

    public static JsonObject toJson(HeartBeat beat) {
        JsonObject json = new JsonObject();
        json.addProperty("beat", beat.beat);
        json.addProperty("timestamp", beat.timestamp);
        json.addProperty("source", beat.source);
        return json;
    }

    public JsonObject toJson() {
        return toJson(this);
    }
}

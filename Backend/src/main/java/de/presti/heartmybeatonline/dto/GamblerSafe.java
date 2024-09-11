package de.presti.heartmybeatonline.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class GamblerSafe {
    public UUID id;
    public String name;
    public int money;
    public int wins;

    public static GamblerSafe fromJson(JsonObject json) {
        GamblerSafe gambler = new GamblerSafe();
        gambler.id = UUID.fromString(json.get("id").getAsString());
        gambler.name = json.get("name").getAsString();
        gambler.money = json.get("money").getAsInt();
        gambler.wins = json.get("wins").getAsInt();
        return gambler;
    }

    public static JsonObject toJson(GamblerSafe gambler) {
        JsonObject json = new JsonObject();
        json.addProperty("id", gambler.id.toString());
        json.addProperty("name", gambler.name);
        json.addProperty("money", gambler.money);
        json.addProperty("wins", gambler.wins);
        return json;
    }

    public JsonObject toJson() {
        return toJson(this);
    }

    public static GamblerSafe fromGambler(Gambler gambler) {
        if (gambler == null) return null;
        GamblerSafe gamblerSafe = new GamblerSafe();
        gamblerSafe.id = gambler.id;
        gamblerSafe.name = gambler.name;
        gamblerSafe.money = gambler.money;
        gamblerSafe.wins = gambler.wins;
        return gamblerSafe;
    }

    public static Gambler toGambler(GamblerSafe gamblerSafe) {
        Gambler gambler = new Gambler();
        gambler.id = gamblerSafe.id;
        gambler.name = gamblerSafe.name;
        gambler.money = gamblerSafe.money;
        gambler.wins = gamblerSafe.wins;
        return gambler;
    }

    public Gambler toGambler() {
        return toGambler(this);
    }
}

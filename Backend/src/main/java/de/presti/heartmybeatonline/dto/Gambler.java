package de.presti.heartmybeatonline.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class Gambler {
    public UUID id;
    public String authToken;
    public String name;
    public int money;
    public int wins;

    public static Gambler fromJson(JsonObject json) {
        Gambler gambler = new Gambler();
        gambler.id = UUID.fromString(json.get("id").getAsString());
        gambler.authToken = json.get("authToken").getAsString();
        gambler.name = json.get("name").getAsString();
        gambler.money = json.get("money").getAsInt();
        gambler.wins = json.get("wins").getAsInt();
        return gambler;
    }

    public static JsonObject toJson(Gambler gambler) {
        JsonObject json = new JsonObject();
        json.addProperty("id", gambler.id.toString());
        json.addProperty("authToken", gambler.authToken);
        json.addProperty("name", gambler.name);
        json.addProperty("money", gambler.money);
        json.addProperty("wins", gambler.wins);
        return json;
    }

    public JsonObject toJson() {
        return toJson(this);
    }
}

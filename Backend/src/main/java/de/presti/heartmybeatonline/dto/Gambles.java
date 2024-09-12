package de.presti.heartmybeatonline.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class Gambles {
    public Gambler user;
    public UUID gambleID;
    @Getter
    public int gambleAmount;
    public int heartBeat;
    public String timestamp;
}

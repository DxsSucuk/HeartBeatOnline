package de.presti.heartmybeatonline.dto;

import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class Gambler {
    public UUID id;
    public String name;
    public int money;
    public int wins;
}

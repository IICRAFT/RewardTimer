package com.willzcode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerKeep {
    public UUID uid;
    public boolean isKeep;
    public Map<String, Integer> keepMap = new HashMap<>();

    public PlayerKeep(UUID uid, boolean isKeep) {
        this.uid = uid;
        this.isKeep = isKeep;
    }
}

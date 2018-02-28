package com.willzcode;

import org.bukkit.inventory.ItemStack;

public class Reward {
    public String key;
    public ItemStack item;
    public int amount;
    public int interval;
    public int counting;
    public String message;
    public String permission;

    public Reward(String key, ItemStack item, int amount, int interval, int counting, String message, String permission) {
        this.key = key;
        this.item = item;
        this.amount = amount;
        this.interval = interval;
        this.counting = counting;
        this.message = message;
        this.permission = permission;
    }
}

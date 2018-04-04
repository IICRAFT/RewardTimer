package com.willzcode;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@SuppressWarnings("deprecation")
public class RewardTimer extends JavaPlugin implements Listener {
    private boolean isDefaultKeep;
    private String defaultMessage;
    private List<Reward> rewards = new ArrayList<>();
    private Map<UUID, PlayerKeep> playerKeep = new HashMap<>();

    @Override
    public void onEnable() {

        new BukkitRunnable(){
            @Override
            public void run(){
                for (Reward r : rewards) {
                    if (r.counting >= r.interval) {
                        r.counting = 0;
                        sendReward(r, r.message != null ? r.message : defaultMessage, r.permission);
                    }
                    r.counting++;
                }

            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sendKeep(event.getPlayer());
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            sendKeep(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0){
            sender.sendMessage("§3=========更好的在线奖励=========\n§2/rt reload 重载配置\n§2/rt claim 领取奖励");
        } else {
            switch (args[0]) {
                case "claim":
                    if (sender instanceof Player) {
                        sendKeep((Player) sender);
                    }

                    break;
                case "reload":
                    if (sender.hasPermission("rewardtimer.reload")) {
                        loadConfig();
                        sender.sendMessage("§2配置重载成功！");
                    }
                    break;
                default:
            }
        }
        return true;
    }

    @Override
    public void onLoad() {
        loadConfig();
    }

    private void loadConfig() {
        rewards.clear();
        playerKeep.clear();

        saveDefaultConfig();
        reloadConfig();
        FileConfiguration config = getConfig();

        isDefaultKeep = config.getBoolean("defaultKeep");
        defaultMessage = config.getString("defaultMessage");
        if(defaultMessage != null)
            defaultMessage = defaultMessage.replace("&", "§");

        Set<String> keys = config.getConfigurationSection("timerRewards").getKeys(false);
        for (String key : keys) {
            int interval = config.getInt("timerRewards." + key + ".interval");
            int id = config.getInt("timerRewards." + key + ".id");
            short damage = (short)config.getInt("timerRewards." + key + ".damage");
            int amount = config.getInt("timerRewards." + key + ".amount");
            String name = config.getString("timerRewards." + key + ".name");
            List<String> lores = config.getStringList("timerRewards." + key + ".lores");
            String message = config.getString("timerRewards." + key + ".message");
            String permission = config.getString("timerRewards." + key + ".permission");

            if(name != null)
                name = name.replace("&", "§");

            if(message != null)
                message = message.replace("&", "§");

            for (int i = 0; i < lores.size(); i++) {
                lores.set(i, lores.get(i).replace("&", "§"));
            }

            ItemStack item = new ItemStack(Material.getMaterial(id), amount, damage);
            ItemMeta im = item.getItemMeta();
            im.setDisplayName(name);
            im.setLore(lores);
            item.setItemMeta(im);

            Reward reward = new Reward(key, item, amount, interval, 0, message, permission);
            rewards.add(reward);

            getLogger().info("奖励已加载: "+key);
        }
    }

    private void sendReward(Reward reward, String message, String permission) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                if (isDefaultKeep) {
                    UUID uid = player.getUniqueId();
                    PlayerKeep keep = playerKeep.computeIfAbsent(uid, k -> new PlayerKeep(uid, true));

                    keep.keepMap.putIfAbsent(reward.key, 0);
                    int count = keep.keepMap.get(reward.key);
                    count += reward.amount;
                    keep.keepMap.replace(reward.key, count);
                    playerKeep.replace(uid, keep);
                } else {
                    player.getInventory().addItem(reward.item);
                    if(message != null)
                        player.sendMessage(message);
                }
            }
        }
    }

    private void sendKeep(Player player) {
        Inventory pi = player.getInventory();
        PlayerKeep keep = playerKeep.get(player.getUniqueId());
        int total = 0;
        boolean isFull = false;
        if(keep != null)
            for (Reward reward : rewards) {
                ItemStack item = reward.item;
                keep.keepMap.putIfAbsent(reward.key, 0);
                int count = keep.keepMap.get(reward.key);
                int stackSize = item.getMaxStackSize();
                for(int i = 0; i < pi.getSize() && count > 0; i++) {
                    int added;
                    ItemStack current = pi.getItem(i);
                    ItemStack temp = new ItemStack(item);
                    if (current == null) {
                        if (count > stackSize) {
                            added = stackSize;
                            count -= stackSize;
                        } else {
                            added = count;
                            count = 0;
                        }
                        total += added;
                        temp.setAmount(added);
                        pi.setItem(i, temp);
                        continue;
                    }
                    if (current.isSimilar(item) && current.getAmount() < stackSize) {
                        if (count > stackSize - current.getAmount()) {
                            added = stackSize - current.getAmount();
                            count -= stackSize - current.getAmount();
                        } else {
                            added = count;
                            count = 0;
                        }
                        total += added;
                        temp.setAmount(current.getAmount() + added);
                        pi.setItem(i, temp);
                    }

                }
                keep.keepMap.replace(reward.key, count);
                playerKeep.replace(player.getUniqueId(), keep);
                if (count > 0) {
                    player.sendMessage("§3你的背包满了，请留出足够的空间再来领取");
                    isFull = true;
                    break;
                }
            }

        if (!isFull)
            if (total > 0) {
                player.sendMessage("§2奖励已发送到你的背包");
            } else {
                player.sendMessage("§4暂时没有奖励，再等一会试试");
            }
    }
}

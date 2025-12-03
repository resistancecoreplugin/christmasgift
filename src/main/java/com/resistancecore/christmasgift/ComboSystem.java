package com.resistancecore.christmasgift;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class ComboSystem {
    private final ChristmasGift plugin;
    private final Map<UUID, ComboData> playerCombos = new HashMap<>();
    
    private static final long COMBO_TIMEOUT = 10000;
    private static final int COMBO_TIER_1 = 3;
    private static final int COMBO_TIER_2 = 5;
    private static final int COMBO_TIER_3 = 10;
    
    public ComboSystem(ChristmasGift plugin) {
        this.plugin = plugin;
        startComboChecker();
    }
    
    public void addCombo(Player player) {
        UUID playerId = player.getUniqueId();
        ComboData data = playerCombos.getOrDefault(playerId, new ComboData());
        
        data.combo++;
        data.lastOpenTime = System.currentTimeMillis();
        playerCombos.put(playerId, data);
        
        displayCombo(player, data.combo);
        playComboEffects(player, data.combo);
    }
    
    public double getMultiplier(Player player) {
        ComboData data = playerCombos.get(player.getUniqueId());
        if (data == null) return 1.0;
        
        int combo = data.combo;
        if (combo >= COMBO_TIER_3) return 3.0;
        if (combo >= COMBO_TIER_2) return 2.0;
        if (combo >= COMBO_TIER_1) return 1.5;
        return 1.0;
    }
    
    public int getComboCount(Player player) {
        ComboData data = playerCombos.get(player.getUniqueId());
        return data != null ? data.combo : 0;
    }
    
    private void displayCombo(Player player, int combo) {
        String message;
        String colorCode;
        
        if (combo >= COMBO_TIER_3) {
            colorCode = ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD.toString();
            message = colorCode + "@ " + combo + "x COMBO! @ [3.0x MULTIPLIER]";
        } else if (combo >= COMBO_TIER_2) {
            colorCode = ChatColor.GOLD.toString() + ChatColor.BOLD.toString();
            message = colorCode + "* " + combo + "x COMBO! * [2.0x MULTIPLIER]";
        } else if (combo >= COMBO_TIER_1) {
            colorCode = ChatColor.AQUA.toString() + ChatColor.BOLD.toString();
            message = colorCode + "# " + combo + "x COMBO! # [1.5x MULTIPLIER]";
        } else {
            colorCode = ChatColor.YELLOW.toString();
            message = colorCode + combo + "x Combo";
        }
        
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        
        if (combo >= COMBO_TIER_1) {
            player.sendMessage(message);
        }
    }
    
    private void playComboEffects(Player player, int combo) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        if (combo >= COMBO_TIER_3) {
            player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);
            player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.5f);
            spawnComboParticles(player, Particle.FLAME, 20);
            spawnComboParticles(player, Particle.CRIT, 15);
            spawnComboParticles(player, Particle.ENCHANT, 10);
            
        } else if (combo >= COMBO_TIER_2) {
            player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.playSound(loc, Sound.BLOCK_BELL_USE, 1.0f, 2.0f);
            spawnComboParticles(player, Particle.FLAME, 15);
            spawnComboParticles(player, Particle.CRIT, 10);
            
        } else if (combo >= COMBO_TIER_1) {
            player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            spawnComboParticles(player, Particle.CRIT, 10);
            spawnComboParticles(player, Particle.ENCHANT, 8);
            
        } else {
            player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f + (combo * 0.1f));
        }
    }
    
    private void spawnComboParticles(Player player, Particle particle, int count) {
        Location loc = player.getLocation().add(0, 1, 0);
        
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 10 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                double angle = ticks * Math.PI / 4;
                double radius = 1.0;
                
                for (int i = 0; i < 3; i++) {
                    double offsetAngle = angle + (i * Math.PI * 2 / 3);
                    double x = Math.cos(offsetAngle) * radius;
                    double z = Math.sin(offsetAngle) * radius;
                    
                    Location particleLoc = loc.clone().add(x, ticks * 0.1, z);
                    player.getWorld().spawnParticle(particle, particleLoc, 1);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void breakCombo(Player player, int comboCount) {
        if (comboCount >= COMBO_TIER_1) {
            String breakerMessage = ChatColor.RED.toString() + ChatColor.BOLD.toString() + "X COMBO BREAKER! X";
            String infoMessage = ChatColor.GRAY + "Your " + comboCount + "x combo has ended!";
            
            player.sendMessage(breakerMessage);
            player.sendMessage(infoMessage);
            
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.5, 0.5, 0.5);
            player.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 0.3, 0.3, 0.3);
        }
    }
    
    private void startComboChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                List<UUID> toRemove = new ArrayList<>();
                
                for (Map.Entry<UUID, ComboData> entry : playerCombos.entrySet()) {
                    UUID playerId = entry.getKey();
                    ComboData data = entry.getValue();
                    
                    if (currentTime - data.lastOpenTime > COMBO_TIMEOUT) {
                        toRemove.add(playerId);
                    }
                }
                
                for (UUID playerId : toRemove) {
                    ComboData data = playerCombos.get(playerId);
                    if (data != null) {
                        Player player = plugin.getServer().getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            breakCombo(player, data.combo);
                        }
                        playerCombos.remove(playerId);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    public void clearCombo(Player player) {
        playerCombos.remove(player.getUniqueId());
    }
    
    private static class ComboData {
        int combo = 0;
        long lastOpenTime = 0;
    }
}

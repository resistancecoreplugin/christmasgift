package com.resistancecore.christmasgift;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import java.util.*;

public class RouletteSystem {
    private final ChristmasGift plugin;
    private final Map<String, TierInfo> tierInfoMap = new LinkedHashMap<>();
    
    public RouletteSystem(ChristmasGift plugin) {
        this.plugin = plugin;
        initializeTierInfo();
    }
    
    // Utility method to clear action bar
    public void clearActionBar(Player player) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                new TextComponent(""));
        }
    }
    
    private void initializeTierInfo() {
        // Define tiers in order from lowest to highest for roulette display
        // Using safe ASCII symbols that render properly in Minecraft
        tierInfoMap.put("common", new TierInfo("COMMON", "&f&l", "+", ChatColor.WHITE));
        tierInfoMap.put("rare", new TierInfo("RARE", "&b&l", "*", ChatColor.AQUA));
        tierInfoMap.put("epic", new TierInfo("EPIC", "&5&l", "#", ChatColor.DARK_PURPLE));
        tierInfoMap.put("legendary", new TierInfo("LEGENDARY", "&6&l", "@", ChatColor.GOLD));
        tierInfoMap.put("mythic", new TierInfo("MYTHIC", "&d&l", "&", ChatColor.LIGHT_PURPLE));
    }
    
    public void startRouletteAnimation(Player player, String finalTier) {
        new BukkitRunnable() {
            private int tickCount = 0;
            private final int TOTAL_TICKS = 80; // Total animation duration
            private final int FAST_PHASE = 30; // Fast spinning phase
            private final int SLOW_PHASE = 70; // Start slowing down
            
            private int currentTierIndex = 0;
            private final List<String> tierOrder = new ArrayList<>(tierInfoMap.keySet());
            
            // Speed control
            private int ticksPerChange = 2; // Start fast
            private int ticksSinceLastChange = 0;
            
            @Override
            public void run() {
                if (tickCount >= TOTAL_TICKS || !player.isOnline()) {
                    // Final reveal
                    revealFinalReward(player, finalTier);
                    this.cancel();
                    return;
                }
                
                // Calculate current speed based on phase
                if (tickCount < FAST_PHASE) {
                    // Fast spinning
                    ticksPerChange = 2;
                } else if (tickCount < SLOW_PHASE) {
                    // Medium speed
                    ticksPerChange = 4;
                } else {
                    // Slow down progressively
                    int remaining = TOTAL_TICKS - tickCount;
                    ticksPerChange = 6 + (TOTAL_TICKS - tickCount) / 2;
                }
                
                ticksSinceLastChange++;
                
                // Change displayed tier when enough ticks have passed
                if (ticksSinceLastChange >= ticksPerChange) {
                    ticksSinceLastChange = 0;
                    
                    // Cycle through tiers
                    currentTierIndex = (currentTierIndex + 1) % tierOrder.size();
                    
                    String currentTier = tierOrder.get(currentTierIndex);
                    displayTierInActionBar(player, currentTier, tickCount);
                    
                    // Play sound with increasing pitch
                    float pitch = 0.5f + (tickCount / (float) TOTAL_TICKS) * 1.5f;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
                }
                
                tickCount++;
            }
            
            private void displayTierInActionBar(Player player, String tierKey, int tick) {
                TierInfo tier = tierInfoMap.get(tierKey);
                if (tier == null) return;
                
                // Create spinning effect with surrounding tiers
                StringBuilder display = new StringBuilder();
                
                // Add previous tier (dimmed)
                int prevIndex = (currentTierIndex - 1 + tierOrder.size()) % tierOrder.size();
                TierInfo prevTier = tierInfoMap.get(tierOrder.get(prevIndex));
                display.append(ChatColor.DARK_GRAY).append(prevTier.symbol).append(" ");
                
                // Add current tier (highlighted)
                String color = ChatColor.translateAlternateColorCodes('&', tier.colorCode);
                display.append(color);
                
                // Blinking effect for current tier - using simple brackets
                if (tick % 4 < 2) {
                    display.append("[[ ").append(tier.symbol).append(" ").append(tier.displayName).append(" ").append(tier.symbol).append(" ]]");
                } else {
                    display.append(">> ").append(tier.symbol).append(" ").append(tier.displayName).append(" ").append(tier.symbol).append(" <<");
                }
                
                // Add next tier (dimmed)
                int nextIndex = (currentTierIndex + 1) % tierOrder.size();
                TierInfo nextTier = tierInfoMap.get(tierOrder.get(nextIndex));
                display.append(" ").append(ChatColor.DARK_GRAY).append(nextTier.symbol);
                
                // Send to action bar
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new TextComponent(display.toString()));
            }
            
            private void revealFinalReward(Player player, String finalTier) {
                TierInfo tier = tierInfoMap.get(finalTier);
                if (tier == null) return;
                
                // Play final reveal sound
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);
                
                // Display final result in action bar with special effect
                String color = ChatColor.translateAlternateColorCodes('&', tier.colorCode);
                
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new TextComponent(color + "[[ " + tier.displayName + " REWARD! ]]"));
                
                // Flash effect in action bar
                new BukkitRunnable() {
                    private int flashCount = 0;
                    
                    @Override
                    public void run() {
                        if (flashCount >= 6 || !player.isOnline()) {
                            // IMPORTANT: Clear action bar after animation
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                                new TextComponent(""));
                            this.cancel();
                            return;
                        }
                        
                        if (flashCount % 2 == 0) {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                                new TextComponent(color + "*** " + tier.displayName + " REWARD! ***"));
                        } else {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                                new TextComponent(color + ">>> " + tier.displayName + " REWARD! <<<"));
                        }
                        
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                        flashCount++;
                    }
                }.runTaskTimer(plugin, 0L, 3L);
                
                // Give reward after flash effect completes
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getRewardSystem().giveReward(player, finalTier);
                }, 20L);
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for smooth animation
    }
    
    private static class TierInfo {
        final String displayName;
        final String colorCode;
        final String symbol;
        final ChatColor chatColor;
        
        TierInfo(String displayName, String colorCode, String symbol, ChatColor chatColor) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.symbol = symbol;
            this.chatColor = chatColor;
        }
    }
}
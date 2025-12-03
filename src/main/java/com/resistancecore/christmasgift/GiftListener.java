package com.resistancecore.christmasgift;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class GiftListener implements Listener {
    private final ChristmasGift plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutes

    public GiftListener(ChristmasGift plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.CHEST || !item.hasItemMeta() ||
            !item.getItemMeta().hasDisplayName() ||
            !item.getItemMeta().getDisplayName().contains("Christmas Gift")) {
            return;
        }

        // Prevent block placement
        event.setCancelled(true);

        // Check cooldown - READ FROM CONFIG
        String playerName = player.getName();
        long cooldownTime = plugin.getConfig().getLong("settings.gift-cooldown", 1000);
        
        if (cooldowns.containsKey(playerName)) {
            long timeElapsed = System.currentTimeMillis() - cooldowns.get(playerName);
            if (timeElapsed < cooldownTime) {
                return;
            }
        }

        cooldowns.put(playerName, System.currentTimeMillis());

        // Add combo
        plugin.getComboSystem().addCombo(player);

        // Clear action bar first to prevent conflicts
        plugin.getRouletteSystem().clearActionBar(player);

        // Remove one gift from hand
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Calculate tier BEFORE starting animation
        String selectedTier = plugin.getRewardSystem().calculateTier();
        
        // Start gift opening animation first
        plugin.getGiftEffects().playOpeningAnimation(player);
        
        // Start roulette animation after a brief delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getRouletteSystem().startRouletteAnimation(player, selectedTier);
            }
        }, 10L); // Start roulette after 0.5 seconds
        
        // Note: Reward is given by RouletteSystem after animation completes
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        
        // Prevent placing Christmas Gift chests
        if (item != null && item.getType() == Material.CHEST && 
            item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
            item.getItemMeta().getDisplayName().contains("Christmas Gift")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player data when they quit
        cooldowns.remove(event.getPlayer().getName());
        
        // Clear combo
        plugin.getComboSystem().clearCombo(event.getPlayer());
        
        // Clear action bar to prevent leftover messages
        plugin.getRouletteSystem().clearActionBar(event.getPlayer());
    }

    private void startCleanupTask() {
        // Periodic cleanup of old cooldown entries
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, Long>> iterator = cooldowns.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<String, Long> entry = iterator.next();
                if (currentTime - entry.getValue() > CLEANUP_INTERVAL) {
                    iterator.remove();
                }
            }
        }, 6000L, 6000L); // Run every 5 minutes
    }
}

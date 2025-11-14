package com.resistancecore.christmasgift;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class GuiHandler implements Listener {
    private final ChristmasGift plugin;
    private final Map<UUID, GuiState> playerStates = new HashMap<>();
    private final Map<UUID, String> editingTier = new HashMap<>();

    public GuiHandler(ChristmasGift plugin) {
        this.plugin = plugin;
    }

    private enum GuiState {
        EDITING_CHANCE,
        EDITING_PREFIX,
        ADDING_NEW_TIER,
        EDITING_COMMAND,
        NONE
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Only protect our plugin's GUIs
        if (!title.equals("Christmas Gift Admin") && !title.contains("Rewards Editor: ")) {
            return;
        }

        event.setCancelled(true);
        
        // Prevent interaction with bottom inventory
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            return;
        }
        
        // Prevent clicks outside inventory
        if (event.getClickedInventory() == null) {
            return;
        }

        // Only process clicks in our GUI
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        // Handle GUI clicks based on inventory title
        if (title.equals("Christmas Gift Admin")) {
            handleMainMenuClick(event);
        } else if (title.contains("Rewards Editor: ")) {
            handleRewardsEditorClick(event);
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || !clicked.hasItemMeta()) return;
        String itemName = clicked.getItemMeta().getDisplayName();

        if (itemName.contains("Tier: ")) {
            String tierName = ChatColor.stripColor(itemName.substring(itemName.indexOf("Tier: ") + 6));
            editingTier.put(player.getUniqueId(), tierName);

            if (event.getClick() == ClickType.LEFT) {
                player.closeInventory();
                playerStates.put(player.getUniqueId(), GuiState.EDITING_CHANCE);
                player.sendMessage("§e§lEnter new chance for tier " + tierName);
                player.sendMessage("§7Format: <number> (example: 25.5)");
            } else if (event.getClick() == ClickType.RIGHT) {
                openRewardsEditor(player, tierName);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                player.closeInventory();
                playerStates.put(player.getUniqueId(), GuiState.EDITING_PREFIX);
                player.sendMessage("§e§lEnter new prefix for tier " + tierName);
                player.sendMessage("§7Format: &wPrefix (example: &6[Legendary])");
            }
        } else if (itemName.equals("§a§lAdd New Tier")) {
            player.closeInventory();
            playerStates.put(player.getUniqueId(), GuiState.ADDING_NEW_TIER);
            player.sendMessage("§e§lEnter new tier name");
            player.sendMessage("§7Format: tier_name (example: mythic)");
        }
    }

    private void openRewardsEditor(Player player, String tierName) {
        Inventory gui = Bukkit.createInventory(null, 54, "Rewards Editor: " + tierName);
        ConfigurationSection tierSection = plugin.getRewardsConfig().getConfigurationSection("tiers." + tierName);
        
        if (tierSection != null) {
            List<String> rewards = tierSection.getStringList("rewards");
            
            // Fill border with glass panes
            ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta borderMeta = border.getItemMeta();
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
            
            for (int i = 0; i < 54; i++) {
                gui.setItem(i, border);
            }
            
            // Clear middle area for rewards (keep border)
            for (int row = 1; row < 5; row++) {
                for (int col = 1; col < 8; col++) {
                    gui.setItem(row * 9 + col, null);
                }
            }
            
            // Add rewards in the middle area
            int slot = 10; // Start after border
            int rewardIndex = 0;
            
            for (String reward : rewards) {
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§6Reward #" + (rewardIndex + 1));
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Command: " + reward);
                lore.add("");
                lore.add("§eLeft click to edit");
                lore.add("§cRight click to delete");
                
                meta.setLore(lore);
                item.setItemMeta(meta);
                gui.setItem(slot, item);
                
                rewardIndex++;
                slot++;
                
                // Move to next row if reached border
                if (slot % 9 == 8) {
                    slot += 2;
                }
            }

            // Add new reward button
            ItemStack addButton = new ItemStack(Material.EMERALD);
            ItemMeta addMeta = addButton.getItemMeta();
            addMeta.setDisplayName("§a§lAdd New Reward");
            List<String> addLore = new ArrayList<>();
            addLore.add("§7Click to add a new reward");
            addMeta.setLore(addLore);
            addButton.setItemMeta(addMeta);
            gui.setItem(49, addButton);

            // Add back button
            ItemStack backButton = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = backButton.getItemMeta();
            backMeta.setDisplayName("§c§lBack");
            List<String> backLore = new ArrayList<>();
            backLore.add("§7Click to return to main menu");
            backMeta.setLore(backLore);
            backButton.setItemMeta(backMeta);
            gui.setItem(45, backButton);
        }

        player.openInventory(gui);
    }

    private void handleRewardsEditorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        
        // Ensure click happened in top inventory (GUI)
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        
        String tierName = event.getView().getTitle().substring(event.getView().getTitle().indexOf(": ") + 2);
        ItemStack clicked = event.getCurrentItem();
        
        // Validate clicked item
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clicked.getItemMeta().getDisplayName();
        
        if (itemName.equals("§c§lBack")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getCommand("christmasgift").getExecutor().onCommand(player, null, "", new String[0]);
            });
            return;
        }
        
        if (itemName.equals("§a§lAdd New Reward")) {
            player.closeInventory();
            editingTier.put(player.getUniqueId(), tierName);
            playerStates.put(player.getUniqueId(), GuiState.EDITING_COMMAND);
            player.sendMessage("§e§lEnter new reward command");
            player.sendMessage("§7Format: command (example: give %player% diamond 5)");
            return;
        }
        
        if (itemName.contains("Reward #")) {
            try {
                int rewardIndex = Integer.parseInt(itemName.substring(itemName.indexOf("#") + 1)) - 1;
                
                if (event.getClick() == ClickType.LEFT) {
                    player.closeInventory();
                    editingTier.put(player.getUniqueId(), tierName + ":" + rewardIndex);
                    playerStates.put(player.getUniqueId(), GuiState.EDITING_COMMAND);
                    player.sendMessage("§e§lEnter new command for this reward");
                    player.sendMessage("§7Format: command (example: give %player% diamond 5)");
                } else if (event.getClick() == ClickType.RIGHT) {
                    ConfigurationSection tierSection = plugin.getRewardsConfig().getConfigurationSection("tiers." + tierName);
                    if (tierSection != null) {
                        List<String> rewards = tierSection.getStringList("rewards");
                        if (rewardIndex >= 0 && rewardIndex < rewards.size()) {
                            rewards.remove(rewardIndex);
                            tierSection.set("rewards", rewards);
                            plugin.saveRewardsConfig();
                            plugin.getRewardSystem().reloadRewardTiers();
                            openRewardsEditor(player, tierName);
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
                // Invalid reward number format, ignore
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        GuiState state = playerStates.get(playerId);
        
        if (state == null || state == GuiState.NONE) return;
        
        event.setCancelled(true);
        String message = event.getMessage();
        String tierName = editingTier.get(playerId);

        switch (state) {
            case EDITING_CHANCE:
                try {
                    double chance = Double.parseDouble(message);
                    plugin.getRewardsConfig().set("tiers." + tierName + ".chance", chance);
                    plugin.saveRewardsConfig();
                    plugin.getRewardSystem().reloadRewardTiers();
                    player.sendMessage("§a§lChance successfully changed!");
                } catch (NumberFormatException e) {
                    player.sendMessage("§c§lInvalid number format!");
                }
                break;

            case EDITING_PREFIX:
                plugin.getRewardsConfig().set("tiers." + tierName + ".prefix", message);
                plugin.saveRewardsConfig();
                plugin.getRewardSystem().reloadRewardTiers();
                player.sendMessage("§a§lPrefix successfully changed to: " + ChatColor.translateAlternateColorCodes('&', message));
                break;

            case ADDING_NEW_TIER:
                ConfigurationSection tiersSection = plugin.getRewardsConfig().createSection("tiers." + message);
                tiersSection.set("chance", 10.0);
                tiersSection.set("color", "&f");
                tiersSection.set("prefix", "&f[" + message.substring(0, 1).toUpperCase() + message.substring(1) + "]");
                tiersSection.set("rewards", new ArrayList<String>());
                plugin.saveRewardsConfig();
                plugin.getRewardSystem().reloadRewardTiers();
                player.sendMessage("§a§lNew tier successfully added!");
                break;

            case EDITING_COMMAND:
                if (tierName.contains(":")) {
                    String[] parts = tierName.split(":");
                    String tier = parts[0];
                    int index = Integer.parseInt(parts[1]);
                    
                    List<String> rewards = plugin.getRewardsConfig().getStringList("tiers." + tier + ".rewards");
                    rewards.set(index, message);
                    plugin.getRewardsConfig().set("tiers." + tier + ".rewards", rewards);
                } else {
                    List<String> rewards = plugin.getRewardsConfig().getStringList("tiers." + tierName + ".rewards");
                    rewards.add(message);
                    plugin.getRewardsConfig().set("tiers." + tierName + ".rewards", rewards);
                }
                plugin.saveRewardsConfig();
                plugin.getRewardSystem().reloadRewardTiers();
                player.sendMessage("§a§lCommand successfully saved!");
                break;
        }

        playerStates.put(playerId, GuiState.NONE);
        editingTier.remove(playerId);

        // Reopen main menu after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getCommand("christmasgift").getExecutor().onCommand(player, null, "", new String[0]);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        
        // Only check for our plugin's GUIs
        if (title.equals("Christmas Gift Admin") || title.contains("Rewards Editor: ")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        GuiState state = playerStates.get(playerId);
        if (state == GuiState.NONE) {
            playerStates.remove(playerId);
            editingTier.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player data when they quit
        UUID playerId = event.getPlayer().getUniqueId();
        playerStates.remove(playerId);
        editingTier.remove(playerId);
    }
}
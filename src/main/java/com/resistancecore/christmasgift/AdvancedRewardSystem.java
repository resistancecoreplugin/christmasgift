package com.resistancecore.christmasgift;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import java.util.*;

public class AdvancedRewardSystem {
    private final ChristmasGift plugin;
    private final Random random = new Random();
    private final Map<String, RewardTier> rewardTiers = new HashMap<>();

    public AdvancedRewardSystem(ChristmasGift plugin) {
        this.plugin = plugin;
        loadRewardTiers();
    }

    private void loadRewardTiers() {
        ConfigurationSection tiersSection = plugin.getRewardsConfig().getConfigurationSection("tiers");
        if (tiersSection == null) return;

        for (String tierName : tiersSection.getKeys(false)) {
            ConfigurationSection tierSection = tiersSection.getConfigurationSection(tierName);
            if (tierSection == null) continue;

            RewardTier tier = new RewardTier(
                tierName,
                tierSection.getDouble("chance", 0.0),
                tierSection.getString("color", "&f"),
                tierSection.getString("prefix", ""),
                tierSection.getStringList("rewards")
            );
            rewardTiers.put(tierName, tier);
        }
    }
    
    public void reloadRewardTiers() {
        rewardTiers.clear();
        loadRewardTiers();
    }

    public String calculateTier() {
        RewardTier selectedTier = selectTier();
        return selectedTier != null ? selectedTier.getName() : "common";
    }

    public void giveReward(Player player, String tierName) {
        RewardTier selectedTier = rewardTiers.get(tierName);
        if (selectedTier == null) {
            selectedTier = rewardTiers.get("common");
        }
        if (selectedTier == null) return;

        // Get random reward from tier
        String reward = selectRewardFromTier(selectedTier);
        if (reward == null) return;

        // Get combo multiplier
        double multiplier = plugin.getComboSystem().getMultiplier(player);
        int comboCount = plugin.getComboSystem().getComboCount(player);
        
        // Apply multiplier to reward
        String multipliedReward = applyMultiplier(reward, multiplier);

        // Execute command silently
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String command = multipliedReward.replace("%player%", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        });

        // Prepare reward display message
        String rewardDisplay = formatRewardDisplay(multipliedReward);
        
        // Add combo info to display if combo active
        if (comboCount >= 3) {
            String comboText = ChatColor.YELLOW + " (x" + String.format("%.1f", multiplier) + " Combo!)";
            rewardDisplay += comboText;
        }
        
        // Send title to player
        sendRewardTitle(player, selectedTier, rewardDisplay);
        
        // Broadcast to all players
        broadcastReward(player.getName(), rewardDisplay, selectedTier);
    }

    // LEGACY METHOD: Keep for backward compatibility
    public void giveReward(Player player) {
        String tier = calculateTier();
        giveReward(player, tier);
    }
    
    private void sendRewardTitle(Player player, RewardTier tier, String rewardDisplay) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            tier.getColor() + "✦ " + tier.getName().toUpperCase() + " REWARD ✦");
        String subtitle = ChatColor.translateAlternateColorCodes('&', 
            "&fYou received &e" + rewardDisplay + "&f!");
        
        player.sendTitle(title, subtitle, 10, 70, 20);
    }
    
    private void broadcastReward(String playerName, String rewardDisplay, RewardTier tier) {
        String message = ChatColor.translateAlternateColorCodes('&', 
            tier.getColor() + "✦ " + tier.getName().toUpperCase() + " ✦ &f" + 
            playerName + " received &e" + rewardDisplay + "&f!");
        plugin.getServer().broadcastMessage(message);
    }
    
    private String formatRewardDisplay(String command) {
        StringBuilder message = new StringBuilder();
        String[] parts = command.split(" ");

        try {
            if (command.contains("give")) {
                // Parse the command properly
                int amount = 1;
                String itemName = "";
                
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("give") && i + 2 < parts.length) {
                        // Skip "give" and player name
                        String rawItem = parts[i + 2];
                        
                        // Remove minecraft: prefix if exists
                        if (rawItem.contains(":")) {
                            rawItem = rawItem.substring(rawItem.indexOf(":") + 1);
                        }
                        
                        // Remove enchantment data (everything in square brackets or curly braces)
                        if (rawItem.contains("[")) {
                            rawItem = rawItem.substring(0, rawItem.indexOf("["));
                        }
                        if (rawItem.contains("{")) {
                            rawItem = rawItem.substring(0, rawItem.indexOf("{"));
                        }
                        
                        itemName = formatItemName(rawItem);
                        
                        // Check if there's an amount after the item
                        if (i + 3 < parts.length) {
                            try {
                                String potentialAmount = parts[i + 3];
                                // Make sure it's not an enchantment data
                                if (!potentialAmount.startsWith("{") && !potentialAmount.startsWith("[")) {
                                    amount = Integer.parseInt(potentialAmount);
                                }
                            } catch (NumberFormatException e) {
                                // If amount is invalid, keep it as 1
                            }
                        }
                        break;
                    }
                }
                
                // Format the message
                if (amount > 1) {
                    message.append(amount).append("x ").append(itemName);
                } else {
                    message.append(itemName);
                }
                
            } else if (command.contains("xp add")) {
                String amount = parts[parts.length - 1];
                message.append(formatNumber(Integer.parseInt(amount))).append(" XP");
                
            } else if (command.contains("eco give")) {
                String amount = parts[parts.length - 1];
                message.append("$").append(formatNumber(Integer.parseInt(amount)));
                
            } else {
                message.append("Special Reward");
            }
        } catch (Exception e) {
            message.append("Special Reward");
        }

        return message.toString();
    }

    
    private String formatItemName(String rawName) {
        // Replace underscores and format special cases
        String name = rawName.toLowerCase()
            .replace("_", " ")
            .replace("netherite", "Netherite")
            .replace("diamond", "Diamond")
            .replace("emerald", "Emerald")
            .replace("golden apple", "Golden Apple")
            .replace("enchanted golden apple", "Enchanted Golden Apple");
            
        // Capitalize first letter of each word
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            if (word.length() > 0) {
                formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
        }
        return formatted.toString();
    }
    
    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.format("%,d", number);
    }
    
    private RewardTier selectTier() {
        double totalChance = rewardTiers.values().stream()
            .mapToDouble(RewardTier::getChance)
            .sum();

        double random = this.random.nextDouble() * totalChance;
        double cumulative = 0.0;

        for (RewardTier tier : rewardTiers.values()) {
            cumulative += tier.getChance();
            if (random <= cumulative) {
                return tier;
            }
        }

        return null;
    }

    private String selectRewardFromTier(RewardTier tier) {
        List<String> rewards = tier.getRewards();
        if (rewards.isEmpty()) return null;
        return rewards.get(random.nextInt(rewards.size()));
    }
    
    private String applyMultiplier(String command, double multiplier) {
        if (multiplier <= 1.0) return command;
        
        String[] parts = command.split(" ");
        StringBuilder result = new StringBuilder();
        
        try {
            if (command.contains("give")) {
                // Find and multiply item amount
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) result.append(" ");
                    
                    // Check if this is the amount parameter (after item name)
                    if (i >= 3 && i == 3) {
                        // This might be the amount or could be item name continuation
                        try {
                            // Try to parse as number
                            int amount = Integer.parseInt(parts[i]);
                            int multipliedAmount = (int) Math.ceil(amount * multiplier);
                            result.append(multipliedAmount);
                        } catch (NumberFormatException e) {
                            // Not a number, just append as is
                            result.append(parts[i]);
                        }
                    } else if (i == parts.length - 1 && i >= 3) {
                        // Last parameter might be amount
                        try {
                            int amount = Integer.parseInt(parts[i]);
                            int multipliedAmount = (int) Math.ceil(amount * multiplier);
                            result.append(multipliedAmount);
                        } catch (NumberFormatException e) {
                            result.append(parts[i]);
                        }
                    } else {
                        result.append(parts[i]);
                    }
                }
                
            } else if (command.contains("xp add") || command.contains("eco give")) {
                // Multiply last number (the amount)
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) result.append(" ");
                    
                    if (i == parts.length - 1) {
                        try {
                            int amount = Integer.parseInt(parts[i]);
                            int multipliedAmount = (int) Math.ceil(amount * multiplier);
                            result.append(multipliedAmount);
                        } catch (NumberFormatException e) {
                            result.append(parts[i]);
                        }
                    } else {
                        result.append(parts[i]);
                    }
                }
            } else {
                // Unknown command type, return as is
                return command;
            }
            
            return result.toString();
            
        } catch (Exception e) {
            // If any error, return original command
            return command;
        }
    }

    private static class RewardTier {
        private final String name;
        private final double chance;
        private final String color;
        private final String prefix;
        private final List<String> rewards;

        public RewardTier(String name, double chance, String color, String prefix, List<String> rewards) {
            this.name = name;
            this.chance = chance;
            this.color = color;
            this.prefix = prefix;
            this.rewards = rewards;
        }

        public String getName() {
            return name;
        }

        public double getChance() {
            return chance;
        }

        public String getColor() {
            return color;
        }

        public String getPrefix() {
            return prefix;
        }

        public List<String> getRewards() {
            return rewards;
        }

        @Override
        public String toString() {
            return "RewardTier{" +
                "name='" + name + '\'' +
                ", chance=" + chance +
                ", color='" + color + '\'' +
                ", prefix='" + prefix + '\'' +
                ", rewards=" + rewards +
                '}';
        }
    }
}
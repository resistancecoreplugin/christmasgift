package com.resistancecore.christmasgift;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;

public class GiftCommand implements CommandExecutor {
    private final ChristmasGift plugin;

    public GiftCommand(ChristmasGift plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("christmasgift.admin")) {
            sender.sendMessage("§c§lChristmasGift §7» §cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c§lChristmasGift §7» §cThis command can only be used by players!");
                sender.sendMessage("§e§lUsage: §7/" + label + " give <amount> <player>");
                sender.sendMessage("§e§lUsage: §7/" + label + " reload");
                return true;
            }
            openAdminGUI((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 2) {
                sender.sendMessage("§c§lChristmasGift §7» §cInvalid usage!");
                if (sender instanceof Player) {
                    sender.sendMessage("§e§lUsage: §7/" + label + " give <amount> [player]");
                } else {
                    sender.sendMessage("§e§lUsage: §7/" + label + " give <amount> <player>");
                }
                return true;
            }

            // Parse amount
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§c§lChristmasGift §7» §cAmount must be between 1-64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c§lChristmasGift §7» §cAmount must be a number!");
                return true;
            }

            // Determine target player
            Player targetPlayer = null;
            
            if (args.length >= 3) {
                // Target player specified
                targetPlayer = plugin.getServer().getPlayer(args[2]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    sender.sendMessage("§c§lChristmasGift §7» §cPlayer '" + args[2] + "' not found or offline!");
                    return true;
                }
            } else {
                // No target specified
                if (sender instanceof Player) {
                    // Sender is player, give to self
                    targetPlayer = (Player) sender;
                } else {
                    // Sender is console, require target player
                    sender.sendMessage("§c§lChristmasGift §7» §cYou must specify a player when using this command from console!");
                    sender.sendMessage("§e§lUsage: §7/" + label + " give <amount> <player>");
                    return true;
                }
            }

            // Give gift to target player
            giveGift(targetPlayer, amount);
            
            // Send confirmation messages
            if (sender instanceof Player && sender.equals(targetPlayer)) {
                sender.sendMessage("§a§lChristmasGift §7» §aYou have received §e" + amount + "x §aChristmas Gift!");
            } else {
                sender.sendMessage("§a§lChristmasGift §7» §aSuccessfully gave §e" + amount + "x §aChristmas Gift to §e" + targetPlayer.getName() + "§a!");
                targetPlayer.sendMessage("§a§lChristmasGift §7» §aYou have received §e" + amount + "x §aChristmas Gift§a!");
            }
            
            return true;
        }

        // Add reload command
        if (args[0].equalsIgnoreCase("reload")) {
            try {
                // Reload configs
                plugin.reloadConfig();
                plugin.reloadRewardsConfig();
                
                // Reload reward system
                plugin.getRewardSystem().reloadRewardTiers();
                
                sender.sendMessage("§a§lChristmasGift §7» §aConfiguration reloaded successfully!");
                return true;
            } catch (Exception e) {
                sender.sendMessage("§c§lChristmasGift §7» §cAn error occurred while reloading configuration!");
                plugin.getLogger().severe("Error reloading config: " + e.getMessage());
                return true;
            }
        }

        sender.sendMessage("§c§lChristmasGift §7» §cInvalid usage!");
        sender.sendMessage("§e§lUsage: §7/" + label + " give <amount> [player]");
        sender.sendMessage("§e§lUsage: §7/" + label + " reload");
        return true;
    }

    private void openAdminGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 54, "Christmas Gift Admin");

        // Add tier management items
        ConfigurationSection tiers = plugin.getRewardsConfig().getConfigurationSection("tiers");
        if (tiers != null) {
            int slot = 0;
            for (String tierName : tiers.getKeys(false)) {
                if (slot >= 53) break; // Prevent overflow
                
                ConfigurationSection tier = tiers.getConfigurationSection(tierName);
                if (tier == null) continue;

                ItemStack item = new ItemStack(Material.BOOK);
                ItemMeta meta = item.getItemMeta();
                String color = tier.getString("color", "&f");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                    color + "Tier: " + tierName));

                List<String> lore = new ArrayList<>();
                lore.add("§7Chance: " + tier.getDouble("chance") + "%");
                lore.add("§7Prefix: " + tier.getString("prefix"));
                lore.add("");
                lore.add("§eLeft click to edit chance");
                lore.add("§eRight click to edit rewards");
                lore.add("§eShift + Right click to edit prefix");

                meta.setLore(lore);
                item.setItemMeta(meta);

                gui.setItem(slot++, item);
            }
        }

        // Add new tier button
        ItemStack newTier = new ItemStack(Material.EMERALD);
        ItemMeta meta = newTier.getItemMeta();
        meta.setDisplayName("§a§lAdd New Tier");
        List<String> lore = new ArrayList<>();
        lore.add("§7Click to add a new tier");
        meta.setLore(lore);
        newTier.setItemMeta(meta);
        gui.setItem(53, newTier);

        player.openInventory(gui);
    }

    private void giveGift(Player player, int amount) {
        ItemStack gift = new ItemStack(Material.CHEST, amount);
        ItemMeta meta = gift.getItemMeta();
        meta.setDisplayName("§c§lChristmas Gift");
        List<String> lore = new ArrayList<>();
        lore.add("§7Right click to open!");
        lore.add("§7✦ Contains special rewards!");
        lore.add("§7✦ From Common to Mythic");
        meta.setLore(lore);
        gift.setItemMeta(meta);

        player.getInventory().addItem(gift);
    }
}

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
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
            openAdminGUI((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /christmasgift give <amount>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[1]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§cAmount must be between 1-64!");
                    return true;
                }
                giveGift((Player) sender, amount);
                sender.sendMessage("§aYou have received " + amount + " Christmas Gift!");
            } catch (NumberFormatException e) {
                sender.sendMessage("§cAmount must be a number!");
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

        sender.sendMessage("§cUsage: /christmasgift [give <amount>|reload]");
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
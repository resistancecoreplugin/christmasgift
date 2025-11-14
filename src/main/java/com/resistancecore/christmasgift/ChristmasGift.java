package com.resistancecore.christmasgift;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class ChristmasGift extends JavaPlugin {
    private static ChristmasGift instance;
    private FileConfiguration rewardsConfig;
    private File rewardsFile;
    private AdvancedRewardSystem rewardSystem;
    private GiftEffects giftEffects;
    private RouletteSystem rouletteSystem;
    private ComboSystem comboSystem;

    @Override
    public void onEnable() {
        instance = this;
        
        // Setup configs
        saveDefaultConfig();
        setupRewardsConfig();
        
        // Initialize systems
        this.rewardSystem = new AdvancedRewardSystem(this);
        this.giftEffects = new GiftEffects(this);
        this.rouletteSystem = new RouletteSystem(this);
        this.comboSystem = new ComboSystem(this); // NEW
        
        // Register events
        getServer().getPluginManager().registerEvents(new GiftListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiHandler(this), this);
        
        // Register commands
        getCommand("christmasgift").setExecutor(new GiftCommand(this));
        
        getLogger().info("ChristmasGift Plugin has been enabled!");
        getLogger().info("Roulette System activated! 🎰");
        getLogger().info("Combo System activated! 🔥");
    }
    
    @Override
    public void onDisable() {
        // Cancel all running tasks
        getServer().getScheduler().cancelTasks(this);
        
        // Save configs
        saveRewardsConfig();
        
        getLogger().info("ChristmasGift Plugin has been disabled!");
    }
    
    public static ChristmasGift getInstance() {
        return instance;
    }
    
    private void setupRewardsConfig() {
        rewardsFile = new File(getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            rewardsFile.getParentFile().mkdirs();
            saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
    }
    
    public void saveRewardsConfig() {
        if (rewardsConfig == null || rewardsFile == null) {
            return;
        }
        try {
            rewardsConfig.save(rewardsFile);
        } catch (Exception e) {
            getLogger().severe("Could not save rewards config: " + e.getMessage());
        }
    }
    
    public FileConfiguration getRewardsConfig() {
        return rewardsConfig;
    }
    
    public AdvancedRewardSystem getRewardSystem() {
        return rewardSystem;
    }
    
    public GiftEffects getGiftEffects() {
        return giftEffects;
    }
    
    public RouletteSystem getRouletteSystem() {
        return rouletteSystem;
    }
    
    public ComboSystem getComboSystem() {
        return comboSystem;
    }
    
    public void reloadRewardsConfig() {
        // Save default rewards.yml if it doesn't exist
        if (!rewardsFile.exists()) {
            saveResource("rewards.yml", false);
        }
        
        // Reload rewards config from file
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        
        // Reload reward tiers
        if (rewardSystem != null) {
            rewardSystem.reloadRewardTiers();
        }
    }
}
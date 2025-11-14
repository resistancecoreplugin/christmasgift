package com.resistancecore.christmasgift;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GiftEffects {
    private final ChristmasGift plugin;

    public GiftEffects(ChristmasGift plugin) {
        this.plugin = plugin;
    }

    public void playOpeningAnimation(Player player) {
        Location loc = player.getLocation();
        
        new BukkitRunnable() {
            private int step = 0;
            private final int STEPS = 30; // Reduced from 40
            private double angle = 0;

            @Override
            public void run() {
                if (step >= STEPS || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                if (step < 8) { // Reduced from 10
                    // Initial burst
                    playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0F, 1.0F + (step * 0.1F));
                    spawnSpiral(loc, step);
                } else if (step < 16) { // Reduced from 20
                    // Rising particles
                    spawnRisingParticles(loc);
                    if (step % 2 == 0) { // Play sound every other tick
                        playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.0F + ((step-8) * 0.1F));
                    }
                } else if (step < 24) { // Reduced from 30
                    // Explosion effect
                    spawnFireworks(loc);
                    if (step == 16) {
                        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                    }
                } else {
                    // Final sparkles
                    spawnSparkles(loc);
                }

                angle += Math.PI / 6; // Slightly faster rotation
                step++;
            }

            private void spawnSpiral(Location loc, int step) {
                // Reduced particles for better performance
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 4) { // Reduced from PI/8
                    double x = Math.cos(i + angle) * (step * 0.2);
                    double z = Math.sin(i + angle) * (step * 0.2);
                    loc.add(x, 0.5, z);
                    player.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
                    loc.subtract(x, 0.5, z);
                }
            }

            private void spawnRisingParticles(Location loc) {
                // Reduced particles for better performance
                for (int i = 0; i < 6; i++) { // Reduced from 8
                    double x = Math.cos(angle + (i * Math.PI / 3)) * 0.8;
                    double z = Math.sin(angle + (i * Math.PI / 3)) * 0.8;
                    loc.add(x, 0, z);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 1, 0, 0, 0, 0.1);
                    player.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0.05);
                    loc.subtract(x, 0, z);
                }
            }

            private void spawnFireworks(Location loc) {
                // Reduced particles for better performance
                for (int i = 0; i < 12; i++) { // Reduced from 16
                    double x = Math.cos(i * Math.PI / 6) * 1.5;
                    double z = Math.sin(i * Math.PI / 6) * 1.5;
                    loc.add(x, 1, z);
                    player.getWorld().spawnParticle(Particle.WITCH, loc, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
                    loc.subtract(x, 1, z);
                }
            }

            private void spawnSparkles(Location loc) {
                // Reduced particles for better performance
                for (int i = 0; i < 3; i++) { // Reduced from 4
                    double x = (Math.random() - 0.5) * 2.5; // Reduced range
                    double y = Math.random() * 1.5; // Reduced range
                    double z = (Math.random() - 0.5) * 2.5; // Reduced range
                    loc.add(x, y, z);
                    player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, loc, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.FIREWORK, loc, 1, 0, 0, 0, 0);
                    loc.subtract(x, y, z);
                }
            }

            private void playSound(Player player, Sound sound, float volume, float pitch) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }.runTaskTimer(plugin, 0L, 2L); // Changed to 2L (every 2 ticks) for better performance
    }
}
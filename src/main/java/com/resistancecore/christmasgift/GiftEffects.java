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
            private final int STEPS = 30;
            private double angle = 0;

            @Override
            public void run() {
                if (step >= STEPS || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                if (step < 8) {
                    // Initial burst
                    playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0F, 1.0F + (step * 0.1F));
                    spawnSpiral(loc, step);
                } else if (step < 16) {
                    // Rising particles
                    spawnRisingParticles(loc);
                    if (step % 2 == 0) {
                        playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.0F + ((step-8) * 0.1F));
                    }
                } else if (step < 24) {
                    // Explosion effect
                    spawnFireworks(loc);
                    if (step == 16) {
                        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                    }
                } else {
                    // Final sparkles
                    spawnSparkles(loc);
                }

                angle += Math.PI / 6;
                step++;
            }

            private void spawnSpiral(Location loc, int step) {
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 4) {
                    double x = Math.cos(i + angle) * (step * 0.2);
                    double z = Math.sin(i + angle) * (step * 0.2);
                    Location spiralLoc = loc.clone().add(x, 0.5, z);
                    player.getWorld().spawnParticle(Particle.FLAME, spiralLoc, 1);
                }
            }

            private void spawnRisingParticles(Location loc) {
                for (int i = 0; i < 6; i++) {
                    double x = Math.cos(angle + (i * Math.PI / 3)) * 0.8;
                    double z = Math.sin(angle + (i * Math.PI / 3)) * 0.8;
                    Location risingLoc = loc.clone().add(x, 0, z);
                    player.getWorld().spawnParticle(Particle.CLOUD, risingLoc, 1);
                    player.getWorld().spawnParticle(Particle.CRIT, risingLoc, 1);
                }
            }

            private void spawnFireworks(Location loc) {
                for (int i = 0; i < 12; i++) {
                    double x = Math.cos(i * Math.PI / 6) * 1.5;
                    double z = Math.sin(i * Math.PI / 6) * 1.5;
                    Location fireworkLoc = loc.clone().add(x, 1, z);
                    player.getWorld().spawnParticle(Particle.FLAME, fireworkLoc, 1);
                    player.getWorld().spawnParticle(Particle.CRIT, fireworkLoc, 1);
                }
            }

            private void spawnSparkles(Location loc) {
                for (int i = 0; i < 3; i++) {
                    double x = (Math.random() - 0.5) * 2.5;
                    double y = Math.random() * 1.5;
                    double z = (Math.random() - 0.5) * 2.5;
                    Location sparkleLoc = loc.clone().add(x, y, z);
                    player.getWorld().spawnParticle(Particle.CRIT_MAGIC, sparkleLoc, 2);
                    player.getWorld().spawnParticle(Particle.ENCHANT, sparkleLoc, 2);
                }
            }

            private void playSound(Player player, Sound sound, float volume, float pitch) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}

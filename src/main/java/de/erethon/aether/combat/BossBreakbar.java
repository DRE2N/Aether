package de.erethon.aether.combat;

import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.qxl.QxlInstanceScope;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;

/** Defiance window owned by one configured Aether mob. All values are encounter settings. */
public final class BossBreakbar {

    private final AetherBaseMob mob;
    private final BossBar bar;
    private final double maximum;
    private double windowMaximum;
    private final boolean autoWindow;
    private final int windowTicks;
    private final int intervalTicks;
    private final int brokenTicks;
    private final double exposedMultiplier;
    private final double viewRadius;
    private int untilWindow = 40;
    private int windowRemaining;
    private int brokenRemaining;
    private int ticks;
    private double pressure;
    private boolean wasNoAi;
    private boolean audiencePresent;
    private boolean failed;

    public BossBreakbar(AetherBaseMob mob, double maximum, int windowTicks, int intervalTicks,
                        int brokenTicks, double exposedMultiplier, double viewRadius, String name,
                        boolean autoWindow) {
        this.mob = mob;
        this.maximum = Math.max(1.0, maximum);
        this.windowMaximum = this.maximum;
        this.autoWindow = autoWindow;
        this.windowTicks = Math.max(20, windowTicks);
        this.intervalTicks = Math.max(this.windowTicks + 20, intervalTicks);
        this.brokenTicks = Math.max(20, brokenTicks);
        this.exposedMultiplier = Math.max(1.0, exposedMultiplier);
        this.viewRadius = Math.max(8.0, viewRadius);
        this.bar = Bukkit.createBossBar(name, BarColor.BLUE, BarStyle.SEGMENTED_10);
        bar.setVisible(false);
    }

    public void tick() {
        if (mob.isDeadOrDying() || !mob.isAlive()) {
            close();
            return;
        }
        if (++ticks % 10 == 0) syncAudience();
        if (!audiencePresent) {
            if (brokenRemaining > 0) mob.setNoAi(wasNoAi);
            brokenRemaining = 0;
            windowRemaining = 0;
            pressure = 0;
            failed = false;
            untilWindow = 40;
            bar.setVisible(false);
            return;
        }
        if (brokenRemaining > 0) {
            if (--brokenRemaining == 0) {
                mob.setNoAi(wasNoAi);
                pressure = 0;
                untilWindow = Math.max(20, intervalTicks - windowTicks);
                bar.setVisible(false);
            }
        } else if (windowRemaining > 0) {
            if (--windowRemaining == 0) {
                pressure = 0;
                failed = true;
                untilWindow = Math.max(20, intervalTicks - windowTicks);
                bar.setVisible(false);
            }
        } else if (autoWindow && --untilWindow <= 0) {
            openWindow();
        }
    }

    public void openWindow() {
        if (brokenRemaining > 0 || windowRemaining > 0) return;
        syncAudience();
        long nearbyParticipants = bar.getPlayers().stream()
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR
                        && player.getLocation().distanceSquared(mob.getBukkitLivingEntity().getLocation()) <= 144)
                .count();
        windowMaximum = maximum * (1.0 + 0.4 * Math.max(0, Math.min(4, nearbyParticipants) - 1));
        pressure = 0;
        failed = false;
        windowRemaining = windowTicks;
        bar.setColor(BarColor.BLUE);
        bar.setProgress(1.0);
        bar.setVisible(true);
        mob.getBukkitLivingEntity().getWorld().playSound(mob.getBukkitLivingEntity().getLocation(),
                Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.85f);
        syncAudience();
    }

    public boolean apply(double amount) {
        if (windowRemaining <= 0 || brokenRemaining > 0 || !Double.isFinite(amount)) return false;
        pressure = Math.min(windowMaximum, pressure + Math.max(0, amount));
        bar.setProgress(Math.max(0.0, 1.0 - pressure / windowMaximum));
        if (pressure >= windowMaximum) {
            windowRemaining = 0;
            brokenRemaining = brokenTicks;
            wasNoAi = mob.isNoAi();
            mob.setNoAi(true);
            bar.setColor(BarColor.YELLOW);
            bar.setProgress(1.0);
            var location = mob.getBukkitLivingEntity().getLocation();
            location.getWorld().playSound(location, Sound.BLOCK_ANVIL_BREAK, 1.2f, 0.9f);
            location.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    location.add(0, mob.getBukkitLivingEntity().getHeight() * 0.5, 0),
                    24, 0.7, 0.7, 0.7, 0.1);
        }
        return true;
    }

    public boolean isActive() { return windowRemaining > 0; }
    public boolean isBroken() { return brokenRemaining > 0; }
    /** Stays true after a missed window until the next authored window opens. */
    public boolean hasFailed() { return failed; }
    public double damageMultiplier() { return isBroken() ? exposedMultiplier : 1.0; }

    private void syncAudience() {
        var entity = mob.getBukkitLivingEntity();
        for (Player player : entity.getWorld().getNearbyPlayers(entity.getLocation(), viewRadius)) {
            if (QxlInstanceScope.canInteract(mob, player) && player.canSee(entity)) {
                if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
            }
        }
        for (Player player : bar.getPlayers().toArray(new Player[0])) {
            if (!player.isOnline() || player.getWorld() != entity.getWorld()
                    || player.getLocation().distanceSquared(entity.getLocation()) > viewRadius * viewRadius
                    || !QxlInstanceScope.canInteract(mob, player) || !player.canSee(entity)) {
                bar.removePlayer(player);
            }
        }
        audiencePresent = !bar.getPlayers().isEmpty();
    }

    public void close() {
        if (brokenRemaining > 0 && !mob.isRemoved()) mob.setNoAi(wasNoAi);
        bar.removeAll();
        bar.setVisible(false);
    }
}

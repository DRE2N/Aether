package de.erethon.aether.creature.boss;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ModelledMob;
import de.erethon.aether.creature.NPCData;
import de.erethon.papyrus.CraftPDamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Base class for Aether boss encounter
 *
 * Provides:
 *   - Phase system driven by HP thresholds
 *   - Defiance (breakbar) mechanic for crowd-control interaction
 *   - Boss bar and optional defiance bar displayed to all participants
 *   - Arena boundary enforcement
 *   - Enrage timer
 *   - Mechanic scheduling helpers
 *   - Broadcast utilities (messages and titles)
 *
 * Subclasses should call the setup helpers (setupBossBar, setArena, setEnrageTimer,
 * phaseThresholds) during their constructor or onLoad, and then override the hook
 * methods to implement fight logic.
 */
public abstract class AbstractAetherBoss extends ModelledMob {

    // ── Phase ─────────────────────────────────────────────────────────────────

    private int currentPhase = 0;
    /**
     * HP fraction thresholds that advance the phase counter, in descending order.
     * Example: {0.75, 0.50, 0.25} triggers phases 1, 2, 3 at those HP percentages.
     */
    protected double[] phaseThresholds = {};

    // ── Defiance bar ──────────────────────────────────────────────────────────

    private double breakbarMax = 100.0;
    private double breakbarDamage = 0.0;
    private boolean breakbarBroken = false;
    private int breakbarBrokenTicksLeft = 0;
    /** How many ticks the boss remains in the Broken state before recovering. */
    protected int breakbarBrokenDuration = 100; // 5 s
    /** Incoming damage multiplier while the defiance bar is broken. */
    protected double breakbarDamageBuff = 1.5;
    private BossBar defianceBar;

    // ── HP boss bar ───────────────────────────────────────────────────────────

    private BossBar bossBar;
    private static final int BOSSBAR_SYNC_INTERVAL = 10; // ticks

    // ── Arena ─────────────────────────────────────────────────────────────────

    private Location arenaCenter;
    private double arenaRadiusSq;
    private boolean arenaEnabled = false;
    private static final int ARENA_CHECK_INTERVAL = 20; // ticks

    // ── Enrage ────────────────────────────────────────────────────────────────

    private int enrageAfterTicks = -1;
    private boolean enraged = false;

    // ── Internal tick counter ─────────────────────────────────────────────────

    private int bossTick = 0;

    // ── Scheduled mechanics ───────────────────────────────────────────────────

    private final List<BukkitTask> scheduledTasks = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    protected AbstractAetherBoss(EntityType<? extends Mob> type, Level world) {
        super(type, world);
    }

    protected AbstractAetherBoss(NPCData data, World world) {
        this(data, world, null);
    }

    protected AbstractAetherBoss(NPCData data, World world, Integer overrideLevel) {
        super(data, world, overrideLevel);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        bossTick++;

        if (isDeadOrDying()) return;

        checkPhaseTransition();
        checkEnrage();
        tickBreakbar();

        if (bossTick % BOSSBAR_SYNC_INTERVAL == 0) {
            syncBossBars();
        }
        if (arenaEnabled && bossTick % ARENA_CHECK_INTERVAL == 0) {
            checkArenaBoundary();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount, CraftPDamageType type) {
        float finalAmount = breakbarBroken ? amount * (float) breakbarDamageBuff : amount;
        return super.hurtServer(level, source, finalAmount, type);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        teardown();
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        super.onRemoval(reason);
        teardown();
    }

    private void teardown() {
        scheduledTasks.forEach(BukkitTask::cancel);
        scheduledTasks.clear();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        if (defianceBar != null) {
            defianceBar.removeAll();
            defianceBar = null;
        }
    }

    // ── Phase system ──────────────────────────────────────────────────────────

    private void checkPhaseTransition() {
        if (currentPhase >= phaseThresholds.length) return;
        double hpFraction = getHealth() / getMaxHealth();
        if (hpFraction <= phaseThresholds[currentPhase]) {
            currentPhase++;
            onPhaseStart(currentPhase);
        }
    }

    /** Returns the current phase index (0 = pre-first threshold, 1+ after each crossing). */
    protected int getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Called when the boss crosses a phase threshold.
     * @param phase 1-based phase index, matches the threshold that was crossed.
     */
    protected void onPhaseStart(int phase) {}

    // ── Defiance / Breakbar ───────────────────────────────────────────────────

    /**
     * Applies crowd-control damage to the defiance bar.
     * When the bar fills, the boss is Broken: it takes bonus damage and triggers
     * {@link #onBreakbarBroken()} until it recovers after {@link #breakbarBrokenDuration} ticks.
     */
    public void damageBreakbar(double amount) {
        if (breakbarBroken) return;
        breakbarDamage = Math.min(breakbarDamage + amount, breakbarMax);
        if (breakbarDamage >= breakbarMax) {
            breakBreakbar();
        }
        syncDefianceBar();
    }

    private void breakBreakbar() {
        breakbarBroken = true;
        breakbarBrokenTicksLeft = breakbarBrokenDuration;
        onBreakbarBroken();
    }

    private void tickBreakbar() {
        if (!breakbarBroken) return;
        breakbarBrokenTicksLeft--;
        if (breakbarBrokenTicksLeft <= 0) {
            breakbarBroken = false;
            breakbarDamage = 0.0;
            syncDefianceBar();
            onBreakbarRecovered();
        }
    }

    protected void setBreakbarMax(double max) {
        this.breakbarMax = max;
    }

    public boolean isBreakbarBroken() {
        return breakbarBroken;
    }

    /** Returns defiance bar fill as a fraction [0, 1]. */
    public double getBreakbarProgress() {
        return breakbarDamage / breakbarMax;
    }

    /** Called when the defiance bar is fully depleted and the boss is Broken. */
    protected void onBreakbarBroken() {}

    /** Called when the Broken state expires and the defiance bar resets. */
    protected void onBreakbarRecovered() {}

    // ── Boss bar display ──────────────────────────────────────────────────────

    /**
     * Creates and enables the HP boss bar shown to all fight participants.
     * Call this during construction or {@code onPhaseStart} to change appearance.
     */
    protected void setupBossBar(String name, BarColor color, BarStyle style) {
        if (bossBar != null) bossBar.removeAll();
        bossBar = Bukkit.createBossBar(name, color, style);
    }

    /**
     * Creates and enables a secondary defiance bar shown below the HP bar.
     * Automatically switches to yellow while the boss is Broken.
     */
    protected void setupDefianceBar(String name) {
        if (defianceBar != null) defianceBar.removeAll();
        defianceBar = Bukkit.createBossBar(name, BarColor.BLUE, BarStyle.SEGMENTED_10);
    }

    private void syncBossBars() {
        if (bossBar == null && defianceBar == null) return;

        for (Player player : getDamageParticipants()) {
            if (bossBar != null && !bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
            if (defianceBar != null && !defianceBar.getPlayers().contains(player)) {
                defianceBar.addPlayer(player);
            }
        }

        if (bossBar != null) {
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, getHealth() / getMaxHealth())));
        }
        syncDefianceBar();
    }

    private void syncDefianceBar() {
        if (defianceBar == null) return;
        if (breakbarBroken) {
            defianceBar.setProgress(1.0);
            defianceBar.setColor(BarColor.YELLOW);
        } else {
            defianceBar.setProgress(Math.min(1.0, breakbarDamage / breakbarMax));
            defianceBar.setColor(BarColor.BLUE);
        }
    }

    // ── Arena boundary ────────────────────────────────────────────────────────

    /**
     * Enables arena boundary enforcement.
     * Players who stray beyond {@code radius} blocks from {@code center} trigger
     * {@link #onPlayerLeftArena(Player)}.
     */
    protected void setArena(Location center, double radius) {
        this.arenaCenter = center;
        this.arenaRadiusSq = radius * radius;
        this.arenaEnabled = true;
    }

    protected boolean isInArena(Location location) {
        if (arenaCenter == null) return true;
        return location.distanceSquared(arenaCenter) <= arenaRadiusSq;
    }

    private void checkArenaBoundary() {
        if (arenaCenter == null) return;
        for (Player player : getDamageParticipants()) {
            if (player.isOnline() && !isInArena(player.getLocation())) {
                onPlayerLeftArena(player);
            }
        }
    }

    /**
     * Called when a participant leaves the arena boundary.
     * Default: teleport back to arena center. Override for custom behavior
     * (e.g., send a warning, deal fall damage, instakill).
     */
    protected void onPlayerLeftArena(Player player) {
        player.teleport(arenaCenter);
    }

    // ── Enrage ────────────────────────────────────────────────────────────────

    /**
     * Sets an enrage timer. After {@code ticks} boss ticks have elapsed,
     * {@link #onEnrage()} is called once.
     */
    protected void setEnrageTimer(int ticks) {
        this.enrageAfterTicks = ticks;
    }

    public boolean isEnraged() {
        return enraged;
    }

    private void checkEnrage() {
        if (enraged || enrageAfterTicks < 0) return;
        if (bossTick >= enrageAfterTicks) {
            enraged = true;
            onEnrage();
        }
    }

    /** Called once when the enrage timer expires. */
    protected void onEnrage() {}

    // ── Announcements ─────────────────────────────────────────────────────────

    /** Sends a chat message to every player who has participated in this fight. */
    protected void announceToParticipants(Component message) {
        for (Player player : getDamageParticipants()) {
            player.sendMessage(message);
        }
    }

    /** Shows a title + subtitle to all participants with the default GW2-style timing. */
    protected void broadcastTitle(Component title, Component subtitle) {
        broadcastTitle(title, subtitle, 10, 70, 20);
    }

    protected void broadcastTitle(Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Title t = Title.title(title, subtitle, Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)));
        for (Player player : getDamageParticipants()) {
            player.showTitle(t);
        }
    }

    // ── Mechanic scheduling ───────────────────────────────────────────────────

    /**
     * Schedules a one-shot mechanic that auto-cancels if the boss dies before it fires.
     * Returned task is tracked and cancelled on boss death/removal.
     */
    protected BukkitTask scheduleMechanic(int delayTicks, Runnable mechanic) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(Aether.getInstance(), () -> {
            if (!isDeadOrDying()) mechanic.run();
        }, delayTicks);
        scheduledTasks.add(task);
        return task;
    }

    /**
     * Schedules a repeating mechanic that auto-cancels if the boss dies.
     * Returned task is tracked and cancelled on boss death/removal.
     */
    protected BukkitTask scheduleMechanicRepeating(int delayTicks, int intervalTicks, Runnable mechanic) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Aether.getInstance(), () -> {
            if (!isDeadOrDying()) mechanic.run();
        }, delayTicks, intervalTicks);
        scheduledTasks.add(task);
        return task;
    }

    /** Cancels and removes a previously scheduled mechanic task. */
    protected void cancelMechanic(BukkitTask task) {
        task.cancel();
        scheduledTasks.remove(task);
    }

    // ── Attack telegraphs ─────────────────────────────────────────────────────

    // Particle colors for the two telegraph states
    private static final Particle.DustOptions TELEGRAPH_WARNING = new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.5f);
    private static final Particle.DustOptions TELEGRAPH_DANGER  = new Particle.DustOptions(Color.fromRGB(220, 30,  30), 1.5f);
    /** Ticks remaining at which the telegraph color changes from orange to red. */
    private static final int TELEGRAPH_DANGER_TICKS = 40;
    /** How often (in ticks) the telegraph shape is redrawn. */
    private static final int TELEGRAPH_DRAW_INTERVAL = 3;

    /**
     * Draws a circular AoE indicator on the ground that changes from orange to
     * red in the final 2 seconds, then fires {@code onActivate}.
     *
     * @param center       center of the circle
     * @param radius       radius in blocks
     * @param warningTicks how long to show the warning before activating
     * @param onActivate   mechanic to trigger when the telegraph expires
     */
    protected BukkitTask telegraphCircle(Location center, double radius, int warningTicks, Runnable onActivate) {
        return runTelegraph(warningTicks, color -> drawCircle(center.getWorld(), center, radius, color), onActivate);
    }

    /**
     * Draws a line indicator (cleave / laser path) that changes color and fires.
     */
    protected BukkitTask telegraphLine(Location from, Location to, int warningTicks, Runnable onActivate) {
        return runTelegraph(warningTicks, color -> drawLine(from.getWorld(), from, to, color), onActivate);
    }

    /**
     * Draws a cone indicator in front of the boss.
     *
     * @param origin       tip of the cone (usually the boss location)
     * @param yaw          facing direction in Minecraft yaw degrees (0 = south)
     * @param length       cone reach in blocks
     * @param halfAngleDeg half-width of the cone in degrees (e.g. 30 for a 60° cone)
     */
    protected BukkitTask telegraphCone(Location origin, float yaw, double length, float halfAngleDeg, int warningTicks, Runnable onActivate) {
        return runTelegraph(warningTicks, color -> drawCone(origin.getWorld(), origin, yaw, length, halfAngleDeg, color), onActivate);
    }

    /**
     * Draws a shrinking ring that collapses onto {@code target} as the telegraph
     * expires — useful for telegraphing a targeted meteor or AoE drop.
     *
     * @param target       impact location
     * @param radius       starting radius of the ring; shrinks to zero on activation
     */
    protected BukkitTask telegraphPoint(Location target, double radius, int warningTicks, Runnable onActivate) {
        int totalDraws = Math.max(1, warningTicks / TELEGRAPH_DRAW_INTERVAL);
        AtomicInteger remaining = new AtomicInteger(totalDraws);
        BukkitTask[] taskHolder = {null};
        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(Aether.getInstance(), () -> {
            if (isDeadOrDying()) {
                taskHolder[0].cancel();
                return;
            }
            int left = remaining.decrementAndGet();
            double currentRadius = radius * ((double) left / totalDraws);
            Particle.DustOptions color = left > TELEGRAPH_DANGER_TICKS / TELEGRAPH_DRAW_INTERVAL ? TELEGRAPH_WARNING : TELEGRAPH_DANGER;
            drawCircle(target.getWorld(), target, Math.max(0.05, currentRadius), color);
            if (left <= 0) {
                taskHolder[0].cancel();
                scheduledTasks.remove(taskHolder[0]);
                if (!isDeadOrDying()) onActivate.run();
            }
        }, 0L, TELEGRAPH_DRAW_INTERVAL);
        scheduledTasks.add(taskHolder[0]);
        return taskHolder[0];
    }

    /** Common timing/color logic shared by non-point telegraphs. */
    private BukkitTask runTelegraph(int warningTicks, Consumer<Particle.DustOptions> draw, Runnable onActivate) {
        int dangerDrawThreshold = TELEGRAPH_DANGER_TICKS / TELEGRAPH_DRAW_INTERVAL;
        AtomicInteger remaining = new AtomicInteger(Math.max(1, warningTicks / TELEGRAPH_DRAW_INTERVAL));
        BukkitTask[] taskHolder = {null};
        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(Aether.getInstance(), () -> {
            if (isDeadOrDying()) {
                taskHolder[0].cancel();
                return;
            }
            int left = remaining.decrementAndGet();
            draw.accept(left > dangerDrawThreshold ? TELEGRAPH_WARNING : TELEGRAPH_DANGER);
            if (left <= 0) {
                taskHolder[0].cancel();
                scheduledTasks.remove(taskHolder[0]);
                if (!isDeadOrDying()) onActivate.run();
            }
        }, 0L, TELEGRAPH_DRAW_INTERVAL);
        scheduledTasks.add(taskHolder[0]);
        return taskHolder[0];
    }

    // ── Shape drawing helpers ─────────────────────────────────────────────────

    private void drawCircle(World world, Location center, double radius, Particle.DustOptions color) {
        int points = Math.min(120, Math.max(16, (int) (2 * Math.PI * radius * 4)));
        double step = 2 * Math.PI / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            world.spawnParticle(Particle.DUST,
                    center.getX() + radius * Math.cos(angle),
                    center.getY() + 0.1,
                    center.getZ() + radius * Math.sin(angle),
                    1, 0, 0, 0, 0, color);
        }
    }

    private void drawLine(World world, Location from, Location to, Particle.DustOptions color) {
        double length = from.distance(to);
        int points = Math.max(4, (int) (length * 3));
        double dx = (to.getX() - from.getX()) / points;
        double dy = (to.getY() - from.getY()) / points;
        double dz = (to.getZ() - from.getZ()) / points;
        for (int i = 0; i <= points; i++) {
            world.spawnParticle(Particle.DUST,
                    from.getX() + dx * i,
                    from.getY() + dy * i + 0.1,
                    from.getZ() + dz * i,
                    1, 0, 0, 0, 0, color);
        }
    }

    /**
     * Draws a filled cone on the ground using radial lines and a far arc.
     * Minecraft yaw: 0 = south (+Z), 90 = west (-X), 180 = north (-Z), 270 = east (+X).
     */
    private void drawCone(World world, Location origin, float yaw, double length, float halfAngleDeg, Particle.DustOptions color) {
        double baseRad = Math.toRadians(yaw);
        double halfRad = Math.toRadians(halfAngleDeg);
        int radials     = Math.max(3, (int) (halfAngleDeg * 2 / 10));
        int pointPerRay = Math.max(4, (int) (length * 2));
        int arcPoints   = Math.max(8, (int) (halfAngleDeg * 2 / 360.0 * 2 * Math.PI * length * 4));

        // Radial lines from tip to far end
        for (int r = 0; r <= radials; r++) {
            double rayAngle = baseRad - halfRad + (halfRad * 2 * r / radials);
            double dx = -Math.sin(rayAngle);
            double dz =  Math.cos(rayAngle);
            for (int p = 0; p <= pointPerRay; p++) {
                double dist = length * p / pointPerRay;
                world.spawnParticle(Particle.DUST,
                        origin.getX() + dx * dist,
                        origin.getY() + 0.1,
                        origin.getZ() + dz * dist,
                        1, 0, 0, 0, 0, color);
            }
        }

        // Closing arc at the far end
        for (int p = 0; p <= arcPoints; p++) {
            double arcAngle = baseRad - halfRad + (halfRad * 2 * p / arcPoints);
            world.spawnParticle(Particle.DUST,
                    origin.getX() - Math.sin(arcAngle) * length,
                    origin.getY() + 0.1,
                    origin.getZ() + Math.cos(arcAngle) * length,
                    1, 0, 0, 0, 0, color);
        }
    }

    // ── Combat utilities ──────────────────────────────────────────────────────

    /**
     * Launches all participants within {@code radius} blocks away from {@code origin}.
     * A slight upward component is added so players visibly leave the ground.
     */
    protected void knockbackFromPoint(Location origin, double radius, double strength) {
        double radiusSq = radius * radius;
        for (Player player : getDamageParticipants()) {
            if (!player.isOnline()) continue;
            if (player.getLocation().distanceSquared(origin) > radiusSq) continue;
            Vector dir = player.getLocation().toVector().subtract(origin.toVector());
            if (dir.lengthSquared() < 1e-4) dir = new Vector(0, 1, 0);
            else dir.normalize();
            dir.setY(Math.max(dir.getY(), 0.3));
            player.setVelocity(dir.multiply(strength));
        }
    }

    /**
     * Pulls all participants within {@code radius} blocks toward {@code origin}.
     * Horizontal only — does not fling players into the air.
     */
    protected void pullToPoint(Location origin, double radius, double strength) {
        double radiusSq = radius * radius;
        for (Player player : getDamageParticipants()) {
            if (!player.isOnline()) continue;
            if (player.getLocation().distanceSquared(origin) > radiusSq) continue;
            Vector dir = origin.toVector().subtract(player.getLocation().toVector());
            if (dir.lengthSquared() < 1e-4) continue;
            dir.setY(0).normalize().multiply(strength);
            player.setVelocity(dir);
        }
    }

    // ── Sound utilities ───────────────────────────────────────────────────────

    /** Plays a sound at each participant's own position so distance falloff doesn't apply. */
    protected void playSoundToParticipants(Sound sound, float volume, float pitch) {
        for (Player player : getDamageParticipants()) {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    /** Plays a sound emanating from the boss location, subject to normal falloff. */
    protected void playBossSound(Sound sound, float volume, float pitch) {
        getBukkitEntity().getWorld().playSound(getBukkitEntity().getLocation(), sound, volume, pitch);
    }
}

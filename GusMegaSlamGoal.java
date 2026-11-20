package com.example.gusboss.entity.ai;

import com.example.gusboss.GusStats;
import com.example.gusboss.entity.GusEntity;
import java.util.EnumSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Attack 4 (phase 2): mark the target's position with a red "!" zone, wait, jump high and crash down on it.
 * The zone is locked when the warning starts, so players can run out of it.
 */
public class GusMegaSlamGoal extends Goal {
    private enum Phase { WARN, AIR, RECOVER, DONE }

    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 2.0F);

    private final GusEntity gus;
    private Phase phase = Phase.DONE;
    private int phaseTicks;
    private int nextUseTick;
    private Vec3 landing = Vec3.ZERO;
    private double airSpeed;

    public GusMegaSlamGoal(GusEntity gus) {
        this.gus = gus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.gus.getTarget();
        if (!this.gus.isPhase2() || this.gus.isTransitioning() || target == null || !target.isAlive()
                || !this.gus.onGround() || this.gus.tickCount < this.nextUseTick) {
            return false;
        }
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        double dx = target.getX() - this.gus.getX();
        double dz = target.getZ() - this.gus.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < GusStats.MEGA_MIN_DIST || dist > GusStats.MEGA_MAX_DIST) {
            return false;
        }
        return this.gus.getRandom().nextInt(4) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.phase != Phase.DONE && !this.gus.isTransitioning();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        LivingEntity target = this.gus.getTarget();
        this.landing = target != null ? target.position() : this.gus.position();
        this.phase = Phase.WARN;
        this.phaseTicks = 0;
        this.gus.setAggressive(true);
        this.gus.getNavigation().stop();
        this.gus.setLeapState(GusEntity.LEAP_WINDUP);   // the deep crouch doubles as the telegraph
        this.gus.playSound(SoundEvents.BELL_BLOCK, 3.0F, 0.5F);
    }

    @Override
    public void stop() {
        this.phase = Phase.DONE;
        this.gus.setLeapState(GusEntity.LEAP_NONE);
        this.nextUseTick = this.gus.tickCount + GusStats.MEGA_COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        LivingEntity target = this.gus.getTarget();
        this.phaseTicks++;

        if (this.phase == Phase.WARN) {
            this.gus.getNavigation().stop();
            if (target != null) {
                this.gus.getLookControl().setLookAt(target, 60.0F, 60.0F);
            }
            if (this.phaseTicks % 5 == 1 && this.gus.level() instanceof ServerLevel level) {
                this.drawMarker(level);
            }
            if (this.phaseTicks >= GusStats.MEGA_WARN_TICKS) {
                this.launch();
            }
        } else if (this.phase == Phase.AIR) {
            this.steerInAir();
            if (target != null) {
                this.gus.getLookControl().setLookAt(target, 60.0F, 60.0F);
            }
            if (this.phaseTicks > 4 && this.gus.onGround()) {
                this.land();
            } else if (this.phaseTicks > 90) {
                this.phase = Phase.DONE;   // safety net
            }
        } else if (this.phase == Phase.RECOVER) {
            this.gus.getNavigation().stop();
            if (target != null) {
                this.gus.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (this.phaseTicks >= GusStats.MEGA_RECOVER_TICKS) {
                this.phase = Phase.DONE;
            }
        }
    }

    /** Red ring at the damage radius plus an exclamation mark in the middle, drawn with dust particles. */
    private void drawMarker(ServerLevel level) {
        double r = GusStats.MEGA_RADIUS;
        double y = this.landing.y + 0.15D;

        int points = 40;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            this.dot(level, this.landing.x + Math.cos(angle) * r, y, this.landing.z + Math.sin(angle) * r);
        }
        // the bar of the "!"
        for (int i = 0; i <= 10; i++) {
            double z = -r * 0.55D + (r * 0.65D) * i / 10.0D;
            this.dot(level, this.landing.x - 0.3D, y, this.landing.z + z);
            this.dot(level, this.landing.x + 0.3D, y, this.landing.z + z);
        }
        // the dot of the "!"
        for (int i = 0; i < 3; i++) {
            double z = r * 0.45D + i * 0.5D;
            this.dot(level, this.landing.x - 0.3D, y, this.landing.z + z);
            this.dot(level, this.landing.x + 0.3D, y, this.landing.z + z);
        }
    }

    private void dot(ServerLevel level, double x, double y, double z) {
        level.sendParticles(RED, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private void launch() {
        Vec3 pos = this.gus.position();
        double dx = this.landing.x - pos.x;
        double dz = this.landing.z - pos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        this.airSpeed = Mth.clamp(dist / GusStats.MEGA_FLIGHT_TICKS, 0.25D, 1.4D);
        double hx = dist > 1.0E-4D ? dx / dist * this.airSpeed : 0.0D;
        double hz = dist > 1.0E-4D ? dz / dist * this.airSpeed : 0.0D;
        this.gus.setDeltaMovement(hx, GusStats.MEGA_UP_SPEED, hz);
        this.gus.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0F, 0.4F);
        this.gus.setLeapState(GusEntity.LEAP_AIR);
        this.phase = Phase.AIR;
        this.phaseTicks = 0;
    }

    /** Constant horizontal speed toward the marked point (vertical motion stays vanilla). */
    private void steerInAir() {
        Vec3 pos = this.gus.position();
        double dx = this.landing.x - pos.x;
        double dz = this.landing.z - pos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        Vec3 motion = this.gus.getDeltaMovement();
        if (dist > 0.6D) {
            double speed = Math.min(this.airSpeed, dist);
            this.gus.setDeltaMovement(dx / dist * speed, motion.y, dz / dist * speed);
        } else {
            this.gus.setDeltaMovement(0.0D, motion.y, 0.0D);
        }
    }

    private void land() {
        this.gus.setLeapState(GusEntity.LEAP_SLAM);
        this.gus.megaSlam();
        this.phase = Phase.RECOVER;
        this.phaseTicks = 0;
    }
}

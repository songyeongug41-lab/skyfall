package com.example.gusboss.entity.ai;

import com.example.gusboss.GusStats;
import com.example.gusboss.entity.GusEntity;
import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Attack 2: crouch, leap at the target's position, slam into the ground. */
public class GusLeapSlamGoal extends Goal {
    private enum Phase { WINDUP, AIR, RECOVER, DONE }

    private final GusEntity gus;
    private Phase phase = Phase.DONE;
    private int phaseTicks;
    private int nextUseTick;
    private Vec3 landing = Vec3.ZERO;
    private double airSpeed;

    public GusLeapSlamGoal(GusEntity gus) {
        this.gus = gus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.gus.getTarget();
        if (target == null || !target.isAlive() || !this.gus.onGround() || this.gus.tickCount < this.nextUseTick) {
            return false;
        }
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        double dx = target.getX() - this.gus.getX();
        double dz = target.getZ() - this.gus.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < GusStats.LEAP_MIN_DIST || dist > GusStats.LEAP_MAX_DIST) {
            return false;
        }
        if (!this.gus.getSensing().hasLineOfSight(target)) {
            return false;
        }
        return this.gus.getRandom().nextInt(3) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.phase != Phase.DONE;
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
        this.phase = Phase.WINDUP;
        this.phaseTicks = 0;
        this.gus.setAggressive(true);
        this.gus.getNavigation().stop();
        this.gus.setLeapState(GusEntity.LEAP_WINDUP);
    }

    @Override
    public void stop() {
        this.phase = Phase.DONE;
        this.gus.setLeapState(GusEntity.LEAP_NONE);
        this.nextUseTick = this.gus.tickCount + this.gus.getLeapCooldownTicks();
    }

    @Override
    public void tick() {
        LivingEntity target = this.gus.getTarget();
        this.phaseTicks++;

        if (this.phase == Phase.WINDUP) {
            this.gus.getNavigation().stop();
            if (target != null) {
                this.gus.getLookControl().setLookAt(target, 60.0F, 60.0F);
            }
            if (this.phaseTicks >= GusStats.LEAP_WINDUP_TICKS) {
                this.launch(target);
            }
        } else if (this.phase == Phase.AIR) {
            this.steerInAir();
            if (target != null) {
                this.gus.getLookControl().setLookAt(target, 60.0F, 60.0F);
            }
            if (this.phaseTicks > 4 && this.gus.onGround()) {
                this.land();
            } else if (this.phaseTicks > 80) {
                this.phase = Phase.DONE;   // safety net: never get stuck in the air state
            }
        } else if (this.phase == Phase.RECOVER) {
            this.gus.getNavigation().stop();
            if (target != null) {
                this.gus.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            if (this.phaseTicks >= GusStats.LEAP_RECOVER_TICKS) {
                this.phase = Phase.DONE;
            }
        }
    }

    private void launch(LivingEntity target) {
        Vec3 pos = this.gus.position();
        if (target != null && target.isAlive()) {
            this.landing = target.position();
        } else {
            Vec3 look = this.gus.getLookAngle();
            this.landing = pos.add(look.x * 8.0D, 0.0D, look.z * 8.0D);
        }

        double dx = this.landing.x - pos.x;
        double dz = this.landing.z - pos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        this.airSpeed = Mth.clamp(dist / GusStats.LEAP_FLIGHT_TICKS, 0.25D, 1.1D);

        double hx = dist > 1.0E-4D ? dx / dist * this.airSpeed : 0.0D;
        double hz = dist > 1.0E-4D ? dz / dist * this.airSpeed : 0.0D;
        this.gus.setDeltaMovement(hx, GusStats.LEAP_UP_SPEED, hz);

        this.gus.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.5F);
        this.gus.setLeapState(GusEntity.LEAP_AIR);
        this.phase = Phase.AIR;
        this.phaseTicks = 0;
    }

    /** Keep a constant horizontal speed toward the locked landing point (vertical motion stays vanilla). */
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
        this.gus.groundSlam();
        this.phase = Phase.RECOVER;
        this.phaseTicks = 0;
    }
}

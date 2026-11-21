package com.example.gusboss.entity.ai;

import com.example.gusboss.GusStats;
import com.example.gusboss.entity.GusEntity;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Attack 3 (phase 2): grab one player, lift them overhead, hurl them forward.
 * The damage is dealt when the thrown player actually lands, so motion and hit always agree.
 */
public class GusGrabThrowGoal extends Goal {
    private enum Phase { REACH, HOLD, THROW, RECOVER, DONE }

    private final GusEntity gus;
    private Phase phase = Phase.DONE;
    private int phaseTicks;
    private int nextUseTick;

    @Nullable
    private LivingEntity held;      // currently carried
    @Nullable
    private LivingEntity thrown;    // in flight
    private int flightTicks;

    public GusGrabThrowGoal(GusEntity gus) {
        this.gus = gus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.gus.isPhase2() || this.gus.tickCount < this.nextUseTick
                || !this.gus.onGround() || !this.gus.getPassengers().isEmpty()) {
            return false;
        }
        LivingEntity target = this.gus.getTarget();
        if (!(target instanceof Player player) || !player.isAlive() || player.isCreative()
                || player.isSpectator() || player.isPassenger()) {
            return false;
        }
        double dx = player.getX() - this.gus.getX();
        double dz = player.getZ() - this.gus.getZ();
        if (Math.sqrt(dx * dx + dz * dz) > GusStats.GRAB_RANGE || Math.abs(player.getY() - this.gus.getY()) > 2.5D) {
            return false;
        }
        if (!this.gus.getSensing().hasLineOfSight(player)) {
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
        this.phase = Phase.REACH;
        this.phaseTicks = 0;
        this.held = null;
        this.thrown = null;
        this.gus.setAggressive(true);
        this.gus.getNavigation().stop();
        this.gus.setAction(GusEntity.ACTION_GRAB_REACH);
    }

    @Override
    public void stop() {
        this.gus.ejectPassengers();   // never leave anyone stuck on Gus
        this.held = null;
        this.thrown = null;
        this.phase = Phase.DONE;
        this.gus.setAction(GusEntity.ACTION_NONE);
        this.nextUseTick = this.gus.tickCount + GusStats.GRAB_COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        this.phaseTicks++;
        this.gus.getNavigation().stop();
        LivingEntity target = this.gus.getTarget();

        this.monitorFlight();

        if (this.phase == Phase.REACH) {
            if (target != null) {
                this.gus.getLookControl().setLookAt(target, 60.0F, 60.0F);
            }
            if (this.phaseTicks >= GusStats.GRAB_REACH_TICKS) {
                if (target instanceof Player player && player.isAlive() && !player.isCreative()
                        && !player.isSpectator() && !player.isPassenger()
                        && this.gus.distanceTo(player) <= GusStats.GRAB_RANGE + 1.0D) {
                    this.grab(player);
                } else {
                    this.toRecover();   // whiffed
                }
            }
        } else if (this.phase == Phase.HOLD) {
            if (this.held == null || !this.held.isAlive() || this.held.isRemoved()) {
                this.held = null;
                this.toRecover();
            } else {
                if (this.held.getVehicle() != this.gus) {
                    this.held.startRiding(this.gus, true);   // sneaking off is not an option
                }
                if (this.phaseTicks >= GusStats.GRAB_HOLD_TICKS) {
                    this.phase = Phase.THROW;
                    this.phaseTicks = 0;
                    this.gus.setAction(GusEntity.ACTION_GRAB_THROW);
                }
            }
        } else if (this.phase == Phase.THROW) {
            if (this.phaseTicks == 3) {
                this.release();
            }
            if (this.phaseTicks >= GusStats.GRAB_THROW_TICKS) {
                this.toRecover();
            }
        } else if (this.phase == Phase.RECOVER) {
            boolean flightDone = this.thrown == null;
            if (this.phaseTicks >= GusStats.GRAB_RECOVER_TICKS && flightDone) {
                this.phase = Phase.DONE;
            }
        }
    }

    private void toRecover() {
        this.phase = Phase.RECOVER;
        this.phaseTicks = 0;
        this.gus.setAction(GusEntity.ACTION_RECOVER);
    }

    private void grab(LivingEntity victim) {
        this.held = victim;
        victim.startRiding(this.gus, true);
        this.gus.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5F, 0.7F);
        this.phase = Phase.HOLD;
        this.phaseTicks = 0;
        this.gus.setAction(GusEntity.ACTION_GRAB_HOLD);
    }

    /** The throw: launch the carried player in the direction Gus is facing. */
    private void release() {
        LivingEntity victim = this.held;
        this.held = null;
        if (victim == null) {
            return;
        }
        victim.stopRiding();
        Vec3 look = this.gus.getLookAngle();
        double hx = look.x;
        double hz = look.z;
        double len = Math.sqrt(hx * hx + hz * hz);
        if (len < 1.0E-4D) {
            hx = 1.0D;
            hz = 0.0D;
            len = 1.0D;
        }
        victim.setDeltaMovement(hx / len * GusStats.GRAB_THROW_SPEED, GusStats.GRAB_THROW_UP,
                hz / len * GusStats.GRAB_THROW_SPEED);
        victim.hurtMarked = true;
        victim.fallDistance = 0.0F;
        this.thrown = victim;
        this.flightTicks = 0;
        this.gus.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.6F);
    }

    /** Deal the throw damage the moment the thrown player touches the ground. */
    private void monitorFlight() {
        if (this.thrown == null) {
            return;
        }
        this.flightTicks++;
        if (!this.thrown.isAlive()) {
            this.thrown = null;
            return;
        }
        if (this.flightTicks > 3 && this.thrown.onGround()) {
            this.impact(this.thrown);
            this.thrown = null;
        } else if (this.flightTicks > 80) {
            this.thrown = null;   // never track forever
        }
    }

    private void impact(LivingEntity victim) {
        victim.hurt(this.gus.damageSources().mobAttack(this.gus),
                GusStats.GRAB_THROW_DAMAGE * this.gus.getDamageMultiplier());
        if (this.gus.level() instanceof ServerLevel level) {
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 1.1F);
            level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.2D, victim.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 0.1D, victim.getZ(),
                    20, 0.6D, 0.1D, 0.6D, 0.05D);
        }
    }
}

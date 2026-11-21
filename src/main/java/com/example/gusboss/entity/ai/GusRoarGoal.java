package com.example.gusboss.entity.ai;

import com.example.gusboss.GusStats;
import com.example.gusboss.entity.GusEntity;
import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Attack 5 (phase 2): rear back, roar, and curse everyone nearby with Nausea and Wither. */
public class GusRoarGoal extends Goal {
    private enum Phase { WINDUP, ROAR, RECOVER, DONE }

    private final GusEntity gus;
    private Phase phase = Phase.DONE;
    private int phaseTicks;
    private int nextUseTick;

    public GusRoarGoal(GusEntity gus) {
        this.gus = gus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.gus.getTarget();
        if (!this.gus.isPhase2() || target == null || !target.isAlive()
                || !this.gus.onGround() || this.gus.tickCount < this.nextUseTick) {
            return false;
        }
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        if (this.gus.distanceToSqr(target) > GusStats.ROAR_RADIUS * GusStats.ROAR_RADIUS) {
            return false;
        }
        return this.gus.getRandom().nextInt(6) == 0;
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
        this.gus.setAction(GusEntity.ACTION_ROAR_WINDUP);
    }

    @Override
    public void stop() {
        this.phase = Phase.DONE;
        this.gus.setAction(GusEntity.ACTION_NONE);
        this.nextUseTick = this.gus.tickCount + GusStats.ROAR_COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        this.phaseTicks++;
        this.gus.getNavigation().stop();
        LivingEntity target = this.gus.getTarget();
        if (target != null) {
            this.gus.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (this.phase == Phase.WINDUP) {
            if (this.phaseTicks >= GusStats.ROAR_WINDUP_TICKS) {
                this.phase = Phase.ROAR;
                this.phaseTicks = 0;
                this.gus.setAction(GusEntity.ACTION_ROAR);
                this.gus.playSound(SoundEvents.RAVAGER_ROAR, 4.0F, 0.5F);
            }
        } else if (this.phase == Phase.ROAR) {
            if (this.phaseTicks == 6) {
                this.gus.roarShout();
            }
            if (this.phaseTicks % 20 == 0) {
                this.gus.playSound(SoundEvents.RAVAGER_ROAR, 4.0F, 0.6F);
            }
            if (this.phaseTicks >= GusStats.ROAR_TICKS) {
                this.phase = Phase.RECOVER;
                this.phaseTicks = 0;
                this.gus.setAction(GusEntity.ACTION_RECOVER);
            }
        } else if (this.phase == Phase.RECOVER) {
            if (this.phaseTicks >= 12) {
                this.phase = Phase.DONE;
            }
        }
    }
}

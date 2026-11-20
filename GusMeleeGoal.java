package com.example.gusboss.entity.ai;

import com.example.gusboss.entity.GusEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** Chase the target at full speed and run the 3-hit punch combo (attack 1). */
public class GusMeleeGoal extends Goal {
    private final GusEntity gus;
    private final double speedModifier;
    private int repathDelay;
    private int attackCooldown;
    /** Ticks until the current punch connects; 0 = no punch in flight. */
    private int hitDelay;

    public GusMeleeGoal(GusEntity gus, double speedModifier) {
        this.gus = gus;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.gus.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.gus.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return !(target instanceof Player player) || (!player.isSpectator() && !player.isCreative());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.gus.setAggressive(true);
        this.repathDelay = 0;
        this.attackCooldown = 0;
        this.hitDelay = 0;
    }

    @Override
    public void stop() {
        this.gus.setAggressive(false);
        this.gus.getNavigation().stop();
        this.hitDelay = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.gus.getTarget();
        if (target == null) {
            return;
        }

        this.gus.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (--this.repathDelay <= 0) {
            this.repathDelay = 4 + this.gus.getRandom().nextInt(7);
            this.gus.getNavigation().moveTo(target, this.speedModifier);
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        double distSqr = this.gus.distanceToSqr(target.getX(), target.getY(), target.getZ());
        double reachSqr = this.gus.getBbWidth() * 2.0F * this.gus.getBbWidth() * 2.0F + target.getBbWidth();

        // a punch is in flight: resolve it when the fist is fully extended
        if (this.hitDelay > 0) {
            this.hitDelay--;
            if (this.hitDelay == 0 && distSqr <= reachSqr * 1.44D) {
                this.gus.onPunchImpact();
                this.gus.doHurtTarget(target);
            }
            return;
        }

        if (this.attackCooldown <= 0 && distSqr <= reachSqr && this.gus.getSensing().hasLineOfSight(target)) {
            this.attackCooldown = this.gus.getPunchIntervalTicks();
            this.hitDelay = this.gus.getPunchHitDelayTicks();
            this.gus.startPunch();
        }
    }
}

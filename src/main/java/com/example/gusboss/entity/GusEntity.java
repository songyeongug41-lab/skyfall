package com.example.gusboss.entity;

import com.example.gusboss.GusStats;
import com.example.gusboss.entity.ai.GusLeapSlamGoal;
import com.example.gusboss.entity.ai.GusMeleeGoal;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Gus, The Iron Golem - body, stats, punch combo (attack 1), leap + ground slam (attack 2), combat regen, boss bar. */
public class GusEntity extends Monster {
    public static final int LEAP_NONE = 0;
    public static final int LEAP_WINDUP = 1;
    public static final int LEAP_AIR = 2;
    public static final int LEAP_SLAM = 3;

    /** Incremented on every punch. Drives the combo variant and restarts the client animation. */
    private static final EntityDataAccessor<Integer> DATA_PUNCH_COUNT =
            SynchedEntityData.defineId(GusEntity.class, EntityDataSerializers.INT);
    /** Current leap phase (LEAP_*). Drives the leap animation on the client. */
    private static final EntityDataAccessor<Integer> DATA_LEAP_STATE =
            SynchedEntityData.defineId(GusEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    /** Client only: tickCount at which the current punch / leap phase started. */
    private int punchAnimStartTick = -1000;
    private int leapStateStartTick = -1000;

    public GusEntity(EntityType<? extends GusEntity> type, Level level) {
        super(type, level);
        this.xpReward = GusStats.XP_REWARD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, GusStats.MAX_HEALTH)
                .add(Attributes.ARMOR, GusStats.ARMOR)
                .add(Attributes.MOVEMENT_SPEED, GusStats.MOVE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, GusStats.ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, GusStats.ATTACK_KNOCKBACK)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, GusStats.FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GusLeapSlamGoal(this));   // lower number = higher priority than the punches
        this.goalSelector.addGoal(2, new GusMeleeGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PUNCH_COUNT, 0);
        this.entityData.define(DATA_LEAP_STATE, LEAP_NONE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (this.level().isClientSide) {
            if (DATA_PUNCH_COUNT.equals(key) && this.getPunchCount() > 0) {
                this.punchAnimStartTick = this.tickCount;
            } else if (DATA_LEAP_STATE.equals(key)) {
                this.leapStateStartTick = this.tickCount;
            }
        }
    }

    // ---- attack 1: punch combo ----------------------------------------------------------------

    public int getPunchCount() {
        return this.entityData.get(DATA_PUNCH_COUNT);
    }

    /** 0 = right straight, 1 = left straight, 2 = two-fisted slam (repeats). */
    public int getPunchVariant() {
        return Math.floorMod(this.getPunchCount() - 1, 3);
    }

    /** Server: begin a punch. The damage itself is applied a few ticks later by the goal. */
    public void startPunch() {
        this.entityData.set(DATA_PUNCH_COUNT, this.getPunchCount() + 1);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
    }

    /** Server: the fist connects. */
    public void onPunchImpact() {
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
    }

    /** Client: progress 0..1 of the current punch animation, or -1 when not punching. */
    public float getPunchProgress(float ageInTicks) {
        float t = (ageInTicks - (float) this.punchAnimStartTick) / (float) GusStats.PUNCH_ANIM_TICKS;
        return (t >= 0.0F && t < 1.0F) ? t : -1.0F;
    }

    // ---- attack 2: leap + ground slam ---------------------------------------------------------

    public int getLeapState() {
        return this.entityData.get(DATA_LEAP_STATE);
    }

    public void setLeapState(int state) {
        this.entityData.set(DATA_LEAP_STATE, state);
    }

    /** Client: ticks since the current leap phase began. */
    public float getLeapStateTicks(float ageInTicks) {
        return ageInTicks - (float) this.leapStateStartTick;
    }

    /** Server: shockwave at the landing point. Jumping over it avoids the damage. */
    public void groundSlam() {
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        Vec3 c = this.position();
        double radius = GusStats.SLAM_RADIUS;

        AABB box = new AABB(c.x - radius, c.y - 1.5D, c.z - radius, c.x + radius, c.y + 3.0D, c.z + radius);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !(e instanceof Monster)
                        && !(e instanceof Player p && (p.isSpectator() || p.isCreative())));

        for (LivingEntity victim : victims) {
            double dx = victim.getX() - c.x;
            double dz = victim.getZ() - c.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius || victim.getY() > c.y + 1.2D) {
                continue;   // outside the shockwave, or jumped over it
            }
            float falloff = (float) (1.0D - 0.6D * (dist / radius));
            if (victim.hurt(this.damageSources().mobAttack(this), GusStats.SLAM_DAMAGE * falloff)) {
                double kb = 1.0D - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                if (kb > 0.0D) {
                    double len = Math.max(dist, 0.001D);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(
                            dx / len * 0.9D * falloff * kb, 0.55D * kb, dz / len * 0.9D * falloff * kb));
                    victim.hurtMarked = true;
                }
            }
        }

        // impact effects: sound, debris burst, expanding dust ring
        serverLevel.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.6F);
        serverLevel.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2.0F, 0.5F);
        BlockState below = serverLevel.getBlockState(this.blockPosition().below());
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below),
                c.x, c.y + 0.1D, c.z, 60, 1.5D, 0.2D, 1.5D, 0.15D);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.2D, c.z, 3, 1.0D, 0.1D, 1.0D, 0.0D);
        for (int ring = 1; ring <= 4; ring++) {
            double ringRadius = radius * ring / 4.0D;
            int count = 8 + ring * 6;
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0D * i) / count;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        c.x + Math.cos(angle) * ringRadius, c.y + 0.1D, c.z + Math.sin(angle) * ringRadius,
                        1, 0.0D, 0.05D, 0.0D, 0.02D);
            }
        }
    }

    /** Gus lands from big leaps without hurting himself. */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ---- passive: combat regeneration + boss bar ----------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.getTarget() != null && this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(GusStats.COMBAT_REGEN_PER_SECOND);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ---- sounds (vanilla iron golem sounds for now) -------------------------------------------

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }
}

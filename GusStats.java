package com.example.gusboss;

/** All balance numbers live here. Placeholder values are marked TODO. */
public final class GusStats {
    private GusStats() {}

    public static final double MAX_HEALTH = 500.0D;
    public static final double ARMOR = 15.0D;
    public static final double MOVE_SPEED = 0.40D;            // "very fast" - placeholder, tune in game
    public static final double ATTACK_DAMAGE = 12.0D;         // TODO decide after balance review
    public static final double ATTACK_KNOCKBACK = 1.0D;
    public static final double FOLLOW_RANGE = 64.0D;

    public static final int PUNCH_INTERVAL_TICKS = 10;        // 20 ticks = 1 second
    public static final int PUNCH_ANIM_TICKS = 10;            // length of one punch animation
    public static final int PUNCH_HIT_DELAY_TICKS = 4;        // damage lands when the fist is fully extended

    // ---- attack 2: leap + ground slam ----
    public static final double LEAP_MIN_DIST = 6.0D;          // only leaps when the target is at least this far
    public static final double LEAP_MAX_DIST = 18.0D;         // ... and at most this far
    public static final int LEAP_COOLDOWN_TICKS = 160;        // 8 seconds
    public static final int LEAP_WINDUP_TICKS = 12;           // crouch before launching
    public static final int LEAP_RECOVER_TICKS = 24;          // stands still after landing (punish window)
    public static final double LEAP_UP_SPEED = 0.80D;         // ~4 block apex, ~21 ticks in the air
    public static final double LEAP_FLIGHT_TICKS = 21.0D;     // matches LEAP_UP_SPEED; used to pick horizontal speed
    public static final float SLAM_DAMAGE = 18.0F;            // TODO decide after balance review (100% at center, 40% at the edge)
    public static final double SLAM_RADIUS = 6.0D;

    // ---- phase 2 ----
    public static final float PHASE2_TRIGGER_RATIO = 0.30F;   // phase 2 starts (once) when health drops to this fraction of max or below
    public static final int PIGLIN_COUNT = 10;
    public static final double PHASE2_DAMAGE_BONUS = 0.10D;   // +10% damage
    public static final double PHASE2_SPEED_BONUS = 0.50D;    // +50% movement speed
    public static final double PHASE2_TEMPO_MULT = 0.70D;     // attack intervals x0.7 (= faster)

    // ---- attack 3: grab and throw (phase 2) ----
    public static final double GRAB_RANGE = 3.4D;
    public static final int GRAB_COOLDOWN_TICKS = 200;
    public static final int GRAB_REACH_TICKS = 8;
    public static final int GRAB_HOLD_TICKS = 24;
    public static final int GRAB_THROW_TICKS = 6;             // the target is released on tick 3
    public static final int GRAB_RECOVER_TICKS = 24;
    public static final double GRAB_THROW_SPEED = 1.1D;       // horizontal launch speed
    public static final double GRAB_THROW_UP = 0.55D;
    public static final float GRAB_THROW_DAMAGE = 28.0F;      // TODO decide after balance review (dealt when the target lands)

    // ---- attack 4: area jump slam with the "!" warning (phase 2) ----
    public static final double MEGA_MIN_DIST = 5.0D;
    public static final double MEGA_MAX_DIST = 28.0D;
    public static final int MEGA_COOLDOWN_TICKS = 300;        // 15 seconds
    public static final int MEGA_WARN_TICKS = 50;             // 2.5 second warning before the jump
    public static final int MEGA_RECOVER_TICKS = 30;
    public static final double MEGA_UP_SPEED = 1.0D;          // ~6 block apex, ~25 ticks in the air
    public static final double MEGA_FLIGHT_TICKS = 25.0D;     // matches MEGA_UP_SPEED
    public static final double MEGA_RADIUS = 16.0D;           // TODO the plan said 25-50; 15-20 is dodgeable and readable
    public static final float MEGA_DAMAGE = 32.0F;            // TODO decide after balance review (100% at center, 40% at the edge)
    public static final double MEGA_KNOCKBACK = 1.4D;

    // ---- attack 5: roar (phase 2) ----
    public static final double ROAR_RADIUS = 24.0D;
    public static final int ROAR_COOLDOWN_TICKS = 600;        // 30 seconds
    public static final int ROAR_WINDUP_TICKS = 12;
    public static final int ROAR_TICKS = 40;
    public static final int ROAR_NAUSEA_TICKS = 200;          // 10 seconds
    public static final int ROAR_WITHER_TICKS = 100;          // 5 seconds
    public static final int ROAR_WITHER_AMPLIFIER = 1;        // Wither II
    public static final float COMBAT_REGEN_PER_SECOND = 2.0F; // TODO decide after balance review
    public static final int XP_REWARD = 500;
}

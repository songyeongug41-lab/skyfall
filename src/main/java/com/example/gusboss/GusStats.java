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
    public static final float COMBAT_REGEN_PER_SECOND = 2.0F; // TODO decide after balance review
    public static final int XP_REWARD = 500;
}

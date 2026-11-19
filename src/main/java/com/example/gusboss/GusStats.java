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
    public static final float COMBAT_REGEN_PER_SECOND = 2.0F; // TODO decide after balance review
    public static final int XP_REWARD = 500;
}

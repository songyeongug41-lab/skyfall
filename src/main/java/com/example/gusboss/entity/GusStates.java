package com.example.gusboss.entity;

/** Plain numeric states shared by the entity, the AI goals and the animation code (no Minecraft types). */
public final class GusStates {
    private GusStates() {}

    public static final int LEAP_NONE = 0;
    public static final int LEAP_WINDUP = 1;
    public static final int LEAP_AIR = 2;
    public static final int LEAP_SLAM = 3;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_GRAB_REACH = 1;
    public static final int ACTION_GRAB_HOLD = 2;
    public static final int ACTION_GRAB_THROW = 3;
    public static final int ACTION_RECOVER = 4;          // after a throw
    public static final int ACTION_ROAR_WINDUP = 5;
    public static final int ACTION_ROAR = 6;
    public static final int ACTION_ROAR_RECOVER = 7;
    public static final int ACTION_WHIFF_RECOVER = 8;    // after a missed grab
}

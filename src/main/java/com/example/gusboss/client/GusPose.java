package com.example.gusboss.client;

import com.example.gusboss.GusStats;
import com.example.gusboss.entity.GusStates;

/**
 * All of Gus's pose math in one Minecraft-free class, so it can be tested and tuned in one place.
 * Angles are radians. Torso X: positive = leaning forward. Arm/leg X: negative = swinging forward/up.
 * Arm Z: positive = away from the body (the left arm mirrors the sign in the model).
 * Every pose blends from and back to the stance pose, so states never "pop".
 */
final class GusPose {
    /** Global dial for every pose: 1.0 = full strength, lower = calmer. */
    static final float INTENSITY = 0.85F;

    private static final float RAD = (float) Math.PI / 180.0F;

    // ---- punch timeline: progress 0..1 (the hit lands at 0.50). Values are offsets from the stance pose. ----
    private static final float[] PT       = {0.00F,  0.25F,  0.50F,  0.62F, 1.00F};
    // arcing hammer-fist: the arm is raised high, then chopped down and across the body
    private static final float[] P_ARM    = {0.0F, -1.90F, -0.55F, -0.55F, 0.0F};
    private static final float[] P_ARM_Z  = {0.0F,  0.40F, -0.10F, -0.10F, 0.0F};   // out, then across (right-fist signs)
    private static final float[] P_OFF    = {0.0F, -0.25F, -0.10F, -0.10F, 0.0F};   // the other arm guards
    private static final float[] P_TWIST  = {0.0F,  0.20F, -0.30F, -0.30F, 0.0F};   // coil, then unwind into the blow
    private static final float[] P_LEAN   = {0.0F, -0.06F,  0.18F,  0.18F, 0.0F};
    private static final float[] P_STEP   = {0.0F,  0.05F, -0.22F, -0.22F, 0.0F};   // the leg opposite the fist steps in
    // two-fisted smash
    private static final float[] S_ARM    = {0.0F, -2.50F, -0.55F, -0.55F, 0.0F};
    private static final float[] S_LEAN   = {0.0F, -0.14F,  0.32F,  0.32F, 0.0F};
    private static final float[] S_DROP   = {0.0F,  0.00F,  1.20F,  1.20F, 0.0F};
    private static final float[] S_SPREAD = {0.0F,  0.15F,  0.00F,  0.00F, 0.0F};

    // ---- outputs (read by the model) ----
    float torsoX, torsoY, torsoZ;
    float headX, headY;
    float rightArmX, rightArmZ, leftArmX, leftArmZ;
    float rightLegX, leftLegX;
    float rootY, rootZ;   // rootY in model units (positive = down)

    void compute(boolean aggressive, float limbSwing, float limbSwingAmount, float ageInTicks,
                 float netHeadYawDeg, float headPitchDeg,
                 int leap, float leapTicks, float verticalSpeed,
                 int action, float actionTicks, float punchProgress, int punchVariant) {
        float stance = aggressive ? 1.0F : 0.0F;
        float cycle = limbSwing * 0.5F;
        float run = cos(cycle) * limbSwingAmount;
        float breath = cos(ageInTicks * 0.07F) * 0.03F;

        float baseLean = 0.06F * stance;
        float baseFlare = 0.04F + 0.10F * stance;
        float armBase = -0.18F * stance;

        // ---- base pose: idle / walk / fighting stance ----
        float lean = baseLean + 0.10F * limbSwingAmount;
        float twist = -cos(cycle) * 0.07F * limbSwingAmount;     // shoulders counter-rotate against the hips
        float rArmX = armBase + 0.50F * run;
        float lArmX = armBase - 0.50F * run;
        float rArmZ = baseFlare;
        float lArmZ = baseFlare;
        float legTuck = 0.0F;
        float rLegExtra = 0.0F;
        float lLegExtra = 0.0F;
        float drop = 0.0F;
        float shake = 0.0F;
        float headExtra = 0.0F;
        float bob = limbSwingAmount;
        float walk = 1.0F;                                       // 0 = a special pose owns the legs and arms

        if (leap != GusStates.LEAP_NONE) {
            walk = 0.0F;
            bob = 0.0F;
            twist = 0.0F;
            float t = leapTicks;
            float arms;
            float flare;
            if (leap == GusStates.LEAP_WINDUP) {
                float c = smooth(t / 10.0F);
                lean = lerp(c, baseLean, 0.38F);
                arms = lerp(c, armBase, 0.55F);                 // arms drawn back
                flare = lerp(c, baseFlare, 0.20F);
                legTuck = -0.30F * c;
                drop = 1.6F * c;
                shake = sin(ageInTicks * 2.6F) * 0.02F * c;
            } else if (leap == GusStates.LEAP_AIR) {
                float fall = clamp(-verticalSpeed / 0.8F, 0.0F, 1.0F);
                float b = smooth(t / 6.0F);                      // launch: ease out of the crouch
                arms = lerp(b, 0.55F, lerp(fall, -2.40F, -1.60F));
                lean = lerp(b, 0.38F, lerp(fall, -0.15F, 0.35F));
                legTuck = lerp(b, -0.30F, lerp(fall, -0.35F, -0.10F));
                drop = 1.6F * (1.0F - b);
                flare = 0.20F;
            } else {
                float in = clamp(t / 2.0F, 0.0F, 1.0F);
                float r = smooth((t - 8.0F) / 16.0F);            // recovery 0..1
                arms = lerp(r, lerp(in, -1.60F, -0.50F), armBase);
                lean = lerp(r, lerp(in, 0.35F, 0.45F), baseLean);
                legTuck = lerp(in, -0.10F, -0.20F) * (1.0F - r);
                float squash = t < 3.0F ? t / 3.0F : 1.0F - clamp((t - 3.0F) / 10.0F, 0.0F, 1.0F);
                drop = 1.8F * squash;
                flare = lerp(r, 0.20F, baseFlare);
            }
            rArmX = arms;
            lArmX = arms;
            rArmZ = flare;
            lArmZ = flare;
        } else if (action != GusStates.ACTION_NONE) {
            walk = 0.0F;
            bob = 0.0F;
            twist = 0.0F;
            float t = actionTicks;
            float arms = armBase;
            float flare = baseFlare;
            if (action == GusStates.ACTION_GRAB_REACH) {
                float c = smooth(t / 6.0F);
                arms = lerp(c, armBase, -1.30F);                 // both arms out to grab
                lean = lerp(c, baseLean, 0.22F);
                flare = lerp(c, baseFlare, -0.08F);              // closing in on the target
            } else if (action == GusStates.ACTION_GRAB_HOLD) {
                float c = smooth(t / 12.0F);
                arms = lerp(c, -1.30F, -2.50F);                  // lifting the victim overhead
                lean = lerp(c, 0.22F, -0.10F);
                flare = lerp(c, -0.08F, 0.05F);
                headExtra = -0.20F * c;
                shake = sin(ageInTicks * 2.8F) * 0.02F * c;
            } else if (action == GusStates.ACTION_GRAB_THROW) {
                float c = smooth(t / 5.0F);
                arms = lerp(c, -2.50F, -0.60F);                  // hurl forward and down
                lean = lerp(c, -0.10F, 0.45F);
                flare = lerp(c, 0.05F, 0.12F);
                headExtra = lerp(c, -0.20F, 0.0F);
            } else if (action == GusStates.ACTION_RECOVER) {
                float r = smooth(t / 16.0F);
                arms = lerp(r, -0.60F, armBase);
                lean = lerp(r, 0.45F, baseLean);
                flare = lerp(r, 0.12F, baseFlare);
            } else if (action == GusStates.ACTION_WHIFF_RECOVER) {
                float r = smooth(t / 12.0F);
                arms = lerp(r, -1.30F, armBase);
                lean = lerp(r, 0.22F, baseLean);
                flare = lerp(r, -0.08F, baseFlare);
            } else if (action == GusStates.ACTION_ROAR_WINDUP) {
                float c = smooth(t / (float) GusStats.ROAR_WINDUP_TICKS);
                lean = lerp(c, baseLean, 0.30F);
                arms = lerp(c, armBase, 0.50F);                  // coiling: arms back, head down
                flare = lerp(c, baseFlare, 0.15F);
                headExtra = 0.35F * c;
                drop = 1.2F * c;
                legTuck = -0.20F * c;
            } else if (action == GusStates.ACTION_ROAR) {
                float c = smooth(t / 6.0F);                      // chest out, head up, arms open
                lean = lerp(c, 0.30F, -0.18F);
                arms = lerp(c, 0.50F, -0.40F);
                flare = lerp(c, 0.15F, 0.55F);
                headExtra = lerp(c, 0.35F, -0.45F);
                drop = 1.2F * (1.0F - c);
                legTuck = -0.20F * (1.0F - c);
                shake = sin(ageInTicks * 3.3F) * 0.03F;
            } else if (action == GusStates.ACTION_ROAR_RECOVER) {
                float r = smooth(t / 12.0F);
                lean = lerp(r, -0.18F, baseLean);
                arms = lerp(r, -0.40F, armBase);
                flare = lerp(r, 0.55F, baseFlare);
                headExtra = lerp(r, -0.45F, 0.0F);
                shake = sin(ageInTicks * 3.3F) * 0.03F * (1.0F - r);
            }
            rArmX = arms;
            lArmX = arms;
            rArmZ = flare;
            lArmZ = flare;
        } else if (punchProgress >= 0.0F) {
            float p = punchProgress;
            if (punchVariant == 2) {
                float a = sample(p, PT, S_ARM);
                rArmX += a;
                lArmX += a;
                float spread = sample(p, PT, S_SPREAD);
                rArmZ += spread;
                lArmZ += spread;
                lean += sample(p, PT, S_LEAN);
                drop += sample(p, PT, S_DROP);
            } else {
                float a = sample(p, PT, P_ARM);
                float az = sample(p, PT, P_ARM_Z);
                float off = sample(p, PT, P_OFF);
                float tw = sample(p, PT, P_TWIST);
                float st = sample(p, PT, P_STEP);
                if (punchVariant == 0) {                          // right fist
                    rArmX += a;
                    rArmZ += az;
                    lArmX += off;
                    twist += tw;
                    lLegExtra += st;
                    rLegExtra -= st * 0.5F;
                } else {                                          // left fist
                    lArmX += a;
                    lArmZ += az;
                    rArmX += off;
                    twist -= tw;
                    rLegExtra += st;
                    lLegExtra -= st * 0.5F;
                }
                lean += sample(p, PT, P_LEAN);
            }
        }

        // ---- apply, scaled by the global intensity ----
        float k = INTENSITY;
        torsoX = lean * k + breath * 0.5F;
        torsoY = twist * k;
        torsoZ = shake * k;

        // the head keeps looking at the target while the torso leans and twists
        headX = headPitchDeg * RAD - 0.7F * lean * k + headExtra * k;
        headY = netHeadYawDeg * RAD - twist * k;

        rightArmX = rArmX * k;
        leftArmX = lArmX * k;
        rightArmZ = rArmZ * k + breath;
        leftArmZ = -(lArmZ * k + breath);

        rightLegX = (-0.85F * run * walk + legTuck + rLegExtra) * k;
        leftLegX = (0.85F * run * walk + legTuck + lLegExtra) * k;

        rootY = drop * k - Math.abs(sin(cycle)) * 0.7F * bob;     // crouch sink / running bounce
        rootZ = sin(cycle) * 0.03F * bob;                         // gentle sway while moving
    }

    // ---- small math helpers (no Minecraft classes) ----

    private static float sin(float v) { return (float) Math.sin(v); }

    private static float cos(float v) { return (float) Math.cos(v); }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    private static float lerp(float t, float a, float b) { return a + (b - a) * t; }

    private static float smooth(float x) {
        x = clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** Smooth keyframe interpolation between (times, values). */
    private static float sample(float p, float[] times, float[] values) {
        if (p <= times[0]) {
            return values[0];
        }
        for (int i = 1; i < times.length; i++) {
            if (p <= times[i]) {
                float t = (p - times[i - 1]) / (times[i] - times[i - 1]);
                return lerp(t * t * (3.0F - 2.0F * t), values[i - 1], values[i]);
            }
        }
        return values[values.length - 1];
    }
}

package com.example.gusboss.client;

import com.example.gusboss.GusBossMod;
import com.example.gusboss.GusStats;
import com.example.gusboss.entity.GusEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Iron-golem shaped model with its own animation set:
 *  - run: forward lean, body bounce and sway, long stride
 *  - fighting stance while aggressive: hunched, fists forward and flared
 *  - punch combo: right straight -> left straight -> two-fisted slam (torso twists with every hit)
 *  - leap: deep crouch, arms thrown overhead in the air, crushing slam on landing
 * The torso is pivoted at the hips and owns the head and arms, so leaning/twisting moves them together.
 */
public class GusModel<T extends GusEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(GusBossMod.MOD_ID, "gus"), "main");

    private static final float RAD = (float) Math.PI / 180.0F;

    // ---- punch keyframes: progress 0..1 over PUNCH_ANIM_TICKS, the hit lands at 0.40 ----------
    // values are offsets from the current stance pose (0 at both ends)
    private static final float[] KEY_TIMES   = {0.00F, 0.22F, 0.40F, 0.55F, 1.00F};
    // straight punch (right-fist signs; the left fist mirrors the twist)
    private static final float[] STRAIGHT_ARM   = {0.0F,  1.2F, -1.1F, -1.1F, 0.0F};
    private static final float[] STRAIGHT_OFF   = {0.0F, -0.2F,  0.6F,  0.6F, 0.0F};   // the other arm
    private static final float[] STRAIGHT_TWIST = {0.0F,  0.40F, -0.55F, -0.55F, 0.0F};
    private static final float[] STRAIGHT_LEAN  = {0.0F, -0.08F, 0.22F, 0.22F, 0.0F};
    // two-fisted slam
    private static final float[] SLAM_ARM  = {0.0F, -2.4F, -0.4F, -0.4F, 0.0F};
    private static final float[] SLAM_LEAN = {0.0F, -0.22F, 0.45F, 0.45F, 0.0F};
    // phase 2 transition: crouch and shake -> rear up and roar -> slam -> back to stance
    private static final float[] TR_TIMES = {0.00F, 0.25F, 0.45F, 0.65F, 0.75F, 1.00F};
    private static final float[] TR_LEAN  = {0.12F, 0.75F, -0.35F, -0.35F, 0.50F, 0.12F};
    private static final float[] TR_ARM   = {-0.45F, 0.20F, -2.90F, -2.90F, -0.50F, -0.45F};
    private static final float[] TR_HEAD  = {0.00F, 0.50F, -0.60F, -0.60F, 0.20F, 0.00F};
    private static final float[] TR_DROP  = {0.00F, 2.50F, 0.00F, 0.00F, 2.00F, 0.00F};

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GusModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.rightArm = this.torso.getChild("right_arm");
        this.leftArm = this.torso.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    /** Same geometry and texture layout as the vanilla golem, re-parented under a hip-pivoted torso. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        PartDefinition torso = part.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 40).addBox(-9.0F, -17.0F, -6.0F, 18.0F, 12.0F, 11.0F)
                        .texOffs(0, 70).addBox(-4.5F, -5.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        torso.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F)
                        .texOffs(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -15.0F, -2.0F));
        torso.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(0.0F, -15.0F, 0.0F));
        torso.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(60, 58).addBox(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(0.0F, -15.0F, 0.0F));

        part.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(-4.0F, 11.0F, 0.0F));
        part.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /** Smooth keyframe interpolation between (times, values). */
    private static float sample(float p, float[] times, float[] values) {
        if (p <= times[0]) {
            return values[0];
        }
        for (int i = 1; i < times.length; i++) {
            if (p <= times[i]) {
                float t = (p - times[i - 1]) / (times[i] - times[i - 1]);
                t = t * t * (3.0F - 2.0F * t);
                return Mth.lerp(t, values[i - 1], values[i]);
            }
        }
        return values[values.length - 1];
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float stance = entity.isAggressive() ? 1.0F : 0.0F;
        float run = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;     // -1..1 leg cycle
        float breath = Mth.cos(ageInTicks * 0.09F) * 0.04F;
        float bobAmount = limbSwingAmount;

        // ---- base pose: idle / run / fighting stance ----
        float lean = 0.12F * stance + 0.18F * limbSwingAmount;          // forward hunch
        float twist = 0.0F;
        float armBase = -0.45F * stance;                                // fists forward when fighting
        float rightArmX = armBase + 1.3F * run;
        float leftArmX = armBase - 1.3F * run;
        float flare = 0.05F + 0.17F * stance;                           // arms held away from the body
        float legTuck = 0.0F;                                           // added to both legs
        float crouchDrop = 0.0F;                                        // whole body sinks (model units)
        float shake = 0.0F;
        float headExtra = 0.0F;                                         // extra head pitch

        int leap = entity.getLeapState();
        if (entity.isTransitioning()) {
            // ---- phase 2 transition ----
            float tp = Mth.clamp(entity.getTransitionTicks(ageInTicks) / (float) GusStats.TRANSITION_TICKS, 0.0F, 1.0F);
            run = 0.0F;
            bobAmount = 0.0F;
            lean = sample(tp, TR_TIMES, TR_LEAN);
            rightArmX = sample(tp, TR_TIMES, TR_ARM);
            leftArmX = rightArmX;
            headExtra = sample(tp, TR_TIMES, TR_HEAD);
            crouchDrop = sample(tp, TR_TIMES, TR_DROP);
            flare = 0.3F;
            legTuck = -0.15F * crouchDrop;
            shake = Mth.sin(ageInTicks * 3.1F) * 0.05F * (1.0F - smooth((tp - 0.45F) / 0.3F));
        } else if (leap != GusEntity.LEAP_NONE) {
            // ---- attack 2: leap + slam (no walk cycle while this plays) ----
            float t = entity.getLeapStateTicks(ageInTicks);
            run = 0.0F;
            bobAmount = 0.0F;
            if (leap == GusEntity.LEAP_WINDUP) {
                float c = smooth(t / (float) GusStats.LEAP_WINDUP_TICKS);
                lean = 0.12F + 0.5F * c;
                rightArmX = Mth.lerp(c, armBase, 1.1F);                 // arms pulled back
                leftArmX = rightArmX;
                flare = 0.22F + 0.15F * c;
                legTuck = -0.45F * c;
                crouchDrop = 2.5F * c;
                shake = Mth.sin(ageInTicks * 2.6F) * 0.03F * c;         // trembling before the launch
            } else if (leap == GusEntity.LEAP_AIR) {
                float vy = (float) (entity.getY() - entity.yo);         // per-tick vertical speed
                float fall = Mth.clamp(-vy / 0.8F, 0.0F, 1.0F);
                rightArmX = Mth.lerp(fall, -2.9F, -1.4F);               // arms overhead, then coming down
                leftArmX = rightArmX;
                lean = Mth.lerp(fall, -0.25F, 0.5F);
                legTuck = Mth.lerp(fall, -0.7F, -0.2F);
                flare = 0.3F;
            } else {
                float slamArm = t < 2.0F ? Mth.lerp(t / 2.0F, -1.4F, -0.5F) : -0.5F;
                float r = smooth((t - 8.0F) / 16.0F);                   // recovery 0..1
                rightArmX = Mth.lerp(r, slamArm, armBase);
                leftArmX = rightArmX;
                lean = Mth.lerp(r, 0.65F, 0.12F * stance);
                float squash = t < 3.0F ? t / 3.0F : 1.0F - Mth.clamp((t - 3.0F) / 10.0F, 0.0F, 1.0F);
                crouchDrop = 2.5F * squash;
                flare = 0.25F;
                legTuck = -0.3F * (1.0F - r);
            }
        } else {
            // ---- attack 1: punch combo overlay ----
            float p = entity.getPunchProgress(ageInTicks);
            if (p >= 0.0F) {
                int variant = entity.getPunchVariant();
                if (variant == 2) {
                    float arm = sample(p, KEY_TIMES, SLAM_ARM);
                    rightArmX += arm;
                    leftArmX += arm;
                    lean += sample(p, KEY_TIMES, SLAM_LEAN);
                } else {
                    float arm = sample(p, KEY_TIMES, STRAIGHT_ARM);
                    float off = sample(p, KEY_TIMES, STRAIGHT_OFF);
                    if (variant == 0) {
                        rightArmX += arm;
                        leftArmX += off;
                        twist = sample(p, KEY_TIMES, STRAIGHT_TWIST);
                    } else {
                        leftArmX += arm;
                        rightArmX += off;
                        twist = -sample(p, KEY_TIMES, STRAIGHT_TWIST);
                    }
                    lean += sample(p, KEY_TIMES, STRAIGHT_LEAN);
                }
            }
        }

        // ---- apply ----
        this.torso.xRot = lean + breath * 0.5F;
        this.torso.yRot = twist;
        this.torso.zRot = shake;

        // the head keeps looking at the target while the torso leans and twists
        this.head.xRot = headPitch * RAD - 0.7F * lean + headExtra;
        this.head.yRot = netHeadYaw * RAD - twist;

        this.rightArm.xRot = rightArmX;
        this.leftArm.xRot = leftArmX;
        this.rightArm.zRot = flare + breath;
        this.leftArm.zRot = -(flare + breath);

        this.rightLeg.xRot = -1.3F * run + legTuck;
        this.leftLeg.xRot = 1.3F * run + legTuck;

        // body bounce and sway while running; crouch sink during the leap
        this.root.y = crouchDrop - Math.abs(Mth.sin(limbSwing * 0.6662F)) * 1.6F * bobAmount;
        this.root.zRot = Mth.sin(limbSwing * 0.6662F) * 0.05F * bobAmount;
    }
}

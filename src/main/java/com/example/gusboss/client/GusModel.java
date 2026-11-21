package com.example.gusboss.client;

import com.example.gusboss.GusBossMod;
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

/**
 * Iron-golem shaped model. The torso is pivoted at the hips and owns the head and arms, so leaning and
 * twisting move them together. All animation math lives in {@link GusPose}.
 */
public class GusModel<T extends GusEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(GusBossMod.MOD_ID, "gus"), "main");

    private final GusPose pose = new GusPose();

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

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.pose.compute(entity.isAggressive(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                entity.getLeapState(), entity.getLeapStateTicks(ageInTicks), (float) (entity.getY() - entity.yo),
                entity.getAction(), entity.getActionTicks(ageInTicks),
                entity.getPunchProgress(ageInTicks), entity.getPunchVariant());

        this.torso.xRot = this.pose.torsoX;
        this.torso.yRot = this.pose.torsoY;
        this.torso.zRot = this.pose.torsoZ;
        this.head.xRot = this.pose.headX;
        this.head.yRot = this.pose.headY;
        this.rightArm.xRot = this.pose.rightArmX;
        this.rightArm.zRot = this.pose.rightArmZ;
        this.leftArm.xRot = this.pose.leftArmX;
        this.leftArm.zRot = this.pose.leftArmZ;
        this.rightLeg.xRot = this.pose.rightLegX;
        this.leftLeg.xRot = this.pose.leftLegX;
        this.root.y = this.pose.rootY;
        this.root.zRot = this.pose.rootZ;
    }
}

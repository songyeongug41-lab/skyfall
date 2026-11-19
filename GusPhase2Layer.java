package com.example.gusboss.client;

import com.example.gusboss.entity.GusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Phase 2 look without needing a new skin: the whole model glows a pulsing, enraged red.
 * (Remove this layer once a dedicated phase 2 skin is in place, or keep it as an extra glow.)
 */
public class GusPhase2Layer extends RenderLayer<GusEntity, GusModel<GusEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/iron_golem/iron_golem.png");

    public GusPhase2Layer(RenderLayerParent<GusEntity, GusModel<GusEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, GusEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!entity.isPhase2()) {
            return;
        }
        float pulse = 0.40F + 0.15F * Mth.sin(ageInTicks * 0.25F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(TEXTURE));
        this.getParentModel().renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1.0F, 0.12F, 0.08F, pulse);
    }
}

package com.example.gusboss.client;

import com.example.gusboss.entity.GusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GusRenderer extends MobRenderer<GusEntity, GusModel<GusEntity>> {
    // Placeholder: vanilla iron golem texture. Swap for gusboss:textures/entity/gus.png when the skin is ready.
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/iron_golem/iron_golem.png");

    // When a finished phase 2 skin exists at assets/gusboss/textures/entity/gus_phase2.png, set this to true.
    private static final boolean HAS_PHASE2_SKIN = false;
    private static final ResourceLocation TEXTURE_PHASE2 =
            new ResourceLocation("gusboss", "textures/entity/gus_phase2.png");

    public GusRenderer(EntityRendererProvider.Context context) {
        super(context, new GusModel<>(context.bakeLayer(GusModel.LAYER_LOCATION)), 0.7F);
        this.addLayer(new GusPhase2Layer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(GusEntity entity) {
        return (HAS_PHASE2_SKIN && entity.isPhase2()) ? TEXTURE_PHASE2 : TEXTURE;
    }
}

package com.example.gusboss.client;

import com.example.gusboss.entity.GusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GusRenderer extends MobRenderer<GusEntity, GusModel<GusEntity>> {
    // Placeholder: vanilla iron golem texture. Swap for gusboss:textures/entity/gus.png when the skin is ready.
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/iron_golem/iron_golem.png");

    public GusRenderer(EntityRendererProvider.Context context) {
        super(context, new GusModel<>(context.bakeLayer(GusModel.LAYER_LOCATION)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(GusEntity entity) {
        return TEXTURE;
    }
}

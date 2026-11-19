package com.example.gusboss.registry;

import com.example.gusboss.GusBossMod;
import com.example.gusboss.entity.GusEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GusBossMod.MOD_ID);

    // Same hitbox as a vanilla iron golem (1.4 x 2.7).
    public static final RegistryObject<EntityType<GusEntity>> GUS = ENTITY_TYPES.register("gus",
            () -> EntityType.Builder.of(GusEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 2.7F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(GusBossMod.MOD_ID, "gus").toString()));
}

package com.example.gusboss.registry;

import com.example.gusboss.GusBossMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GusBossMod.MOD_ID);

    public static final RegistryObject<Item> GUS_SPAWN_EGG = ITEMS.register("gus_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GUS, 0xC9C9C9, 0xB02020, new Item.Properties()));
}

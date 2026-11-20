package com.example.gusboss;

import com.example.gusboss.entity.GusEntity;
import com.example.gusboss.registry.ModEntities;
import com.example.gusboss.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(GusBossMod.MOD_ID)
public class GusBossMod {
    public static final String MOD_ID = "gusboss";
    public static final String VERSION = "0.3.0";
    private static final Logger LOGGER = LogUtils.getLogger();

    public GusBossMod() {
        LOGGER.info("[Gus] gusboss {} loaded", VERSION);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::addCreativeTabItems);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GUS.get(), GusEntity.createAttributes().build());
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.GUS_SPAWN_EGG.get());
        }
    }
}

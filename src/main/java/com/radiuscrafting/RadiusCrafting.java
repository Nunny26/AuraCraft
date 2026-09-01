package com.radiuscrafting;

import com.radiuscrafting.config.ModConfig;
import com.radiuscrafting.network.ModMessages;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

public class RadiusCrafting implements ModInitializer {
    
    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        ModMessages.registerPayloads();
    }
}

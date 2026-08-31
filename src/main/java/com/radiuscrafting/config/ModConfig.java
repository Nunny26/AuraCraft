package com.radiuscrafting.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "radiuscrafting")
public class ModConfig implements ConfigData {

    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    @ConfigEntry.Gui.Tooltip
    public int searchRadius = 8;

    @ConfigEntry.Gui.Tooltip
    public boolean useEnderChest = true;

    @ConfigEntry.Gui.Tooltip
    public boolean useShulkerBoxes = true;
    
    @ConfigEntry.Gui.Tooltip
    public boolean ignoreUnopenedLootChests = true;
    
    public enum PullPriority {
        INVENTORY_FIRST,
        STORAGE_FIRST
    }
    
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public PullPriority pullPriority = PullPriority.INVENTORY_FIRST;
}

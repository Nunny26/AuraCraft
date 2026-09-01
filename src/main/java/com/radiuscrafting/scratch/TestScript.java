package com.radiuscrafting.scratch;
import net.minecraft.world.item.crafting.Recipe;
public class TestScript {
    public static void check(Recipe<?> r) {
        System.out.println(r.placementInfo().getClass().getName());
    }
}

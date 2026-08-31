package com.radiuscrafting;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TestReflection2 {
    public static void main(String[] args) {
        for (Field f : InventoryScreen.class.getDeclaredFields()) {
            System.out.println(f.getType().getSimpleName() + " " + f.getName());
        }
        for (Method m : InventoryScreen.class.getDeclaredMethods()) {
            System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + "()");
        }
    }
}

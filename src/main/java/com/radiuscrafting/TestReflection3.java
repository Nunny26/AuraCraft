package com.radiuscrafting;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import java.lang.reflect.Method;

public class TestReflection3 {
    public static void main(String[] args) {
        for (Method m : RecipeBookComponent.class.getDeclaredMethods()) {
            System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + "()");
        }
    }
}

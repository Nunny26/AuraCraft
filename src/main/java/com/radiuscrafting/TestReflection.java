package com.radiuscrafting;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class TestReflection {
    public static void main(String[] args) {
        for (Method m : GuiGraphicsExtractor.class.getMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + "()");
            }
        }
    }
}

package com.radiuscrafting.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RadiusCraftingNames {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, String> customNames = new HashMap<>();

    public static void load() {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().gameDirectory == null) return;
        Path configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config/radiuscrafting_names.json");
        if (Files.exists(configPath)) {
            try (Reader reader = new FileReader(configPath.toFile())) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    customNames = loaded;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().gameDirectory == null) return;
        Path configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config/radiuscrafting_names.json");
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = new FileWriter(configPath.toFile())) {
                GSON.toJson(customNames, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getName(String id, String defaultName) {
        return customNames.getOrDefault(id, defaultName);
    }

    public static void setName(String id, String name) {
        if (name == null || name.isEmpty()) {
            customNames.remove(id);
        } else {
            customNames.put(id, name);
        }
        save();
    }
}

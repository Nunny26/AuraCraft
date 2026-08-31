package com.radiuscrafting.client;

import com.radiuscrafting.network.ModMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;

import java.util.List;

public class NearbyStorageWidget extends AbstractWidget {

    // ==========================================
    // 🛠️ EASY UI EDITOR: TWEAK THIS NUMBER! 🛠️
    // ==========================================
    // Increase to move right, decrease to move left (e.g., 5, 10, -2, etc.)
    private static final int WINDOW_GAP = 3;
    public static boolean isCustomUiOpen = true; // Remembers if you closed it!
    private double scrollAmount = 0;
    private EditBox renameField = null;
    private String renamingStorageId = null;

    private final net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> parentScreen;

    public NearbyStorageWidget(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> parentScreen, int width, int height) {
        super(((IHandledScreenAccessor)parentScreen).getX() + ((IHandledScreenAccessor)parentScreen).getBackgroundWidth() + WINDOW_GAP,
                ((IHandledScreenAccessor)parentScreen).getY(), width, height, Component.empty());
        this.parentScreen = parentScreen;
    }

    /**
     * Instantly locks the widget to the inventory based on your WINDOW_GAP.
     * Called automatically before rendering and clicking to ensure zero latency!
     */
    private void syncPosition() {
        int inventoryX = ((IHandledScreenAccessor) parentScreen).getX();
        int inventoryWidth = ((IHandledScreenAccessor) parentScreen).getBackgroundWidth();

        // 3 is your perfect pixel gap!
        this.setX(inventoryX + inventoryWidth + 3);
        this.setY(((IHandledScreenAccessor) parentScreen).getY());
    }

    public boolean isRenaming() {
        return renameField != null && renameField.isFocused();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!isCustomUiOpen) return; // Skips rendering and clicking if closed
        this.syncPosition(); // Lock location

        List<ModMessages.StorageUIRecord> records = RadiusCraftingClient.cachedStorageRecords;
        if (records == null || records.isEmpty()) return;

        int rightEdgeX = this.getX();
        int screenY = this.getY();
        int panelWidth = 194;
        int panelHeight = 166;

        net.minecraft.resources.Identifier panelTexture = net.minecraft.resources.Identifier.fromNamespaceAndPath("radiuscrafting", "textures/gui/panel.png");

        // 1. Draw the Background
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, panelTexture, rightEdgeX, screenY, 0.0f, 0.0f, 10, 166, 10, 166, 256, 256);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, panelTexture, rightEdgeX + 10, screenY, 10.0f, 0.0f, 174, 166, 127, 166, 256, 256);
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, panelTexture, rightEdgeX + 184, screenY, 137.0f, 0.0f, 10, 166, 10, 166, 256, 256);

        // 2. Enable Scissor
        graphics.enableScissor(rightEdgeX + 4, screenY + 8, rightEdgeX + 190, screenY + 158);

        // 3. Draw the Scrollable Contents Loop - Pass 1: Items & Slots
        int currentBaseY = 0;

        for (ModMessages.StorageUIRecord record : records) {
            int startX = rightEdgeX + 16;
            int startY = currentBaseY + 24;
            int rows = (record.items().size() + 8) / 9;

            net.minecraft.resources.Identifier slotSprite = net.minecraft.resources.Identifier.withDefaultNamespace("container/slot");

            for (int i = 0; i < record.items().size(); i++) {
                ItemStack stack = record.items().get(i);

                int column = i % 9;
                int row = i / 9;
                int visualItemX = startX + (column * 18);
                int visualItemY = startY + (row * 18) - (int) scrollAmount + screenY;

                // Draw slot squares
                graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, slotSprite, visualItemX - 1, visualItemY - 1, 18, 18);

                // Draw items
                if (!stack.isEmpty()) {
                    graphics.item(stack, visualItemX, visualItemY);
                    graphics.itemDecorations(Minecraft.getInstance().font, stack, visualItemX, visualItemY);
                }

                if (mouseX >= visualItemX && mouseX < visualItemX + 16 && mouseY >= visualItemY && mouseY < visualItemY + 16) {
                    graphics.fill(visualItemX, visualItemY, visualItemX + 16, visualItemY + 16, 0x44FFFFFF);
                }
            }

            currentBaseY += (rows * 18) + 26;
        }

        // Pass 2: Draw Text on top of everything
        currentBaseY = 0;
        for (ModMessages.StorageUIRecord record : records) {
            int startX = rightEdgeX + 16;
            int startY = currentBaseY + 24;
            int rows = (record.items().size() + 8) / 9;

            int textVisualY = startY - 12 - (int) scrollAmount + screenY;

            if (renameField != null && record.id().equals(renamingStorageId)) {
                renameField.setX(startX);
                renameField.setY(textVisualY - 2);
                renameField.extractRenderState(graphics, mouseX, mouseY, delta);
            } else {
                String displayText = RadiusCraftingNames.getName(record.id(), record.displayName().getString());
                graphics.text(Minecraft.getInstance().font, displayText, startX, textVisualY, 0xFFFFFFFF, true);
            }

            currentBaseY += (rows * 18) + 26;
        }

        // 4. Disable Scissor
        graphics.disableScissor();

        // 5. Draw the Scrollbar
        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int scrollbarX = rightEdgeX + 176;
            int scrollbarY = screenY + 8;
            int scrollbarHeight = 150;

            net.minecraft.resources.Identifier scrollBg = net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_items.png");
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, scrollBg, scrollbarX, scrollbarY, 174, 17, 12, scrollbarHeight, 14, 112, 256, 256);

            int thumbY = scrollbarY + (int) (((float) scrollAmount / maxScroll) * (scrollbarHeight - 15));
            net.minecraft.resources.Identifier thumbSprite = net.minecraft.resources.Identifier.withDefaultNamespace("container/creative_inventory/scroller");
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, thumbSprite, scrollbarX, thumbY, 12, 15);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 1. If the UI is closed, ignore clicks entirely!
        if (!isCustomUiOpen) return false;

        this.syncPosition();

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.buttonInfo().button();

        int rightEdgeX = this.getX();
        int screenY = this.getY();
        int panelWidth = 194;
        int panelHeight = 166;

        // 2. If they clicked outside our custom window, RETURN FALSE so the Recipe Book can have the click!
        if (mouseX < rightEdgeX || mouseX > rightEdgeX + panelWidth || mouseY < screenY || mouseY > screenY + panelHeight) {
            return false;
        }

        if (renameField != null && renameField.mouseClicked(event, doubleClick)) {
            return true;
        }

        List<ModMessages.StorageUIRecord> records = RadiusCraftingClient.cachedStorageRecords;
        if (records == null) return false;
        int currentBaseY = 0;

        for (ModMessages.StorageUIRecord record : records) {
            int startX = rightEdgeX + 26;
            int startY = currentBaseY + 24;
            int textVisualY = startY - 12 - (int) scrollAmount + screenY;

            if (mouseX >= rightEdgeX && mouseX <= rightEdgeX + panelWidth && mouseY >= textVisualY - 2 && mouseY <= textVisualY + 10) {
                String displayText = RadiusCraftingNames.getName(record.id(), record.displayName().getString());
                renameField = new EditBox(Minecraft.getInstance().font, rightEdgeX + 2, textVisualY - 2, 140, 10, Component.literal(displayText));
                renameField.setValue(displayText);
                renameField.setTextColor(0xFFFFFFFF);
                renameField.setFocused(true);
                renamingStorageId = record.id();
                return true;
            }

            int rows = (record.items().size() + 8) / 9;

            for (int i = 0; i < record.items().size(); i++) {
                int column = i % 9;
                int row = i / 9;
                int visualItemX = startX + (column * 18);
                int visualItemY = startY + (row * 18) - (int) scrollAmount + screenY;

                if (mouseX >= visualItemX && mouseX < visualItemX + 16 && mouseY >= visualItemY && mouseY < visualItemY + 16) {
                    ClientPlayNetworking.send(new ModMessages.StorageClickC2SPacket(record.id(), i, button, ContainerInput.PICKUP));
                    return true;
                }
            }

            currentBaseY += (rows * 18) + 26;
        }

        return true;
    }

    private int getMaxScroll() {
        List<ModMessages.StorageUIRecord> records = RadiusCraftingClient.cachedStorageRecords;
        if (records == null) return 0;
        int totalHeight = 0;
        for (ModMessages.StorageUIRecord record : records) {
            int rows = (record.items().size() + 8) / 9;
            totalHeight += (rows * 18) + 26;
        }
        int visibleHeight = 150;
        return Math.max(0, totalHeight - visibleHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isCustomUiOpen) return false;

        this.syncPosition();

        int rightEdgeX = this.getX();
        int screenY = this.getY();
        int panelWidth = 194;
        int panelHeight = 166;

        if (mouseX >= rightEdgeX && mouseX <= rightEdgeX + panelWidth && mouseY >= screenY && mouseY <= screenY + panelHeight) {
            scrollAmount -= verticalAmount * 15;
            scrollAmount = Math.max(0, Math.min(scrollAmount, getMaxScroll()));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (renameField != null && renameField.isFocused()) {
            if (event.key() == 257 || event.key() == 335 || event.key() == 320) {
                RadiusCraftingNames.setName(renamingStorageId, renameField.getValue().trim());
                renameField.setFocused(false);
                renameField = null;
                renamingStorageId = null;
                return true;
            }
            return renameField.keyPressed(event);
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (renameField != null && renameField.isFocused()) {
            return renameField.charTyped(event);
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

}
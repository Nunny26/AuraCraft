package com.radiuscrafting.mixin;

import com.radiuscrafting.client.NearbyStorageWidget;
import com.radiuscrafting.network.ModMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen implements com.radiuscrafting.client.IHandledScreenAccessor {

    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Override public int getX() { return this.leftPos; }
    @Override public int getY() { return this.topPos; }
    @Override public int getBackgroundWidth() { return this.imageWidth; }
    @Override public int getImageHeight() { return this.imageHeight; }

    @Override
    public NearbyStorageWidget getRadiusCraftingWidget() { return this.radiusCrafting$widget; }

    protected HandledScreenMixin() {
        super(net.minecraft.network.chat.Component.empty());
    }

    private NearbyStorageWidget radiusCrafting$widget;

    @Inject(method = "init", at = @At("TAIL"))
    private void radiuscrafting$onInit(CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen || (Object) this instanceof CraftingScreen) {
            if (this.minecraft != null && this.minecraft.getConnection() != null) {
                ClientPlayNetworking.send(new ModMessages.RequestStorageUIC2SPacket());
            }

            this.radiusCrafting$widget = new NearbyStorageWidget((AbstractContainerScreen<?>)(Object)this, 176, this.imageHeight);
            this.addRenderableWidget(this.radiusCrafting$widget);

            net.minecraft.client.gui.components.AbstractButton toggleButton = new net.minecraft.client.gui.components.AbstractButton(
                    this.leftPos + 126, this.height / 2 - 22, 20, 18,
                    net.minecraft.network.chat.Component.empty()
            ) {
                @Override
                public void onPress(net.minecraft.client.input.InputWithModifiers modifiers) {
                    NearbyStorageWidget.isCustomUiOpen = !NearbyStorageWidget.isCustomUiOpen;

                    int center = (HandledScreenMixin.this.width - HandledScreenMixin.this.imageWidth) / 2;
                    int shift = 0;

                    if (NearbyStorageWidget.isCustomUiOpen) {
                        // We just opened. If Recipe Book is closed, shift left by 98.
                        if (HandledScreenMixin.this.leftPos == center) shift = -98;
                    } else {
                        // We just closed. If Recipe Book is closed, shift back to center (+98).
                        if (HandledScreenMixin.this.leftPos == center - 98) shift = 98;
                    }

                    if (shift != 0) {
                        // 1. Shift the Main Inventory
                        HandledScreenMixin.this.leftPos += shift;
                        // 2. Instantly shift our Custom Button
                        this.setX(HandledScreenMixin.this.leftPos + 126);

                        // 3. Shift the Vanilla Recipe Book Button exactly ONCE!
                        for (net.minecraft.client.gui.components.events.GuiEventListener child : HandledScreenMixin.this.children()) {
                            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                                // Find any button sitting on the left side (like the Recipe Book toggle)
                                if (widget != this && widget.getY() == HandledScreenMixin.this.height / 2 - 22) {
                                    widget.setX(widget.getX() + shift);
                                }
                            }
                        }
                    }
                }

                @Override
                protected void extractContents(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
                    this.setX(HandledScreenMixin.this.leftPos + 126);
                    net.minecraft.resources.Identifier tex = net.minecraft.resources.Identifier.fromNamespaceAndPath("radiuscrafting", "textures/gui/toggle_button.png");
                    float vOffset = this.isHovered() ? 18.0f : 0.0f;
                    graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), 0.0f, vOffset, 20, 18, 20, 36);
                }

                @Override
                protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {}
            };

            this.addRenderableWidget(toggleButton);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.radiusCrafting$widget != null && this.radiusCrafting$widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void radiuscrafting$onKeyPressed(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.radiusCrafting$widget != null && this.radiusCrafting$widget.isRenaming()) {
            this.radiusCrafting$widget.keyPressed(event);
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (this.radiusCrafting$widget != null && this.radiusCrafting$widget.isRenaming()) {
            this.radiusCrafting$widget.charTyped(event);
            return true;
        }
        return super.charTyped(event);
    }
}
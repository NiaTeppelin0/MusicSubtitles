package com.example.yandexmusichud.mixin;

import com.example.yandexmusichud.hud.PlayerBadgeManager;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_355;
import net.minecraft.class_5348;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = class_355.class, remap = false)
public abstract class PlayerListHudMixin {
    @Redirect(method = "method_1919", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_327;method_27525(Lnet/minecraft/class_5348;)I", ordinal = 0, remap = false), remap = false)
    private int yandexmusichud$widenName(class_327 renderer, class_5348 text) {
        int w = renderer.method_27525(text);
        if (text instanceof class_2561 t && PlayerBadgeManager.isModTabText(t)) {
            w += PlayerBadgeManager.tabBadgeExtraWidth(renderer);
        }
        return w;
    }

    @Redirect(method = "method_1919", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_27535(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;III)V", remap = false), remap = false)
    private void yandexmusichud$drawName(class_332 context, class_327 renderer, class_2561 text, int x, int y, int color) {
        if (PlayerBadgeManager.isModTabText(text)) {
            PlayerBadgeManager.drawTabBadge(context, renderer, text, x, y, color);
        } else {
            context.method_27535(renderer, text, x, y, color);
        }
    }
}

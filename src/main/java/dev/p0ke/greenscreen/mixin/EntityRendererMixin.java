package dev.p0ke.greenscreen.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.p0ke.greenscreen.Greenscreen.EntityRenderState;
import dev.p0ke.greenscreen.GreenscreenMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(
            method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderNameTag(T entity, Component displayName, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return;

        EntityRenderState state = GreenscreenMod.greenscreen().nameTageRenderState();
        if (state == EntityRenderState.ALL) return;
        if (state == EntityRenderState.PLAYERS && entity instanceof Player) return;
        if (state == EntityRenderState.WHITELIST &&
                GreenscreenMod.greenscreen().isWhitelisted(entity.getScoreboardName())) return;
        if (state == EntityRenderState.BLACKLIST &&
                !GreenscreenMod.greenscreen().isBlacklisted(entity.getScoreboardName())) return;
        if (state == EntityRenderState.SELF &&
                entity.getScoreboardName().equals(Minecraft.getInstance().player.getScoreboardName())) return;

        ci.cancel();
    }

    @ModifyVariable(
            method = "renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Component modifyDisplayName(Component displayName) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return displayName;

        return Component.literal(GreenscreenMod.greenscreen().getTransformedNameTag(displayName.getString()));
    }
}

package dev.p0ke.greenscreen.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.p0ke.greenscreen.Greenscreen.EntityRenderState;
import dev.p0ke.greenscreen.GreenscreenMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(
            method = "renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderSectionLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return;
        if (GreenscreenMod.greenscreen().blockRenderState().enabled()) return;

        ci.cancel();
    }

    @Inject(
            method = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderEntity(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return;

        // armor stand check
        EntityRenderState state;
        if (entity instanceof ArmorStand) {
            state = GreenscreenMod.greenscreen().armorStandRenderState();
        } else {
            state = GreenscreenMod.greenscreen().entityRenderState();
        }

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

    @Inject(
            method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return;
        if (!GreenscreenMod.greenscreen().customSkyRenderState().enabled()) return;

        ci.cancel();
        GreenscreenMod.instance().drawGreenscreenSky(frustumMatrix);
    }
}

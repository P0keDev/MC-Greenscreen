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
            method = "renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderChunkLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo ci) {
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
        if (entity instanceof ArmorStand) {
            if (!GreenscreenMod.greenscreen().armorStandRenderState().enabled()) {
                ci.cancel();
            }
            return;
        }

        // entity check
        EntityRenderState state = GreenscreenMod.greenscreen().entityRenderState();
        if (state == EntityRenderState.ALL) return;
        if (state == EntityRenderState.PLAYERS && entity instanceof Player) return;
        if (state == EntityRenderState.SELF && entity.is(Minecraft.getInstance().player)) return;

        ci.cancel();
    }

    @Inject(
            method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return;
        if (!GreenscreenMod.greenscreen().customSkyRenderState().enabled()) return;

        ci.cancel();
        GreenscreenMod.instance().drawGreenscreenSky(poseStack);
    }
}

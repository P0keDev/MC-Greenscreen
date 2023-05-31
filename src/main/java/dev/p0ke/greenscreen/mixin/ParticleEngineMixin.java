package dev.p0ke.greenscreen.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.p0ke.greenscreen.GreenscreenMod;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
            at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack matrixStack, MultiBufferSource.BufferSource buffer, LightTexture lightTexture, Camera activeRenderInfo, float partialTicks, CallbackInfo ci) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return;
        if (GreenscreenMod.greenscreen().particleRenderState().enabled()) return;

        ci.cancel();
    }
}

package dev.p0ke.greenscreen.mixin;

import dev.p0ke.greenscreen.GreenscreenMod;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
    at = @At("HEAD"), cancellable = true)
    private static void onSetupFog(Camera camera, FogMode fogMode, float farPlaneDistance, boolean bl, float f, CallbackInfo ci) {
        if (!GreenscreenMod.greenscreen().state().enabled()) return;
        if (!GreenscreenMod.greenscreen().customSkyRenderState().enabled()) return;

        ci.cancel();
    }
}

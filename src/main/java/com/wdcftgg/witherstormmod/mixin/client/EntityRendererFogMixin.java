package com.wdcftgg.witherstormmod.mixin.client;

import com.wdcftgg.witherstormmod.client.OptifineCompat;
import com.wdcftgg.witherstormmod.client.WitherStormClientConfig;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void witherstormmod$disableVanillaFog(int startCoords, float partialTicks,
                                                  CallbackInfo callback) {
        if (!WitherStormClientConfig.disableVanillaFog || OptifineCompat.areShadersActive()) return;
        GlStateManager.setFog(GlStateManager.FogMode.EXP);
        GlStateManager.setFogDensity(0.0F);

        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.0F);
        GlStateManager.disableFog();
    }
}

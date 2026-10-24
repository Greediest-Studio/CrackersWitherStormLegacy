package com.wdcftgg.witherstormmod.client.render;

import com.wdcftgg.witherstormmod.client.model.SickenedIllagerModel;
import com.wdcftgg.witherstormmod.common.entity.SickenedMobEntity;
import net.minecraft.client.renderer.entity.RenderManager;

public final class SickenedIllagerRenderer<T extends SickenedMobEntity>
        extends SickenedMobRenderer<T> {

    public SickenedIllagerRenderer(RenderManager manager, boolean rangedPose,
                                   boolean aggressiveOnly, String texturePath) {
        super(manager, new SickenedIllagerModel(rangedPose), 0.5F, texturePath);
        addLayer(new SickenedIllagerHeldItemLayer(this, aggressiveOnly));
    }
}

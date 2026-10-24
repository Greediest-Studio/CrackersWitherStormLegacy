package com.wdcftgg.witherstormmod.api.common.event;

import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** 按风暴及所在维度调整吞噬资格，不污染全局方块缓存。 */
@HasResult
public final class WitherStormConsumeBlockEvent extends WitherStormEvent {
    private final IBlockState state;

    public WitherStormConsumeBlockEvent(WitherStormEntity storm, IBlockState state) {
        super(storm);
        this.state = state;
    }

    public IBlockState getState() { return state; }
}

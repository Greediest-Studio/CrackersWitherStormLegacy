package com.wdcftgg.witherstormmod.client;

import com.wdcftgg.witherstormmod.Tags;
import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID + "/client")
public final class WitherStormClientConfig {
    @Config.Name("blockClusterRendering")
    @Config.Comment("是否渲染方块质量簇；强制渲染的剧情质量簇不受此选项影响。")
    public static boolean blockClusterRendering = true;

    @Config.Name("renderDebrisCloud")
    @Config.Comment("Render the rotating debris clusters surrounding the Wither Storm.")
    public static boolean renderDebrisCloud = true;

    @Config.Name("renderDebrisRings")
    @Config.Comment("Render the broad translucent debris rings surrounding large Wither Storm phases.")
    public static boolean renderDebrisRings = true;

    @Config.Name("hideDebrisRingsUntilSplit")
    @Config.Comment("Hide debris rings until the Wither Storm reaches phase 6.")
    public static boolean hideDebrisRingsUntilSplit = false;

    @Config.Name("renderShine")
    @Config.Comment("Render the Wither Storm's distant night shine.")
    public static boolean renderShine = true;

    @Config.Name("renderTractorBeams")
    @Config.Comment("是否渲染凋零风暴头部发出的牵引光束。")
    public static boolean renderTractorBeams = true;

    @Config.Name("renderTractorBeamOverlay")
    @Config.Comment("玩家处于牵引光束中时是否渲染屏幕边缘覆盖层。")
    public static boolean renderTractorBeamOverlay = true;

    @Config.Name("tractorBeamParticles")
    @Config.Comment("是否渲染牵引光束内部粒子和远端撞击碎屑。")
    public static boolean tractorBeamParticles = true;

    @Config.Name("blindingEffects")
    @Config.Comment("是否启用白屏覆盖效果。")
    public static boolean blindingEffects = true;

    @Config.Name("cameraShakeEffects")
    @Config.Comment("是否启用相机震动效果。")
    public static boolean cameraShakeEffects = true;

    @Config.Name("chromaticAberration")
    @Config.Comment("是否启用 Formidibomb 爆炸后的色差后处理。")
    public static boolean chromaticAberration = true;

    @Config.Name("renderEmissiveDecalForHeads")
    @Config.Comment("是否渲染凋零风暴头部的发光眼睛与牙齿贴图。")
    public static boolean renderEmissiveDecalForHeads = true;

    @Config.Name("patronCosmetic")
    @Config.Comment("是否显示赞助者彩蛋外观（nonamecrackers2 命名的共生体）。")
    public static boolean patronCosmetic = true;

    @Config.Name("optifineWarning")
    @Config.Comment("进入世界时是否提示 OptiFine 兼容性警告。")
    public static boolean optifineWarning = true;

    @Config.Name("renderDistantDebris")
    @Config.Comment("远距离渲染凋零风暴时是否仍渲染碎片云。")
    public static boolean renderDistantDebris = true;

    @Config.Name("witherStormLOD")
    @Config.Comment("远距离渲染凋零风暴时是否改用低分辨率模型。")
    public static boolean witherStormLOD = false;

    @Config.Name("lowResModels")
    @Config.Comment("第 4 阶段及以上的风暴模型是否使用更大的方块（低分辨率）。")
    public static boolean lowResModels = false;

    @Config.Name("distantRenderer")
    @Config.Comment("是否允许凋零风暴超出原版实体渲染距离后仍被渲染。")
    public static boolean distantRenderer = true;

    @Config.Name("distantFog")
    @Config.Comment("远距离渲染凋零风暴时是否对牵引光束应用雾化。")
    public static boolean distantFog = true;

    @Config.Name("disableVanillaFog")
    @Config.Comment("是否完全禁用 Minecraft 原版雾，包括普通、Boss、失明、水下与岩浆雾。")
    public static boolean disableVanillaFog = true;

    @Config.Name("renderPulse")
    @Config.Comment("风暴被撕裂时是否渲染脉冲方块效果。")
    public static boolean renderPulse = true;

    @Config.Name("aprilFools")
    @Config.Comment("是否启用愚人节粉色风暴和心形粒子效果。")
    public static boolean aprilFools = true;

    @Config.Name("vertexBufferRendering")
    @Config.Comment("是否使用 VBO 缓存方块质量簇与风暴碎片云；主体模型由 1.12 ModelRenderer 显示列表缓存。")
    public static boolean vertexBufferRendering = true;

    @Config.Name("asyncBufferBuilders")
    @Config.Comment("是否在线程池构建 VBO 的纯 CPU 顶点数据；OpenGL 上传、绘制和删除始终留在渲染线程。")
    public static boolean asyncBufferBuilders = true;

    @Config.Name("renderSkyAmbienceEffects")
    @Config.Comment("风暴附近时是否渲染天空/云/雾的氛围配色。")
    public static boolean renderSkyAmbienceEffects = true;

    @Config.Name("earRingingEffects")
    @Config.Comment("是否启用耳鸣音效（Formidibomb 爆炸的响亮音效）。")
    public static boolean earRingingEffects = true;

    @Config.Name("witherSicknessLayer")
    @Config.Comment("是否对感染凋零病的实体叠加病化覆盖层。")
    public static boolean witherSicknessLayer = true;

    private WitherStormClientConfig() {
    }
}

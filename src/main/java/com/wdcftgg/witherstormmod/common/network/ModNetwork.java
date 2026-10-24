package com.wdcftgg.witherstormmod.common.network;

import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import com.wdcftgg.witherstormmod.common.entity.WitherStormEntity;
import com.wdcftgg.witherstormmod.common.entity.SupplementalEntities;
import com.wdcftgg.witherstormmod.common.entity.DistantStormTrackingResync;
import com.wdcftgg.witherstormmod.common.util.StormDiagnosticLogger;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModNetwork {
    public static final int COMMAND_BLOCK_PARTICLES_GAUSSIAN = 0;
    public static final int COMMAND_BLOCK_PARTICLES_UNIFORM_VELOCITY = 1;
    public static final int COMMAND_BLOCK_PARTICLES_EXACT_VELOCITY = 2;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);
    private static boolean registered;

    private ModNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        int discriminator = 0;
        CHANNEL.registerMessage(ShakeScreenMessage.Handler.class, ShakeScreenMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(BlindScreenMessage.Handler.class, BlindScreenMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(GlobalSoundMessage.Handler.class, GlobalSoundMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(FormidibombExplosionMessage.Handler.class, FormidibombExplosionMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(InjureWitherStormHeadMessage.Handler.class, InjureWitherStormHeadMessage.class,
                discriminator++, Side.SERVER);
        CHANNEL.registerMessage(BossThemeAccessMessage.Handler.class, BossThemeAccessMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(WitherSicknessMessage.Handler.class, WitherSicknessMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(CommandBlockParticlesMessage.Handler.class, CommandBlockParticlesMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(WitherStormLoopMessage.Handler.class, WitherStormLoopMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(WitherStormRotationMessage.Handler.class,
                WitherStormRotationMessage.class, discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(UpdateDamagingProjectileMessage.Handler.class,
                UpdateDamagingProjectileMessage.class, discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(HeadAttackedMessage.Handler.class, HeadAttackedMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(CreateDebrisMessage.Handler.class, CreateDebrisMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(StopSoundMessage.Handler.class, StopSoundMessage.class,
                discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(AttackPlayingDeadCoreMessage.Handler.class,
                AttackPlayingDeadCoreMessage.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(CommandBlockTickParticlesMessage.Handler.class,
                CommandBlockTickParticlesMessage.class, discriminator++, Side.CLIENT);
        CHANNEL.registerMessage(ClientWorldReadyMessage.Handler.class,
                ClientWorldReadyMessage.class, discriminator++, Side.SERVER);
        CHANNEL.registerMessage(DiagnosticLoggingMessage.Handler.class,
                DiagnosticLoggingMessage.class, discriminator++, Side.CLIENT);
        registered = true;
    }

    public static void shakeTracking(Entity entity, float duration, float power) {
        if (entity == null || entity.world.isRemote) return;
        CHANNEL.sendToAllTracking(new ShakeScreenMessage(duration, power), entity);
    }

    public static void shakePlayer(EntityPlayerMP player, float duration, float power) {
        if (player == null) return;
        CHANNEL.sendTo(new ShakeScreenMessage(duration, power), player);
    }

    public static void setPlayerMotion(EntityPlayerMP player, Entity movedEntity, Vec3d motion) {
        if (player == null || movedEntity == null || motion == null) return;
        player.connection.sendPacket(new SPacketEntityVelocity(
                movedEntity.getEntityId(), motion.x, motion.y, motion.z));
    }

    public static void syncDamagingProjectile(EntityFireball projectile) {
        if (projectile == null || projectile.world == null || projectile.world.isRemote) return;
        CHANNEL.sendToAllTracking(new UpdateDamagingProjectileMessage(projectile), projectile);
    }

    public static void notifyHeadAttacked(Entity stormPart, int head) {
        if (stormPart == null || stormPart.world == null || stormPart.world.isRemote) return;
        CHANNEL.sendToAllTracking(new HeadAttackedMessage(stormPart.getEntityId(), head), stormPart);
    }

    public static void syncWitherSickness(EntityLivingBase entity, NBTTagCompound data) {
        if (entity == null || entity.world == null || entity.world.isRemote || data == null) return;
        WitherSicknessMessage message = new WitherSicknessMessage(entity.getEntityId(), data);
        CHANNEL.sendToAllTracking(message, entity);
        if (entity instanceof EntityPlayerMP) CHANNEL.sendTo(message, (EntityPlayerMP) entity);
    }

    public static void syncWitherSicknessTo(EntityLivingBase entity, NBTTagCompound data,
                                             EntityPlayerMP player) {
        if (entity == null || data == null || player == null) return;
        CHANNEL.sendTo(new WitherSicknessMessage(entity.getEntityId(), data), player);
    }

    public static void shakeNear(World world, double x, double y, double z, double radius,
                                 float duration, float power) {
        if (world == null || world.isRemote) return;
        CHANNEL.sendToAllAround(new ShakeScreenMessage(duration, power),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, radius));
    }

    public static void shakeDimension(World world, float duration, float power) {
        if (world == null || world.isRemote) return;
        CHANNEL.sendToDimension(new ShakeScreenMessage(duration, power), world.provider.getDimension());
    }

    public static void shakeAll(World world, float duration, float power) {
        if (world == null || world.isRemote) return;
        CHANNEL.sendToAll(new ShakeScreenMessage(duration, power));
    }

    public static void blindTracking(Entity entity, int duration, int fadeInDuration, int fadeOutDuration) {
        if (entity == null || entity.world.isRemote) return;
        CHANNEL.sendToAllTracking(new BlindScreenMessage(duration, fadeInDuration, fadeOutDuration), entity);
    }

    public static void blindNear(World world, double x, double y, double z, double radius,
                                 int duration, int fadeInDuration, int fadeOutDuration) {
        if (world == null || world.isRemote) return;
        CHANNEL.sendToAllAround(new BlindScreenMessage(duration, fadeInDuration, fadeOutDuration),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, radius));
    }

    public static void playGlobalSound(World world, SoundEvent sound, float volume, float pitch) {
        if (world == null || world.isRemote || sound == null || sound.getRegistryName() == null) return;
        CHANNEL.sendToDimension(new GlobalSoundMessage(sound.getRegistryName(), volume, pitch),
                world.provider.getDimension());
    }

    public static void playGlobalSoundAll(World world, SoundEvent sound, float volume, float pitch) {
        if (world == null || world.isRemote || sound == null || sound.getRegistryName() == null) return;
        CHANNEL.sendToAll(new GlobalSoundMessage(sound.getRegistryName(), volume, pitch));
    }

    public static void stopSound(EntityPlayerMP player, SoundEvent sound, SoundCategory category) {
        if (player == null || sound == null || sound.getRegistryName() == null || category == null) return;
        CHANNEL.sendTo(new StopSoundMessage(sound.getRegistryName(), category), player);
    }

    public static void sendFormidibombExplosion(World world, Entity source, double x, double y, double z,
                                                int radius, int squish) {
        if (world == null || world.isRemote) return;
        int sourceId = source == null ? 0 : source.getEntityId();
        CHANNEL.sendToDimension(new FormidibombExplosionMessage(sourceId, x, y, z, radius, squish),
                world.provider.getDimension());
    }

    public static void notifyClientWorldReady(int dimension) {
        CHANNEL.sendToServer(new ClientWorldReadyMessage(dimension));
    }


    public static void syncDiagnosticLogging(EntityPlayerMP player) {
        if (player != null) {
            CHANNEL.sendTo(new DiagnosticLoggingMessage(StormDiagnosticLogger.isEnabled()), player);
        }
    }


    public static void syncDiagnosticLogging() {
        CHANNEL.sendToAll(new DiagnosticLoggingMessage(StormDiagnosticLogger.isEnabled()));
    }

    public static void sendCommandBlockParticles(World world, Vec3d position, int count,
                                                 double spreadX, double spreadY, double spreadZ,
                                                 double speed, int distribution) {
        if (world == null || world.isRemote || position == null || count <= 0) return;
        CHANNEL.sendToAllAround(new CommandBlockParticlesMessage(position, count,
                        spreadX, spreadY, spreadZ, speed, distribution),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(),
                        position.x, position.y, position.z, 192.0D));
    }

    public static void sendCommandBlockTickParticles(
            SupplementalEntities.CommandBlockEntity entity, float particleSpeed,
            int luringPlayerId) {
        if (entity == null || entity.world == null || entity.world.isRemote) return;
        CHANNEL.sendToAllTracking(new CommandBlockTickParticlesMessage(entity.getEntityId(),
                particleSpeed, luringPlayerId), entity);
    }

    public static void injureWitherStormHead(WitherStormEntity storm, int head) {
        if (storm == null || !storm.world.isRemote || head < 0 || head >= storm.getTotalHeads()) return;
        CHANNEL.sendToServer(new InjureWitherStormHeadMessage(storm.getEntityId(), head));
    }

    public static void injureWitherStormHead(SupplementalEntities.WitherStormSegmentEntity segment, int head) {
        if (segment == null || !segment.world.isRemote || head < 0 || head >= segment.getTotalHeads()) return;
        CHANNEL.sendToServer(new InjureWitherStormHeadMessage(segment.getEntityId(), head));
    }

    public static void attackPlayingDeadCore(SupplementalEntities.CommandBlockEntity core) {
        if (core == null || !core.world.isRemote) return;
        CHANNEL.sendToServer(new AttackPlayingDeadCoreMessage(core.getEntityId()));
    }

    public static void sendBossThemeAccess(EntityPlayerMP player, WitherStormEntity storm, boolean allowed) {
        if (player == null || storm == null || storm.world.isRemote) return;
        CHANNEL.sendTo(new BossThemeAccessMessage(storm.getEntityId(), allowed), player);
    }

    public static void createDebris(EntityPlayerMP player, WitherStormEntity storm, boolean hidden) {
        if (player == null || storm == null || storm.world == null || storm.world.isRemote) return;
        CHANNEL.sendTo(new CreateDebrisMessage(storm.getEntityId(), hidden), player);
    }

    public static void updateWitherStormLoop(WitherStormEntity storm) {
        if (storm == null || storm.world == null || storm.world.isRemote) return;
        CHANNEL.sendToDimension(new WitherStormLoopMessage(storm.getEntityId(), storm.posX,
                        storm.posY, storm.posZ, storm.getPhase(), true),
                storm.world.provider.getDimension());
    }

    public static void removeWitherStormLoop(WitherStormEntity storm) {
        if (storm == null || storm.world == null || storm.world.isRemote) return;
        CHANNEL.sendToDimension(new WitherStormLoopMessage(storm.getEntityId(), storm.posX,
                        storm.posY, storm.posZ, storm.getPhase(), false),
                storm.world.provider.getDimension());
    }


    public static void syncWitherStormRotation(WitherStormEntity storm) {
        if (storm == null || storm.world == null || storm.world.isRemote) return;
        CHANNEL.sendToAllTracking(new WitherStormRotationMessage(storm.getEntityId(),
                storm.getXBodyRot(), storm.getBodyYRotation(1.0F)), storm);
    }

    public static final class ShakeScreenMessage implements IMessage {
        private float duration;
        private float power;

        public ShakeScreenMessage() {
        }

        public ShakeScreenMessage(float duration, float power) {
            this.duration = duration;
            this.power = power;
        }

        public float getDuration() {
            return duration;
        }

        public float getPower() {
            return power;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            duration = buffer.readFloat();
            power = buffer.readFloat();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeFloat(duration);
            buffer.writeFloat(power);
        }

        public static final class Handler implements IMessageHandler<ShakeScreenMessage, IMessage> {
            @Override
            public IMessage onMessage(ShakeScreenMessage message, MessageContext context) {
                WitherStormMod.proxy.handleShakeScreen(message.duration, message.power);
                return null;
            }
        }
    }

    public static final class BlindScreenMessage implements IMessage {
        private int duration;
        private int fadeInDuration;
        private int fadeOutDuration;

        public BlindScreenMessage() {
        }

        public BlindScreenMessage(int duration, int fadeInDuration, int fadeOutDuration) {
            this.duration = duration;
            this.fadeInDuration = fadeInDuration;
            this.fadeOutDuration = fadeOutDuration;
        }

        public int getDuration() {
            return duration;
        }

        public int getFadeInDuration() {
            return fadeInDuration;
        }

        public int getFadeOutDuration() {
            return fadeOutDuration;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            duration = buffer.readInt();
            fadeInDuration = buffer.readInt();
            fadeOutDuration = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(duration);
            buffer.writeInt(fadeInDuration);
            buffer.writeInt(fadeOutDuration);
        }

        public static final class Handler implements IMessageHandler<BlindScreenMessage, IMessage> {
            @Override
            public IMessage onMessage(BlindScreenMessage message, MessageContext context) {
                WitherStormMod.proxy.handleBlindScreen(
                        message.duration, message.fadeInDuration, message.fadeOutDuration);
                return null;
            }
        }
    }

    public static final class GlobalSoundMessage implements IMessage {
        private ResourceLocation sound;
        private float volume;
        private float pitch;

        public GlobalSoundMessage() {
        }

        public GlobalSoundMessage(ResourceLocation sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public ResourceLocation getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            sound = new ResourceLocation(ByteBufUtils.readUTF8String(buffer));
            volume = buffer.readFloat();
            pitch = buffer.readFloat();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            ByteBufUtils.writeUTF8String(buffer, sound.toString());
            buffer.writeFloat(volume);
            buffer.writeFloat(pitch);
        }

        public static final class Handler implements IMessageHandler<GlobalSoundMessage, IMessage> {
            @Override
            public IMessage onMessage(GlobalSoundMessage message, MessageContext context) {
                WitherStormMod.proxy.handleGlobalSound(message.sound, message.volume, message.pitch);
                return null;
            }
        }
    }

    public static final class StopSoundMessage implements IMessage {
        private ResourceLocation sound;
        private SoundCategory category;

        public StopSoundMessage() {
        }

        public StopSoundMessage(ResourceLocation sound, SoundCategory category) {
            this.sound = sound;
            this.category = category;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            sound = new ResourceLocation(ByteBufUtils.readUTF8String(buffer));
            int ordinal = buffer.readUnsignedByte();
            SoundCategory[] categories = SoundCategory.values();
            category = ordinal < categories.length ? categories[ordinal] : SoundCategory.MASTER;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            ByteBufUtils.writeUTF8String(buffer, sound.toString());
            buffer.writeByte(category.ordinal());
        }

        public static final class Handler implements IMessageHandler<StopSoundMessage, IMessage> {
            @Override
            public IMessage onMessage(StopSoundMessage message, MessageContext context) {
                WitherStormMod.proxy.handleStopSound(message.sound, message.category);
                return null;
            }
        }
    }

    public static final class FormidibombExplosionMessage implements IMessage {
        private int sourceEntityId;
        private double x;
        private double y;
        private double z;
        private byte radius;
        private byte squish;

        public FormidibombExplosionMessage() {
        }

        public FormidibombExplosionMessage(int sourceEntityId, double x, double y, double z,
                                           int radius, int squish) {
            this.sourceEntityId = sourceEntityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = (byte) radius;
            this.squish = (byte) squish;
        }

        public int getSourceEntityId() {
            return sourceEntityId;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public int getRadius() {
            return radius;
        }

        public int getSquish() {
            return squish;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            sourceEntityId = buffer.readInt();
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            radius = buffer.readByte();
            squish = buffer.readByte();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(sourceEntityId);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeByte(radius);
            buffer.writeByte(squish);
        }

        public static final class Handler implements IMessageHandler<FormidibombExplosionMessage, IMessage> {
            @Override
            public IMessage onMessage(FormidibombExplosionMessage message, MessageContext context) {
                WitherStormMod.proxy.handleFormidibombExplosion(message.sourceEntityId,
                        message.x, message.y, message.z, message.radius, message.squish);
                return null;
            }
        }
    }


    public static final class ClientWorldReadyMessage implements IMessage {
        private int dimension;

        public ClientWorldReadyMessage() {
        }

        public ClientWorldReadyMessage(int dimension) {
            this.dimension = dimension;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            dimension = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(dimension);
        }

        public static final class Handler
                implements IMessageHandler<ClientWorldReadyMessage, IMessage> {
            @Override
            public IMessage onMessage(final ClientWorldReadyMessage message,
                                      final MessageContext context) {
                final EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    if (player.isDead || player.dimension != message.dimension
                            || player.getServerWorld().provider.getDimension() != message.dimension) {
                        StormDiagnosticLogger.warn(
                                "[风暴诊断][忽略客户端维度就绪] 玩家={} 消息维度={} 玩家维度={} 世界维度={} 死亡={}",
                                player.getName(), message.dimension, player.dimension,
                                player.getServerWorld().provider.getDimension(), player.isDead);
                        return;
                    }
                    EntityTracker tracker = player.getServerWorld().getEntityTracker();
                    if (tracker instanceof DistantStormTrackingResync) {
                        ((DistantStormTrackingResync) tracker)
                                .witherstormmod$resyncDistantStorms(player);
                    }
                });
                return null;
            }
        }
    }


    public static final class DiagnosticLoggingMessage implements IMessage {
        private boolean enabled;

        public DiagnosticLoggingMessage() {
        }

        public DiagnosticLoggingMessage(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            enabled = buffer.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeBoolean(enabled);
        }

        public static final class Handler
                implements IMessageHandler<DiagnosticLoggingMessage, IMessage> {
            @Override
            public IMessage onMessage(final DiagnosticLoggingMessage message,
                                      MessageContext context) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(
                        () -> StormDiagnosticLogger.setEnabled(message.enabled));
                return null;
            }
        }
    }

    public static final class CommandBlockParticlesMessage implements IMessage {
        private double x;
        private double y;
        private double z;
        private int count;
        private double spreadX;
        private double spreadY;
        private double spreadZ;
        private double speed;
        private int distribution;

        public CommandBlockParticlesMessage() {
        }

        public CommandBlockParticlesMessage(Vec3d position, int count,
                                            double spreadX, double spreadY, double spreadZ,
                                            double speed, int distribution) {
            x = position.x;
            y = position.y;
            z = position.z;
            this.count = count;
            this.spreadX = spreadX;
            this.spreadY = spreadY;
            this.spreadZ = spreadZ;
            this.speed = speed;
            this.distribution = distribution;
        }

        public Vec3d getPosition() {
            return new Vec3d(x, y, z);
        }

        public int getCount() {
            return count;
        }

        public double getSpreadX() {
            return spreadX;
        }

        public double getSpreadY() {
            return spreadY;
        }

        public double getSpreadZ() {
            return spreadZ;
        }

        public double getSpeed() {
            return speed;
        }

        public int getDistribution() {
            return distribution;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            count = buffer.readInt();
            spreadX = buffer.readDouble();
            spreadY = buffer.readDouble();
            spreadZ = buffer.readDouble();
            speed = buffer.readDouble();
            distribution = buffer.readUnsignedByte();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeInt(count);
            buffer.writeDouble(spreadX);
            buffer.writeDouble(spreadY);
            buffer.writeDouble(spreadZ);
            buffer.writeDouble(speed);
            buffer.writeByte(distribution);
        }

        public static final class Handler
                implements IMessageHandler<CommandBlockParticlesMessage, IMessage> {
            @Override
            public IMessage onMessage(CommandBlockParticlesMessage message,
                                      MessageContext context) {
                WitherStormMod.proxy.handleCommandBlockParticles(message);
                return null;
            }
        }
    }


    public static final class CommandBlockTickParticlesMessage implements IMessage {
        private int entityId;
        private float particleSpeed;
        private int luringPlayerId;

        public CommandBlockTickParticlesMessage() {
        }

        private CommandBlockTickParticlesMessage(int entityId, float particleSpeed,
                                                 int luringPlayerId) {
            this.entityId = entityId;
            this.particleSpeed = particleSpeed;
            this.luringPlayerId = luringPlayerId;
        }

        public int getEntityId() {
            return entityId;
        }

        public float getParticleSpeed() {
            return particleSpeed;
        }

        public int getLuringPlayerId() {
            return luringPlayerId;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            particleSpeed = buffer.readFloat();
            luringPlayerId = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeFloat(particleSpeed);
            buffer.writeInt(luringPlayerId);
        }

        public static final class Handler
                implements IMessageHandler<CommandBlockTickParticlesMessage, IMessage> {
            @Override
            public IMessage onMessage(CommandBlockTickParticlesMessage message,
                                      MessageContext context) {
                WitherStormMod.proxy.handleCommandBlockTickParticles(message);
                return null;
            }
        }
    }

    public static final class WitherStormLoopMessage implements IMessage {
        private int entityId;
        private double x;
        private double y;
        private double z;
        private int phase;
        private boolean active;

        public WitherStormLoopMessage() {
        }

        public WitherStormLoopMessage(int entityId, double x, double y, double z,
                                      int phase, boolean active) {
            this.entityId = entityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.phase = phase;
            this.active = active;
        }

        public int getEntityId() { return entityId; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public int getPhase() { return phase; }
        public boolean isActive() { return active; }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            phase = buffer.readUnsignedByte();
            active = buffer.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeByte(phase);
            buffer.writeBoolean(active);
        }

        public static final class Handler implements IMessageHandler<WitherStormLoopMessage, IMessage> {
            @Override
            public IMessage onMessage(WitherStormLoopMessage message, MessageContext context) {
                WitherStormMod.proxy.handleWitherStormLoop(message);
                return null;
            }
        }
    }

    public static final class WitherStormRotationMessage implements IMessage {
        private int entityId;
        private float xRotation;
        private float yRotation;

        public WitherStormRotationMessage() {
        }

        public WitherStormRotationMessage(int entityId, float xRotation, float yRotation) {
            this.entityId = entityId;
            this.xRotation = xRotation;
            this.yRotation = yRotation;
        }

        public int getEntityId() { return entityId; }
        public float getXRotation() { return xRotation; }
        public float getYRotation() { return yRotation; }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            xRotation = buffer.readFloat();
            yRotation = buffer.readFloat();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeFloat(xRotation);
            buffer.writeFloat(yRotation);
        }

        public static final class Handler implements IMessageHandler<WitherStormRotationMessage, IMessage> {
            @Override
            public IMessage onMessage(WitherStormRotationMessage message, MessageContext context) {
                WitherStormMod.proxy.handleWitherStormRotation(message.entityId,
                        message.xRotation, message.yRotation);
                return null;
            }
        }
    }

    public static final class BossThemeAccessMessage implements IMessage {
        private int entityId;
        private boolean allowed;

        public BossThemeAccessMessage() {
        }

        public BossThemeAccessMessage(int entityId, boolean allowed) {
            this.entityId = entityId;
            this.allowed = allowed;
        }

        public int getEntityId() {
            return entityId;
        }

        public boolean isAllowed() {
            return allowed;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            allowed = buffer.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeBoolean(allowed);
        }

        public static final class Handler implements IMessageHandler<BossThemeAccessMessage, IMessage> {
            @Override
            public IMessage onMessage(BossThemeAccessMessage message, MessageContext context) {
                WitherStormMod.proxy.handleBossThemeAccess(message.entityId, message.allowed);
                return null;
            }
        }
    }

    public static final class CreateDebrisMessage implements IMessage {
        private int entityId;
        private boolean hidden;

        public CreateDebrisMessage() {
        }

        public CreateDebrisMessage(int entityId, boolean hidden) {
            this.entityId = entityId;
            this.hidden = hidden;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            hidden = buffer.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeBoolean(hidden);
        }

        public static final class Handler implements IMessageHandler<CreateDebrisMessage, IMessage> {
            @Override
            public IMessage onMessage(CreateDebrisMessage message, MessageContext context) {
                WitherStormMod.proxy.handleCreateDebris(message.entityId, message.hidden);
                return null;
            }
        }
    }

    public static final class WitherSicknessMessage implements IMessage {
        private int entityId;
        private NBTTagCompound data;

        public WitherSicknessMessage() {
        }

        public WitherSicknessMessage(int entityId, NBTTagCompound data) {
            this.entityId = entityId;
            this.data = data.copy();
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            data = ByteBufUtils.readTag(buffer);
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            ByteBufUtils.writeTag(buffer, data);
        }

        public static final class Handler implements IMessageHandler<WitherSicknessMessage, IMessage> {
            @Override
            public IMessage onMessage(WitherSicknessMessage message, MessageContext context) {
                WitherStormMod.proxy.handleWitherSicknessSync(message.entityId, message.data);
                return null;
            }
        }
    }

    public static final class InjureWitherStormHeadMessage implements IMessage {
        private int entityId;
        private int head;

        public InjureWitherStormHeadMessage() {
        }

        public InjureWitherStormHeadMessage(int entityId, int head) {
            this.entityId = entityId;
            this.head = head;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            head = buffer.readUnsignedByte();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeByte(head);
        }

        public static final class Handler implements IMessageHandler<InjureWitherStormHeadMessage, IMessage> {
            @Override
            public IMessage onMessage(final InjureWitherStormHeadMessage message,
                                      final MessageContext context) {
                final EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    Entity entity = player.world.getEntityByID(message.entityId);
                    boolean accepted = false;
                    if (entity instanceof WitherStormEntity && message.head >= 0
                            && message.head < ((WitherStormEntity) entity).getTotalHeads()) {
                        WitherStormEntity storm = (WitherStormEntity) entity;
                        double reach = player.interactionManager.getBlockReachDistance();
                        if (storm.tractorBeamActive(message.head)
                                && storm.canPlayerReachHead(player, message.head, reach)) {
                            accepted = storm.attackHead(message.head, player);
                        }
                    } else if (entity instanceof SupplementalEntities.WitherStormSegmentEntity
                            && message.head >= 0
                            && message.head < ((SupplementalEntities.WitherStormSegmentEntity) entity).getTotalHeads()) {
                        SupplementalEntities.WitherStormSegmentEntity segment =
                                (SupplementalEntities.WitherStormSegmentEntity) entity;
                        double reach = player.interactionManager.getBlockReachDistance();
                        if (segment.tractorBeamActive(message.head)
                                && segment.canPlayerReachHead(player, message.head, reach)) {
                            accepted = segment.attackHead(message.head, player);
                        }
                    }
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            accepted ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG
                                    : SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE,
                            SoundCategory.PLAYERS, 1.0F, 1.0F);
                });
                return null;
            }
        }
    }

    public static final class AttackPlayingDeadCoreMessage implements IMessage {
        private int entityId;

        public AttackPlayingDeadCoreMessage() {
        }

        public AttackPlayingDeadCoreMessage(int entityId) {
            this.entityId = entityId;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
        }

        public static final class Handler implements IMessageHandler<AttackPlayingDeadCoreMessage, IMessage> {
            @Override
            public IMessage onMessage(final AttackPlayingDeadCoreMessage message,
                                      final MessageContext context) {
                final EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    Entity entity = player.world.getEntityByID(message.entityId);
                    if (!(entity instanceof SupplementalEntities.CommandBlockEntity)) return;
                    SupplementalEntities.CommandBlockEntity core =
                            (SupplementalEntities.CommandBlockEntity) entity;
                    if (!canPlayerHitPlayingDeadCore(player, core)) return;
                    core.attackPlayingDeadCore(player);
                });
                return null;
            }

            private static boolean canPlayerHitPlayingDeadCore(
                    EntityPlayerMP player, SupplementalEntities.CommandBlockEntity core) {
                if (!core.isEntityAlive() || core.isIndependentBowelsPart()
                        || core.getCoreState()
                        != SupplementalEntities.CommandBlockEntity.CoreState.PLAYING_DEAD) return false;
                double reach = player.interactionManager.getBlockReachDistance();
                Vec3d eyes = player.getPositionEyes(1.0F);
                Vec3d look = player.getLook(1.0F);
                Vec3d end = new Vec3d(eyes.x + look.x * reach,
                        eyes.y + look.y * reach, eyes.z + look.z * reach);


                RayTraceResult coreHit = core.getInteractionBoundingBox()
                        .grow(Math.max(0.0D, core.getCollisionBorderSize()))
                        .calculateIntercept(eyes, end);
                if (coreHit == null || coreHit.hitVec == null) return false;



                RayTraceResult blockHit = player.world.rayTraceBlocks(eyes, end, false, true, false);
                if (blockHit == null || blockHit.hitVec == null
                        || eyes.distanceTo(blockHit.hitVec) + 0.35D
                        >= eyes.distanceTo(coreHit.hitVec)) return true;




                BlockPos blockPos = blockHit.getBlockPos();
                return blockPos != null && core.getInteractionBoundingBox()
                        .intersects(new AxisAlignedBB(blockPos));
            }
        }
    }

    public static final class UpdateDamagingProjectileMessage implements IMessage {
        private int entityId;
        private double accelerationX;
        private double accelerationY;
        private double accelerationZ;
        private boolean validProjectileUpdate;
        private int legacyHead = -1;

        public UpdateDamagingProjectileMessage() {
        }

        public UpdateDamagingProjectileMessage(EntityFireball projectile) {
            entityId = projectile.getEntityId();
            accelerationX = projectile.accelerationX;
            accelerationY = projectile.accelerationY;
            accelerationZ = projectile.accelerationZ;
            validProjectileUpdate = true;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();



            if (buffer.readableBytes() == 1) {
                legacyHead = buffer.readUnsignedByte();
                return;
            }
            if (buffer.readableBytes() < Double.BYTES * 3) {
                buffer.skipBytes(buffer.readableBytes());
                return;
            }
            accelerationX = buffer.readDouble();
            accelerationY = buffer.readDouble();
            accelerationZ = buffer.readDouble();
            validProjectileUpdate = true;
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeDouble(accelerationX);
            buffer.writeDouble(accelerationY);
            buffer.writeDouble(accelerationZ);
        }

        public static final class Handler
                implements IMessageHandler<UpdateDamagingProjectileMessage, IMessage> {
            @Override
            public IMessage onMessage(UpdateDamagingProjectileMessage message,
                                      MessageContext context) {
                if (message.legacyHead >= 0) {
                    WitherStormMod.proxy.handleHeadAttacked(message.entityId, message.legacyHead);
                } else if (message.validProjectileUpdate) {
                    WitherStormMod.proxy.handleDamagingProjectileSync(message.entityId,
                            message.accelerationX, message.accelerationY, message.accelerationZ);
                }
                return null;
            }
        }
    }

    public static final class HeadAttackedMessage implements IMessage {
        private int entityId;
        private int head;

        public HeadAttackedMessage() {
        }

        public HeadAttackedMessage(int entityId, int head) {
            this.entityId = entityId;
            this.head = head;
        }

        @Override
        public void fromBytes(ByteBuf buffer) {
            entityId = buffer.readInt();
            head = buffer.readUnsignedByte();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(entityId);
            buffer.writeByte(head);
        }

        public static final class Handler implements IMessageHandler<HeadAttackedMessage, IMessage> {
            @Override
            public IMessage onMessage(HeadAttackedMessage message, MessageContext context) {
                WitherStormMod.proxy.handleHeadAttacked(message.entityId, message.head);
                return null;
            }
        }
    }

}

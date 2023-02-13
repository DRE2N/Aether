package de.erethon.aether.network;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.bedrock.chat.MessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceLocation;

public class AetherPacketHandler extends MessageToByteEncoder<Packet> {

    private final Aether aether;
    private final ActiveCreatureManager manager;

    public AetherPacketHandler(Aether aether) {
        this.aether = aether;
        this.manager = aether.getActiveCreatureManager();
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return msg instanceof ClientboundAddEntityPacket;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception {
        final FriendlyByteBuf fbb = new FriendlyByteBuf(out);
        if (msg instanceof ClientboundAddEntityPacket packet) {
            encode(ctx, packet, fbb);
        }
    }

    private void encode(ChannelHandlerContext ctx, ClientboundAddEntityPacket packet, FriendlyByteBuf buf) {
        ActiveNPC npc = manager.get(packet.getUUID());
        MessageUtil.log("Found entity packet: " + packet.getType());
        //writePacketId(ctx, packet, buf);
        buf.writeId(BuiltInRegistries.ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft", "pig")));

        if (npc != null) {
            MessageUtil.log("AE entity: " + packet.getType());
            //writePacketId(ctx, packet, buf);
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, npc.getNpc().getDisplayType());
        }
    }

    private void writePacketId(final ChannelHandlerContext ctx, final Packet<?> packet, final FriendlyByteBuf buf) {
        buf.writeVarInt(ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get().getPacketId(PacketFlow.CLIENTBOUND, packet));
    }
}
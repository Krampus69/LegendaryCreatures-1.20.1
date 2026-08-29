package sfiomn.legendarycreatures.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sfiomn.legendarycreatures.registry.BlockEntityRegistry;

import java.util.UUID;

public class HoundTrapBlockEntity extends BlockEntity {

    private UUID trapped;

    public HoundTrapBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.HOUND_TRAP.get(), pos, state);
    }

    @Nullable
    public UUID getTrapped() {
        return this.trapped;
    }

    public void setTrapped(@Nullable UUID trapped) {
        this.trapped = trapped;
        setChanged();

        if (this.level != null)
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        if (this.trapped != null)
            tag.putUUID("trapped", this.trapped);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.trapped = tag.hasUUID("trapped") ? tag.getUUID("trapped") : null;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

package sfiomn.legendarycreatures.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import sfiomn.legendarycreatures.items.FertileSlimeballItem;
import sfiomn.legendarycreatures.registry.EntityTypeRegistry;
import sfiomn.legendarycreatures.registry.ItemRegistry;

public class FertileSlimeballEntity extends ThrowableItemProjectile {

    public FertileSlimeballEntity(EntityType<? extends FertileSlimeballEntity> type, Level level) {
        super(type, level);
    }

    public FertileSlimeballEntity(Level level, LivingEntity shooter) {
        super(EntityTypeRegistry.FERTILE_SLIMEBALL.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegistry.FERTILE_SLIMEBALL.get();
    }

    private ParticleOptions getParticle() {
        ItemStack stack = this.getItemRaw();
        return stack.isEmpty() ? ParticleTypes.ITEM_SLIME : new ItemParticleOption(ParticleTypes.ITEM, stack);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particle = this.getParticle();
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos hitPos = result.getBlockPos();
            Direction face = result.getDirection();
            BlockPos insidePos = hitPos.relative(face);
            BlockState inside = serverLevel.getBlockState(insidePos);
            if (inside.getBlock() instanceof BonemealableBlock && !(inside.getBlock() instanceof GrassBlock)) {
                if (FertileSlimeballItem.fertilize(serverLevel, insidePos, Direction.UP)) return;
            }
            if (!FertileSlimeballItem.fertilize(serverLevel, hitPos, face)) {
                FertileSlimeballItem.fertilize(serverLevel, insidePos, Direction.UP);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = result.getEntity().blockPosition();
            if (!FertileSlimeballItem.fertilize(serverLevel, pos, Direction.UP)) {
                FertileSlimeballItem.fertilize(serverLevel, result.getEntity().getOnPos(), Direction.UP);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.playSound(SoundEvents.SLIME_BLOCK_BREAK, 1.0F, 0.9F);
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}

package sfiomn.legendarycreatures.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import sfiomn.legendarycreatures.api.ModDamageTypes;
import sfiomn.legendarycreatures.registry.SoundRegistry;
import sfiomn.legendarycreatures.util.DamageSourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HoundTrapBlock extends Block implements EntityBlock {

    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");

    private static final VoxelShape OPEN_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    private static final VoxelShape CLOSED_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 6.0D, 15.0D);

    private static final Vec3 HOLD_MULTIPLIER = new Vec3(0.001D, 0.001D, 0.001D);
    private static final int TRAP_DURATION = 200;
    private static final int REARM_DELAY = 40;
    private static final float TRAP_DAMAGE = 1.0F;
    private static final int DAMAGE_INTERVAL = 10;
    private static final float PITCH_BASE = 0.9F;
    private static final float PITCH_RANGE = 0.25F;

    public HoundTrapBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CLOSED, false).setValue(ARMED, true));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new HoundTrapBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED, ARMED);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return state.getValue(CLOSED) ? CLOSED_SHAPE : OPEN_SHAPE;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void entityInside(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive())
            return;

        if (living instanceof Player player && (player.isCreative() || player.isSpectator()))
            return;

        if (state.getValue(CLOSED)) {
            if (!isTrapped(level, pos, living))
                return;

            hold(living, level, pos);

            if (!level.isClientSide && level.getGameTime() % DAMAGE_INTERVAL == 0) {
                level.playSound(null, pos, SoundRegistry.HOUND_TRAP_DAMAGE.get(), SoundSource.BLOCKS, 1.0F, randomPitch(level));
                hurt(living, level);

                if (!living.isAlive())
                    open(level.getBlockState(pos), level, pos);
            }

            return;
        }

        if (level.isClientSide || !state.getValue(ARMED))
            return;

        level.setBlock(pos, state.setValue(CLOSED, true), 3);
        setTrapped(level, pos, living.getUUID());
        level.scheduleTick(pos, this, TRAP_DURATION);
        level.playSound(null, pos, SoundRegistry.HOUND_TRAP_CLOSED.get(), SoundSource.BLOCKS, 1.0F, randomPitch(level));
        hurt(living, level);

        if (!living.isAlive()) {
            open(level.getBlockState(pos), level, pos);
            return;
        }

        hold(living, level, pos);
    }

    private boolean isTrapped(Level level, BlockPos pos, LivingEntity living) {
        return level.getBlockEntity(pos) instanceof HoundTrapBlockEntity trap
                && living.getUUID().equals(trap.getTrapped());
    }

    private void setTrapped(Level level, BlockPos pos, @Nullable UUID trapped) {
        if (level.getBlockEntity(pos) instanceof HoundTrapBlockEntity trap)
            trap.setTrapped(trapped);
    }

    private float randomPitch(Level level) {
        return PITCH_BASE + level.getRandom().nextFloat() * PITCH_RANGE;
    }

    private void hurt(LivingEntity living, Level level) {
        living.invulnerableTime = 0;
        living.hurt(DamageSourceUtil.getDamageSource(level, ModDamageTypes.HOUND_TRAP), TRAP_DAMAGE);
    }

    private void hold(LivingEntity living, Level level, BlockPos pos) {
        if (living.onGround()) {
            living.makeStuckInBlock(level.getBlockState(pos), HOLD_MULTIPLIER);
            return;
        }

        Vec3 motion = living.getDeltaMovement();
        living.setDeltaMovement(0.0D, Math.min(motion.y, 0.0D), 0.0D);
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!state.getValue(CLOSED))
            return InteractionResult.PASS;

        if (!level.isClientSide)
            open(state, level, pos);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void tick(BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (state.getValue(CLOSED)) {
            open(state, level, pos);
            return;
        }

        if (!state.getValue(ARMED))
            level.setBlock(pos, state.setValue(ARMED, true), 3);
    }

    private void open(BlockState state, Level level, BlockPos pos) {
        level.setBlock(pos, state.setValue(CLOSED, false).setValue(ARMED, false), 3);
        setTrapped(level, pos, null);
        level.scheduleTick(pos, this, REARM_DELAY);
        level.playSound(null, pos, SoundRegistry.HOUND_TRAP_OPENED.get(), SoundSource.BLOCKS, 1.0F, randomPitch(level));
    }
}

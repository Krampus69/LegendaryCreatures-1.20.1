package sfiomn.legendarycreatures.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import sfiomn.legendarycreatures.entities.FertileSlimeballEntity;

import java.util.List;
import java.util.Optional;

public class FertileSlimeballItem extends Item {

    public static final int PLANT_POWER = 5;
    public static final int GROUND_RADIUS = 7;
    public static final int GROUND_ATTEMPTS = 512;
    public static final int GROUND_PARTICLES = 60;

    public FertileSlimeballItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.legendarycreatures.fertile_slimeball")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x3ebb49))));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();

        if (!canFertilize(level, pos, face)) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            fertilize(serverLevel, pos, face);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            FertileSlimeballEntity projectile = new FertileSlimeballEntity(level, player);
            projectile.setItem(stack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(projectile);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static boolean canFertilize(Level level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BonemealableBlock bonemealable) {
            if (bonemealable.isValidBonemealTarget(level, pos, state, level.isClientSide)) {
                return true;
            }
        }
        if (face != Direction.DOWN) {
            BlockPos adjacent = pos.relative(face);
            return level.getFluidState(adjacent).is(FluidTags.WATER) && level.getFluidState(adjacent).isSource();
        }
        return false;
    }

    public static boolean fertilize(ServerLevel level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        RandomSource random = level.getRandom();

        if (state.getBlock() instanceof GrassBlock) {
            fertilizeGround(level, pos, random);
            return true;
        }

        if (state.getBlock() instanceof BonemealableBlock) {
            boolean grew = false;
            for (int i = 0; i < PLANT_POWER; i++) {
                state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) break;
                if (!bonemealable.isValidBonemealTarget(level, pos, state, false)) break;
                if (bonemealable.isBonemealSuccess(level, random, pos, state)) {
                    bonemealable.performBonemeal(level, random, pos, state);
                    grew = true;
                }
            }
            if (grew) {
                level.levelEvent(1505, pos, 0);
                return true;
            }
        }

        if (face != Direction.DOWN) {
            BlockPos adjacent = pos.relative(face);
            boolean grew = false;
            for (int i = 0; i < PLANT_POWER; i++) {
                if (BoneMealItem.growWaterPlant(new ItemStack(Items.BONE_MEAL), level, adjacent, face)) {
                    grew = true;
                }
            }
            if (grew) {
                level.levelEvent(1505, adjacent, 0);
                return true;
            }
        }

        return false;
    }

    private static void fertilizeGround(ServerLevel level, BlockPos pos, RandomSource random) {
        Optional<Holder.Reference<PlacedFeature>> grassFeature = level.registryAccess()
                .registryOrThrow(Registries.PLACED_FEATURE).getHolder(VegetationPlacements.GRASS_BONEMEAL);
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();

        for (int i = 0; i < GROUND_ATTEMPTS; i++) {
            int dx = random.nextInt(GROUND_RADIUS * 2 + 1) - GROUND_RADIUS;
            int dz = random.nextInt(GROUND_RADIUS * 2 + 1) - GROUND_RADIUS;
            if (dx * dx + dz * dz > GROUND_RADIUS * GROUND_RADIUS) continue;
            int dy = random.nextInt(5) - 2;
            target.set(pos.getX() + dx, pos.getY() + 1 + dy, pos.getZ() + dz);

            if (!(level.getBlockState(target.below()).getBlock() instanceof GrassBlock)) continue;
            BlockState existing = level.getBlockState(target);

            if (existing.is(Blocks.GRASS) && random.nextInt(10) == 0) {
                ((BonemealableBlock) Blocks.GRASS).performBonemeal(level, random, target.immutable(), existing);
                continue;
            }
            if (!existing.isAir()) continue;

            Holder<PlacedFeature> feature;
            if (random.nextInt(8) == 0) {
                List<ConfiguredFeature<?, ?>> flowers = level.getBiome(target).value().getGenerationSettings().getFlowerFeatures();
                if (flowers.isEmpty()) continue;
                feature = ((RandomPatchConfiguration) flowers.get(0).config()).feature();
            } else {
                if (grassFeature.isEmpty()) continue;
                feature = grassFeature.get();
            }
            feature.value().place(level, level.getChunkSource().getGenerator(), random, target.immutable());
        }

        level.levelEvent(1505, pos, 0);
        for (int i = 0; i < GROUND_PARTICLES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * GROUND_RADIUS;
            double x = pos.getX() + 0.5 + Math.cos(angle) * dist;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * dist;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (Math.abs(surfaceY - (pos.getY() + 1)) > 3) continue;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, surfaceY + 0.3 + random.nextDouble() * 0.5, z, 1, 0, 0, 0, 0);
        }
    }
}

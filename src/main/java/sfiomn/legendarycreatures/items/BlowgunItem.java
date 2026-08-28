package sfiomn.legendarycreatures.items;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import sfiomn.legendarycreatures.entities.JinxDartEntity;
import sfiomn.legendarycreatures.registry.SoundRegistry;

/**
 * The Jinx's blowgun. Right-click fires a jinx dart (no ammunition needed), costing 1 durability
 * per shot with a short cooldown. Repairable with bamboo in an anvil, and accepts Unbreaking
 * (the vanilla durability enchantment) via enchanting table or book.
 */
public class BlowgunItem extends Item {

    private static final int COOLDOWN_TICKS = 20;      // 1s between shots
    private static final float DART_SPEED = 3.2F;      // same as the Jinx's own darts
    private static final float DART_INACCURACY = 1.0F; // tighter than the mob's 6.0 spread

    public BlowgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.legendarycreatures.blowgun_repair")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x3ebb49))));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            JinxDartEntity dart = new JinxDartEntity(level, player);
            dart.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, DART_SPEED, DART_INACCURACY);
            level.addFreshEntity(dart);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.JINX_SHOOT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        // CONSUME (not SUCCESS) so the client never plays the arm-swing on firing.
        return InteractionResultHolder.consume(stack);
    }

    /** Durability ticking down each shot would otherwise play the re-equip dip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.BAMBOO);
    }

    @Override
    public int getEnchantmentValue() {
        return 10; // enchantable at a table; Unbreaking applies to any damageable item
    }
}

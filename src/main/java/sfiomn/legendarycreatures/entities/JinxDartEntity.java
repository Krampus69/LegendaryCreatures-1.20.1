package sfiomn.legendarycreatures.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.EntityHitResult;
import sfiomn.legendarycreatures.registry.EntityTypeRegistry;

public class JinxDartEntity extends AbstractArrow {

    private static final float DAMAGE = 4.0F; // 2 hearts, fixed

    /** Entities in this tag take dart damage but are immune to its levitation
     *  (data/legendarycreatures/tags/entity_types/dart_levitation_immune.json). */
    private static final TagKey<EntityType<?>> LEVITATION_IMMUNE = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("legendarycreatures", "dart_levitation_immune"));

    public JinxDartEntity(EntityType<? extends JinxDartEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public JinxDartEntity(Level level, LivingEntity shooter) {
        super(EntityTypeRegistry.JINX_DART.get(), shooter, level);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().arrow(this, owner != null ? owner : this);

        // If a shield blocks it, hurt() deals no damage -> no levitation, no laugh.
        boolean blocked = target instanceof LivingEntity le && le.isDamageSourceBlocked(source);

        if (target.hurt(source, DAMAGE) && !blocked && target instanceof LivingEntity living) {
            if (!living.getType().is(LEVITATION_IMMUNE)) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 4), owner); // Levitation V, 3s
            }
            // Legendary creatures are normally fall-immune; a dart touch suspends that (10s window),
            // so lifting one with levitation and letting it drop is lethal.
            if (living instanceof AnimatedCreatureEntity creature) {
                creature.markDartFallVulnerable(200);
            }
            if (owner instanceof JinxEntity jinx) {
                jinx.triggerLaugh();
            }
        } else if (owner instanceof JinxEntity jinx) {
            // Blocked by a shield or otherwise failed to damage: that's a miss, streak resets.
            jinx.resetDartStreak();
        }

        this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F);
        this.discard();
    }

    @Override
    protected void onHitBlock(net.minecraft.world.phys.BlockHitResult result) {
        super.onHitBlock(result);   // lodge in the wall like a vanilla arrow
        if (this.getOwner() instanceof JinxEntity jinx) {
            jinx.resetDartStreak(); // hit scenery, not the target: streak resets
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(Items.ARROW);
    }
}

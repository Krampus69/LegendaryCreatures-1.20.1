package sfiomn.legendarycreatures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import sfiomn.legendarycreatures.entities.AnimatedCreatureEntity;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import sfiomn.legendarycreatures.LegendaryCreatures;

import java.util.HashSet;
import java.util.Set;

public class SummonRarityCommand {

    private static final SimpleCommandExceptionType ERROR_NOT_CREATURE =
            new SimpleCommandExceptionType(Component.literal("That entity type is not a Legendary Creature with rarity tiers"));
    private static final SimpleCommandExceptionType ERROR_FAILED =
            new SimpleCommandExceptionType(Component.literal("Failed to spawn the creature"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                net.minecraft.commands.CommandBuildContext context) {
        dispatcher.register(
                Commands.literal("lcsummon")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                                .suggests(RARITY_ENTITY_SUGGESTIONS)
                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> spawn(
                                                ctx.getSource(),
                                                ResourceArgument.getEntityType(ctx, "entity"),
                                                IntegerArgumentType.getInteger(ctx, "tier"))))));
    }

    private static volatile Set<ResourceLocation> RARITY_SUPPORTED_CACHE = null;

    private static final SuggestionProvider<CommandSourceStack> RARITY_ENTITY_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    getRaritySupportedTypes(ctx.getSource().getLevel()).stream(),
                    builder);

    private static Set<ResourceLocation> getRaritySupportedTypes(ServerLevel level) {
        Set<ResourceLocation> cache = RARITY_SUPPORTED_CACHE;
        if (cache != null) return cache;

        Set<ResourceLocation> result = new HashSet<>();
        for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            // Limit to our mod's entities — no need to probe-create vanilla/other mods' entities.
            if (!id.getNamespace().equals(LegendaryCreatures.MOD_ID)) continue;

            Entity probe = entry.getValue().create(level);
            if (probe instanceof AnimatedCreatureEntity creature
                    && (creature.getLevel2SpawnChance() > 0 || creature.getLevel3SpawnChance() > 0)) {
                result.add(id);
            }
            if (probe != null) probe.discard();
        }
        RARITY_SUPPORTED_CACHE = result;
        return result;
    }

    private static int spawn(CommandSourceStack source,
                             Holder.Reference<EntityType<?>> typeHolder,
                             int tier) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        EntityType<?> type = typeHolder.value();
        Vec3 pos = source.getPosition();

        Entity entity = type.create(level);
        if (entity == null) throw ERROR_FAILED.create();

        // Position before finalizeSpawn — some mob logic reads position there.
        entity.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, source.getRotation().x);

        // Force the desired tier BEFORE finalizeSpawn runs. The parent's
        // rollRarityVariant() will see the variant is already set and... actually,
        // no — it always rolls. So we set the variant AFTER finalizeSpawn returns,
        // then re-apply rarity modifiers explicitly.
        if (!(entity instanceof AnimatedCreatureEntity creature)) {
            entity.discard();
            throw ERROR_NOT_CREATURE.create();
        }

        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.COMMAND, null, null);
        }

        // Override the tier the parent rolled.
        switch (tier) {
            case 1 -> creature.setVariant(0);
            case 2 -> creature.setVariant(creature.getLevel2Variant());
            case 3 -> creature.setVariant(creature.getLevel3Variant());
        }
        // Re-apply modifiers for the forced tier (idempotent thanks to applyMod).
        creature.applyRarityModifiers();

        if (!level.addFreshEntity(entity)) throw ERROR_FAILED.create();

        source.sendSuccess(() -> Component.literal(
                "Spawned " + type.getDescription().getString() + " (Level " + tier + ")"), true);
        return 1;
    }
}
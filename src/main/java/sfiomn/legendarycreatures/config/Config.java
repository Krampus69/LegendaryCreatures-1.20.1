package sfiomn.legendarycreatures.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import sfiomn.legendarycreatures.LegendaryCreatures;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class Config
{
	public static final ForgeConfigSpec COMMON_SPEC;
	public static final Common COMMON;

	static
	{
		final Pair<Common, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = common.getRight();
		COMMON = common.getLeft();
	}

	public static void register()
	{
		try
		{
			Files.createDirectory(LegendaryCreatures.modConfigPath);
			Files.createDirectory(LegendaryCreatures.modConfigJson);
		}
		catch (FileAlreadyExistsException ignored) {}
		catch (IOException e)
		{
			LegendaryCreatures.LOGGER.error("Failed to create Legendary Creatures config directories");
			e.printStackTrace();
		}

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, LegendaryCreatures.MOD_ID + "/" + LegendaryCreatures.MOD_ID + "-common.toml");
	}

	public static class Common
	{
		public final ForgeConfigSpec.BooleanValue desertMojoBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue desertMojoKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue forestMojoBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue forestMojoKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue houndBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue houndKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue scarecrowBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue scarecrowKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue scorpionBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue scorpionKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue scorpionBabyBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue scorpionBabyKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue wispBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue wispKillingEntitySpawn;
		public final ForgeConfigSpec.ConfigValue<List<Integer>> wispPurseXpReward;
		public final ForgeConfigSpec.BooleanValue netherWispBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue netherWispKillingEntitySpawn;
		public final ForgeConfigSpec.ConfigValue<List<Integer>> netherWispPurseXpReward;
		public final ForgeConfigSpec.BooleanValue enderWispBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue enderWispKillingEntitySpawn;
		public final ForgeConfigSpec.ConfigValue<List<Integer>> enderWispPurseXpReward;
		public final ForgeConfigSpec.BooleanValue corpseEaterBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue corpseEaterKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue peacockSpiderBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue peacockSpiderKillingEntitySpawn;
		public final ForgeConfigSpec.BooleanValue bullfrogBreakingBlockSpawn;
		public final ForgeConfigSpec.BooleanValue bullfrogKillingEntitySpawn;

		// Natural spawning (biome spawn list)
		public final ForgeConfigSpec.BooleanValue desertMojoNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue forestMojoNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue houndNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue scarecrowNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue scorpionNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue scorpionBabyNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue corpseEaterNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue peacockSpiderNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue bullfrogNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue wispNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue netherWispNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue enderWispNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue butterflyNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue dragonflyNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue ladybugNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue scarabNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue mantisNaturalSpawn;
		public final ForgeConfigSpec.BooleanValue hermitCrabNaturalSpawn;

		Common(ForgeConfigSpec.Builder builder)
		{
			builder.comment(" Options related to mob spawning").push("MobSpawning");

			builder.push("desert_mojo");
			desertMojoBreakingBlockSpawn = builder.comment("When true, allows the Desert Mojo to spawn when a player breaks a block listed in config/legendarycreatures/json/desert_mojo-spawn.json").define("Desert Mojo Breaking Block Spawn ", true);
			desertMojoKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/desert_mojo-spawn.json has a chance to spawn a Desert Mojo at that location").define("Desert Mojo Killing Entity Spawn ", true);
			desertMojoNaturalSpawn = builder.comment("Enables Desert Mojo natural spawning in the world").define("Desert Mojo Natural Spawn", true);
			builder.pop();

			builder.push("forest_mojo");
			forestMojoBreakingBlockSpawn = builder.comment("When true, allows the Forest Mojo to spawn when a player breaks a block listed in config/legendarycreatures/json/forest_mojo-spawn.json").define("Forest Mojo Breaking Block Spawn ", true);
			forestMojoKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/forest_mojo-spawn.json has a chance to spawn a Forest Mojo at that location").define("Forest Mojo Killing Entity Spawn ", true);
			forestMojoNaturalSpawn = builder.comment("Enables Forest Mojo natural spawning in the world").define("Forest Mojo Natural Spawn", true);
			builder.pop();

			builder.push("hound");
			houndBreakingBlockSpawn = builder.comment("When true, allows the Hound to spawn when a player breaks a block listed in config/legendarycreatures/json/hound-spawn.json").define("Hound Breaking Block Spawn ", true);
			houndKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/hound-spawn.json has a chance to spawn a Hound at that location").define("Hound Killing Entity Spawn ", true);
			houndNaturalSpawn = builder.comment("Enables Hound natural spawning in the world").define("Hound Natural Spawn", true);
			builder.pop();

			builder.push("scarecrow");
			scarecrowBreakingBlockSpawn = builder.comment("When true, allows the Scarecrow to spawn when a player breaks a block listed in config/legendarycreatures/json/scarecrow-spawn.json").define("Scarecrow Breaking Block Spawn ", true);
			scarecrowKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/scarecrow-spawn.json has a chance to spawn a Scarecrow at that location").define("Scarecrow Killing Entity Spawn ", true);
			scarecrowNaturalSpawn = builder.comment("Enables Scarecrow natural spawning in the world").define("Scarecrow Natural Spawn", true);
			builder.pop();

			builder.push("scorpion");
			scorpionBreakingBlockSpawn = builder.comment("When true, allows the Scorpion to spawn when a player breaks a block listed in config/legendarycreatures/json/scorpion-spawn.json").define("Scorpion Breaking Block Spawn ", true);
			scorpionKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/scorpion-spawn.json has a chance to spawn a Scorpion at that location").define("Scorpion Killing Entity Spawn ", true);
			scorpionNaturalSpawn = builder.comment("Enables Scorpion natural spawning in the world").define("Scorpion Natural Spawn", true);
			builder.pop();

			builder.push("scorpion_baby");
			scorpionBabyBreakingBlockSpawn = builder.comment("When true, allows the Baby Scorpion to spawn when a player breaks a block listed in config/legendarycreatures/json/scorpion_baby-spawn.json").define("Baby Scorpion Breaking Block Spawn ", true);
			scorpionBabyKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/scorpion_baby-spawn.json has a chance to spawn a Baby Scorpion at that location").define("Baby Scorpion Killing Entity Spawn ", true);
			scorpionBabyNaturalSpawn = builder.comment("Enables Baby Scorpion natural spawning in the world").define("Baby Scorpion Natural Spawn", true);
			builder.pop();

			builder.push("wisp");
			wispBreakingBlockSpawn = builder.comment("When true, allows the Wisp to spawn when a player breaks a block listed in config/legendarycreatures/json/wisp-spawn.json").define("Wisp Breaking Block Spawn ", true);
			wispKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/wisp-spawn.json has a chance to spawn a Wisp at that location").define("Wisp Killing Entity Spawn ", true);
			wispNaturalSpawn = builder.comment("Enables Wisp natural spawning in the world").define("Wisp Natural Spawn", true);
			wispPurseXpReward = builder.define("Wisp Purse Xp Reward Range ", Arrays.asList(30, 30));
			builder.pop();

			builder.push("nether_wisp");
			netherWispBreakingBlockSpawn = builder.comment("When true, allows the Nether Wisp to spawn when a player breaks a block listed in config/legendarycreatures/json/nether_wisp-spawn.json").define("Nether Wisp Breaking Block Spawn ", true);
			netherWispKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/nether_wisp-spawn.json has a chance to spawn a Nether Wisp at that location").define("Nether Wisp Killing Entity Spawn ", true);
			netherWispNaturalSpawn = builder.comment("Enables Nether Wisp natural spawning in the world").define("Nether Wisp Natural Spawn", true);
			netherWispPurseXpReward = builder.define("Nether Wisp Purse Xp Reward Range ", Arrays.asList(80, 80));
			builder.pop();

			builder.push("ender_wisp");
			enderWispBreakingBlockSpawn = builder.comment("When true, allows the Ender Wisp to spawn when a player breaks a block listed in config/legendarycreatures/json/ender_wisp-spawn.json").define("Ender Wisp Breaking Block Spawn ", true);
			enderWispKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/ender_wisp-spawn.json has a chance to spawn an Ender Wisp at that location").define("Ender Wisp Killing Entity Spawn ", true);
			enderWispNaturalSpawn = builder.comment("Enables Ender Wisp natural spawning in the world").define("Ender Wisp Natural Spawn", true);
			enderWispPurseXpReward = builder.define("Ender Wisp Purse Xp Reward Range ", Arrays.asList(240, 240));
			builder.pop();

			builder.push("corpse_eater");
			corpseEaterBreakingBlockSpawn = builder.comment("When true, allows the Corpse Eater to spawn when a player breaks a block listed in config/legendarycreatures/json/corpse_eater-spawn.json").define("Corpse Eater Breaking Block Spawn ", true);
			corpseEaterKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/corpse_eater-spawn.json has a chance to spawn a Corpse Eater at that location").define("Corpse Eater Killing Entity Spawn ", true);
			corpseEaterNaturalSpawn = builder.comment("Enables Corpse Eater natural spawning in the world").define("Corpse Eater Natural Spawn", true);
			builder.pop();

			builder.push("peacock_spider");
			peacockSpiderBreakingBlockSpawn = builder.comment("When true, allows the Peacock Spider to spawn when a player breaks a block listed in config/legendarycreatures/json/peacock_spider-spawn.json").define("Peacock Spider Breaking Block Spawn ", true);
			peacockSpiderKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/peacock_spider-spawn.json has a chance to spawn a Peacock Spider at that location").define("Peacock Spider Killing Entity Spawn ", true);
			peacockSpiderNaturalSpawn = builder.comment("Enables Peacock Spider natural spawning in the world").define("Peacock Spider Natural Spawn", true);
			builder.pop();

			builder.push("bullfrog");
			bullfrogBreakingBlockSpawn = builder.comment("When true, allows the Bullfrog to spawn when a player breaks a block listed in config/legendarycreatures/json/bullfrog-spawn.json").define("Bullfrog Breaking Block Spawn ", true);
			bullfrogKillingEntitySpawn = builder.comment("When true, killing a mob listed in config/legendarycreatures/json/bullfrog-spawn.json has a chance to spawn a Bullfrog at that location").define("Bullfrog Killing Entity Spawn ", true);
			bullfrogNaturalSpawn = builder.comment("Enables Bullfrog natural spawning in the world").define("Bullfrog Natural Spawn", true);
			builder.pop();

			builder.comment(" WARNING: Disabling an insect that is paired with a hostile mob will give that hostile mob a 100% chance of filling spawn slots").push("insects");

			butterflyNaturalSpawn = builder.comment("Enables Butterfly natural spawning. Paired with: Forest Mojo, Hound").define("Butterfly Natural Spawn", true);
			dragonflyNaturalSpawn = builder.comment("Enables Dragonfly natural spawning. Paired with: Bullfrog").define("Dragonfly Natural Spawn", true);
			ladybugNaturalSpawn = builder.comment("Enables Ladybug natural spawning. Paired with: Scarecrow, Wisp").define("Ladybug Natural Spawn", true);
			scarabNaturalSpawn = builder.comment("Enables Scarab natural spawning. Paired with: Desert Mojo, Scorpion, Baby Scorpion, Corpse Eater").define("Scarab Natural Spawn", true);
			mantisNaturalSpawn = builder.comment("Enables Mantis natural spawning. Paired with: Peacock Spider").define("Mantis Natural Spawn", true);
			hermitCrabNaturalSpawn = builder.comment("Enables Hermit Crab natural spawning. Paired with: Peacock Spider").define("Hermit Crab Natural Spawn", true);

			builder.pop();

			builder.pop();
		}
	}

	public static class Baked
	{
		public static boolean desertMojoBreakingBlockSpawn;
		public static boolean desertMojoKillingEntitySpawn;
		public static boolean forestMojoBreakingBlockSpawn;
		public static boolean forestMojoKillingEntitySpawn;
		public static boolean houndBreakingBlockSpawn;
		public static boolean houndKillingEntitySpawn;
		public static boolean scarecrowBreakingBlockSpawn;
		public static boolean scarecrowKillingEntitySpawn;
		public static boolean scorpionBreakingBlockSpawn;
		public static boolean scorpionKillingEntitySpawn;
		public static boolean scorpionBabyBreakingBlockSpawn;
		public static boolean scorpionBabyKillingEntitySpawn;
		public static boolean wispBreakingBlockSpawn;
		public static boolean wispKillingEntitySpawn;
		public static List<Integer> wispPurseXpReward;
		public static boolean netherWispBreakingBlockSpawn;
		public static boolean netherWispKillingEntitySpawn;
		public static List<Integer> netherWispPurseXpReward;
		public static boolean enderWispBreakingBlockSpawn;
		public static boolean enderWispKillingEntitySpawn;
		public static List<Integer> enderWispPurseXpReward;
		public static boolean corpseEaterBreakingBlockSpawn;
		public static boolean corpseEaterKillingEntitySpawn;
		public static boolean peacockSpiderBreakingBlockSpawn;
		public static boolean peacockSpiderKillingEntitySpawn;
		public static boolean bullfrogBreakingBlockSpawn;
		public static boolean bullfrogKillingEntitySpawn;

		// Natural spawning
		public static boolean desertMojoNaturalSpawn;
		public static boolean forestMojoNaturalSpawn;
		public static boolean houndNaturalSpawn;
		public static boolean scarecrowNaturalSpawn;
		public static boolean scorpionNaturalSpawn;
		public static boolean scorpionBabyNaturalSpawn;
		public static boolean corpseEaterNaturalSpawn;
		public static boolean peacockSpiderNaturalSpawn;
		public static boolean bullfrogNaturalSpawn;
		public static boolean wispNaturalSpawn;
		public static boolean netherWispNaturalSpawn;
		public static boolean enderWispNaturalSpawn;
		public static boolean butterflyNaturalSpawn;
		public static boolean dragonflyNaturalSpawn;
		public static boolean ladybugNaturalSpawn;
		public static boolean scarabNaturalSpawn;
		public static boolean mantisNaturalSpawn;
		public static boolean hermitCrabNaturalSpawn;

		public static void bakeCommon()
		{
			try
			{
				desertMojoBreakingBlockSpawn = COMMON.desertMojoBreakingBlockSpawn.get();
				desertMojoKillingEntitySpawn = COMMON.desertMojoKillingEntitySpawn.get();
				forestMojoBreakingBlockSpawn = COMMON.forestMojoBreakingBlockSpawn.get();
				forestMojoKillingEntitySpawn = COMMON.forestMojoKillingEntitySpawn.get();
				houndBreakingBlockSpawn = COMMON.houndBreakingBlockSpawn.get();
				houndKillingEntitySpawn = COMMON.houndKillingEntitySpawn.get();
				scarecrowBreakingBlockSpawn = COMMON.scarecrowBreakingBlockSpawn.get();
				scarecrowKillingEntitySpawn = COMMON.scarecrowKillingEntitySpawn.get();
				scorpionBreakingBlockSpawn = COMMON.scorpionBreakingBlockSpawn.get();
				scorpionKillingEntitySpawn = COMMON.scorpionKillingEntitySpawn.get();
				scorpionBabyBreakingBlockSpawn = COMMON.scorpionBabyBreakingBlockSpawn.get();
				scorpionBabyKillingEntitySpawn = COMMON.scorpionBabyKillingEntitySpawn.get();
				wispBreakingBlockSpawn = COMMON.wispBreakingBlockSpawn.get();
				wispKillingEntitySpawn = COMMON.wispKillingEntitySpawn.get();
				wispPurseXpReward = COMMON.wispPurseXpReward.get();
				netherWispBreakingBlockSpawn = COMMON.netherWispBreakingBlockSpawn.get();
				netherWispKillingEntitySpawn = COMMON.netherWispKillingEntitySpawn.get();
				netherWispPurseXpReward = COMMON.netherWispPurseXpReward.get();
				enderWispBreakingBlockSpawn = COMMON.enderWispBreakingBlockSpawn.get();
				enderWispKillingEntitySpawn = COMMON.enderWispKillingEntitySpawn.get();
				enderWispPurseXpReward = COMMON.enderWispPurseXpReward.get();
				corpseEaterBreakingBlockSpawn = COMMON.corpseEaterBreakingBlockSpawn.get();
				corpseEaterKillingEntitySpawn = COMMON.corpseEaterKillingEntitySpawn.get();
				peacockSpiderBreakingBlockSpawn = COMMON.peacockSpiderBreakingBlockSpawn.get();
				peacockSpiderKillingEntitySpawn = COMMON.peacockSpiderKillingEntitySpawn.get();
				bullfrogBreakingBlockSpawn = COMMON.bullfrogBreakingBlockSpawn.get();
				bullfrogKillingEntitySpawn = COMMON.bullfrogKillingEntitySpawn.get();

				desertMojoNaturalSpawn = COMMON.desertMojoNaturalSpawn.get();
				forestMojoNaturalSpawn = COMMON.forestMojoNaturalSpawn.get();
				houndNaturalSpawn = COMMON.houndNaturalSpawn.get();
				scarecrowNaturalSpawn = COMMON.scarecrowNaturalSpawn.get();
				scorpionNaturalSpawn = COMMON.scorpionNaturalSpawn.get();
				scorpionBabyNaturalSpawn = COMMON.scorpionBabyNaturalSpawn.get();
				corpseEaterNaturalSpawn = COMMON.corpseEaterNaturalSpawn.get();
				peacockSpiderNaturalSpawn = COMMON.peacockSpiderNaturalSpawn.get();
				bullfrogNaturalSpawn = COMMON.bullfrogNaturalSpawn.get();
				wispNaturalSpawn = COMMON.wispNaturalSpawn.get();
				netherWispNaturalSpawn = COMMON.netherWispNaturalSpawn.get();
				enderWispNaturalSpawn = COMMON.enderWispNaturalSpawn.get();
				butterflyNaturalSpawn = COMMON.butterflyNaturalSpawn.get();
				dragonflyNaturalSpawn = COMMON.dragonflyNaturalSpawn.get();
				ladybugNaturalSpawn = COMMON.ladybugNaturalSpawn.get();
				scarabNaturalSpawn = COMMON.scarabNaturalSpawn.get();
				mantisNaturalSpawn = COMMON.mantisNaturalSpawn.get();
				hermitCrabNaturalSpawn = COMMON.hermitCrabNaturalSpawn.get();
			}
			catch (Exception e)
			{
				LegendaryCreatures.LOGGER.warn("An exception was caused trying to load the common config for Survival Overhaul");
				e.printStackTrace();
			}
		}
	}
}
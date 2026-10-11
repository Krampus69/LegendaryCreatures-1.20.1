package sfiomn.legendarycreatures.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import sfiomn.legendarycreatures.LegendaryCreatures;

public class PlayerRules {

    private static final String ROOT_KEY = LegendaryCreatures.MOD_ID + ":rules";

    public enum Rule {
        DO_MOJOS_SPAWN("domojosspawn", true);

        public final String name;
        public final boolean defaultValue;

        Rule(String name, boolean defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public static Rule byName(String name) {
            for (Rule rule : values()) {
                if (rule.name.equalsIgnoreCase(name))
                    return rule;
            }
            return null;
        }
    }

    private static CompoundTag getRulesTag(Player player, boolean create) {
        CompoundTag persisted = player.getPersistentData();
        if (!persisted.contains(Player.PERSISTED_NBT_TAG)) {
            if (!create) return null;
            persisted.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag forgeData = persisted.getCompound(Player.PERSISTED_NBT_TAG);
        if (!forgeData.contains(ROOT_KEY)) {
            if (!create) return null;
            forgeData.put(ROOT_KEY, new CompoundTag());
        }
        return forgeData.getCompound(ROOT_KEY);
    }

    public static boolean get(Player player, Rule rule) {
        CompoundTag rules = getRulesTag(player, false);
        if (rules == null || !rules.contains(rule.name))
            return rule.defaultValue;
        return rules.getBoolean(rule.name);
    }

    public static void set(Player player, Rule rule, boolean value) {
        getRulesTag(player, true).putBoolean(rule.name, value);
    }
}

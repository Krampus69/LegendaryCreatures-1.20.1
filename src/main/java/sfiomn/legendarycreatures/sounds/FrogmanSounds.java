package sfiomn.legendarycreatures.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import sfiomn.legendarycreatures.entities.FrogmanEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class FrogmanSounds {

    private static final Map<FrogmanEntity, FrogmanSleepSound> SLEEP_SOUNDS = new WeakHashMap<>();

    public static void startSleepSound(FrogmanEntity frogman) {
        FrogmanSleepSound current = SLEEP_SOUNDS.get(frogman);
        if (current != null && !current.isStopped())
            return;

        FrogmanSleepSound sound = new FrogmanSleepSound(frogman, 1.0F, 0.95F + frogman.level().getRandom().nextFloat() * 0.1F);
        SLEEP_SOUNDS.put(frogman, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public static void playFollowing(SoundEvent soundEvent, Entity entity, float volume, float minPitch, float maxPitch) {
        float pitch = minPitch + entity.level().getRandom().nextFloat() * (maxPitch - minPitch);
        Minecraft.getInstance().getSoundManager()
                .play(new FollowingSound(soundEvent, entity, volume, pitch));
    }
}

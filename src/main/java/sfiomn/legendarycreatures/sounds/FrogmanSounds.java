package sfiomn.legendarycreatures.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

public class FrogmanSounds {

    public static void playFollowing(SoundEvent soundEvent, Entity entity, float volume, float minPitch, float maxPitch) {
        float pitch = minPitch + entity.level().getRandom().nextFloat() * (maxPitch - minPitch);
        Minecraft.getInstance().getSoundManager()
                .play(new FollowingSound(soundEvent, entity, volume, pitch));
    }
}

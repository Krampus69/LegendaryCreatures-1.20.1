package sfiomn.legendarycreatures.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A one-shot sound that tracks an entity as it moves, instead of staying at the position where it
 * was first played. Entity.playSound() anchors the sound to a fixed world position, so a mob that
 * runs away leaves its own sound behind.
 */
@OnlyIn(Dist.CLIENT)
public class FollowingSound extends AbstractTickableSoundInstance {

    private final Entity entity;

    public FollowingSound(SoundEvent soundEvent, Entity entity, float volume, float pitch) {
        super(soundEvent, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.looping = false;
        this.volume = volume;
        this.pitch = pitch;
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
    }

    @Override
    public void tick() {
        if (!this.entity.isAlive() || this.entity.isRemoved()) {
            this.stop();
            return;
        }
        this.x = this.entity.getX();
        this.y = this.entity.getY();
        this.z = this.entity.getZ();
    }
}

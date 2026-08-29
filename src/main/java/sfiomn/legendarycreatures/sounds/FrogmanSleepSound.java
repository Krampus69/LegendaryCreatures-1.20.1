package sfiomn.legendarycreatures.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import sfiomn.legendarycreatures.entities.FrogmanEntity;
import sfiomn.legendarycreatures.registry.SoundRegistry;

@OnlyIn(Dist.CLIENT)
public class FrogmanSleepSound extends AbstractTickableSoundInstance {

    private static final int FADE_OUT_TICKS = 6;

    private final FrogmanEntity frogman;
    private final float maxVolume;
    private int fadeOut = -1;

    public FrogmanSleepSound(FrogmanEntity frogman, float volume, float pitch) {
        super(SoundRegistry.FROGMAN_SLEEP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());

        this.frogman = frogman;
        this.maxVolume = volume;

        this.looping = true;
        this.delay = 0;
        this.volume = volume;
        this.pitch = pitch;
        this.x = frogman.getX();
        this.y = frogman.getY();
        this.z = frogman.getZ();
    }

    @Override
    public void tick() {
        if (!this.frogman.isAlive() || this.frogman.isRemoved()) {
            this.stop();
            return;
        }

        this.x = this.frogman.getX();
        this.y = this.frogman.getY();
        this.z = this.frogman.getZ();

        if (!this.frogman.isSleeping() && this.fadeOut < 0)
            this.fadeOut = FADE_OUT_TICKS;

        if (this.fadeOut >= 0) {
            this.volume = this.maxVolume * ((float) this.fadeOut / FADE_OUT_TICKS);
            if (this.fadeOut-- == 0)
                this.stop();
        }
    }
}

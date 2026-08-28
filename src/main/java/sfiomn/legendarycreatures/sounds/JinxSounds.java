package sfiomn.legendarycreatures.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import sfiomn.legendarycreatures.entities.JinxEntity;
import sfiomn.legendarycreatures.registry.SoundRegistry;
/**
 * Client-only entry point. Kept in its own class so the server never loads Minecraft client classes.
 */
public class JinxSounds {

    public static void playFollowing(SoundEvent soundEvent, Entity entity, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager()
                .play(new FollowingSound(soundEvent, entity, volume, pitch));
    }

    /**
     * Cuts every Jinx-emitted sound the instant it dies, so nothing overlaps the death cry.
     * Tickable sounds (fly loop, flee, umbrella) already stop via their isAlive checks; this
     * clears any one-shot (startle, laugh, idle, shoot, hurt, umbrella pop) still mid-play.
     * NOTE: SoundManager.stop is per-event, not per-entity — a nearby second Jinx mid-laugh
     * would be cut too. Rare enough in practice to accept.
     */
    public static void stopAllOnDeath() {
        var sm = Minecraft.getInstance().getSoundManager();
        var events = new SoundEvent[]{
                SoundRegistry.JINX_IDLE.get(), SoundRegistry.JINX_HURT.get(),
                SoundRegistry.JINX_FLEE.get(), SoundRegistry.JINX_STARTLE.get(),
                SoundRegistry.JINX_LAUGH.get(), SoundRegistry.JINX_SHOOT.get(),
                SoundRegistry.JINX_UMBRELLA.get(), SoundRegistry.JINX_FLY.get(),
                // deliberately NOT JINX_DEATH — that one must play out
        };
        for (SoundEvent event : events) {
            sm.stop(event.getLocation(), net.minecraft.sounds.SoundSource.HOSTILE);
        }
    }

    /**
     * Looping flight sound. StoppableSound loops, tracks the mob each tick, and stops itself
     * as soon as the condition flips — here, the moment the Jinx is no longer flying
     * (it lands or touches water, both of which clear the FLYING flag server-side).
     */
    public static void playFlyLoop(JinxEntity jinx) {
        Minecraft.getInstance().getSoundManager()
                .play(new StoppableSound(SoundRegistry.JINX_FLY.get(), jinx,
                        (mob) -> !((JinxEntity) mob).isFlying()));
    }
}

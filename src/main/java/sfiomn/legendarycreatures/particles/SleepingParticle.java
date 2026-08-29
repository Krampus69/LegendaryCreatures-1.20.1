package sfiomn.legendarycreatures.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SleepingParticle extends TextureSheetParticle {
    private final SpriteSet animatedSprite;
    private final float drift;
    private final float initialSize;
    private final float startX;
    private final float startZ;

    protected SleepingParticle(SpriteSet animatedSprite, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);

        this.animatedSprite = animatedSprite;
        this.setSpriteFromAge(animatedSprite);

        this.lifetime = 50 + this.random.nextInt(15);
        this.initialSize = 0.107F + (this.random.nextFloat() * 0.027F);
        this.quadSize = this.initialSize;

        this.drift = (this.random.nextFloat() * 0.6F) + 0.7F;
        this.startX = (float) x;
        this.startZ = (float) z;

        this.xd = xd;
        this.yd = yd + 0.019D;
        this.zd = zd;

        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;

        this.gravity = 0.0F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = (float) this.age / this.lifetime;

        this.y += this.yd;
        this.x = this.startX + (Math.sin(this.age * 0.12F * this.drift) * 0.11D);
        this.z = this.startZ + (Math.cos(this.age * 0.09F * this.drift) * 0.11D);

        this.quadSize = this.initialSize * (1.0F + (progress * 0.6F));

        if (progress < 0.65F)
            this.alpha = Math.min(1.0F, progress * 6.0F);
        else
            this.alpha = 1.0F - ((progress - 0.65F) / 0.35F);

        this.setSpriteFromAge(this.animatedSprite);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet animatedSprite;

        public Factory(SpriteSet animatedSprite) {
            this.animatedSprite = animatedSprite;
        }

        @Nullable
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new SleepingParticle(this.animatedSprite, level, x, y, z, xd, yd, zd);
        }
    }
}

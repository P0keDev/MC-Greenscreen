package dev.p0ke.greenscreen;

import net.minecraft.ChatFormatting;

import java.awt.*;

public class Greenscreen {
    private BooleanRenderState enabled;

    private BooleanRenderState blocks;
    private BooleanRenderState particles;
    private EntityRenderState entities;
    private BooleanRenderState armorStands;
    private EntityRenderState nameTags;

    private BooleanRenderState customSky;
    private Color skyColor;

    public Greenscreen() {
        enabled = BooleanRenderState.DISABLED;

        blocks = BooleanRenderState.DISABLED;
        particles = BooleanRenderState.DISABLED;
        entities = EntityRenderState.SELF;
        armorStands = BooleanRenderState.DISABLED;
        nameTags = EntityRenderState.SELF;

        customSky = BooleanRenderState.ENABLED;
        skyColor = new Color(0, 255, 0);
    }

    public BooleanRenderState state() {
        return enabled;
    }

    public BooleanRenderState blockRenderState() {
        return blocks;
    }

    public BooleanRenderState particleRenderState() {
        return particles;
    }

    public EntityRenderState entityRenderState() {
        return entities;
    }

    public BooleanRenderState armorStandRenderState() {
        return armorStands;
    }

    public EntityRenderState nameTageRenderState() {
        return nameTags;
    }

    public BooleanRenderState customSkyRenderState() {
        return customSky;
    }

    public Color getSkyColor() {
        return skyColor;
    }

    public BooleanRenderState toggleEnabled() {
        enabled = enabled.toggled();
        return enabled;
    }

    public void setEnabled(BooleanRenderState enabled) {
        this.enabled = enabled;
    }

    public void setBlockRending(BooleanRenderState blocks) {
        this.blocks = blocks;
    }

    public void setParticleRendering(BooleanRenderState particles) {
        this.particles = particles;
    }

    public void setEntityRendering(EntityRenderState entities) {
        this.entities = entities;
    }

    public void setArmorStandRendering(BooleanRenderState armorStands) {
        this.armorStands = armorStands;
    }

    public void setNameTagRendering(EntityRenderState nameTags) {
        this.nameTags = nameTags;
    }

    public void setCustomSkyRendering(BooleanRenderState customSky) {
        this.customSky = customSky;
    }

    public BooleanRenderState toggleBlockRendering() {
        blocks = blocks.toggled();
        return blocks;
    }

    public BooleanRenderState toggleParticleRendering() {
        particles = particles.toggled();
        return particles;
    }

    public EntityRenderState cycleEntityRendering() {
        entities = entities.next();
        return entities;
    }

    public BooleanRenderState toggleArmorStandRendering() {
        armorStands = armorStands.toggled();
        return armorStands;
    }

    public EntityRenderState cycleNameTagRendering() {
        nameTags = nameTags.next();
        return nameTags;
    }

    public BooleanRenderState toggleCustomSkyRendering() {
        customSky = customSky.toggled();
        return customSky;
    }

    public boolean setSkyColor(int r, int g, int b) {
        if (r > 255 || g > 255 || b > 255 ||
            r < 0 || g < 0 || b < 0) return false;
        skyColor = new Color(r, g, b);
        return true;
    }

    public enum BooleanRenderState {
        ENABLED(ChatFormatting.GREEN),
        DISABLED(ChatFormatting.RED);

        private ChatFormatting cf;

        BooleanRenderState(ChatFormatting cf) {
            this.cf = cf;
        }

        BooleanRenderState toggled() {
            if (this == ENABLED) return DISABLED;
            return ENABLED;
        }

        public boolean enabled() {
            return this == ENABLED;
        }

        @Override
        public String toString() {
            return cf.toString() + name();
        }
    }

    public enum EntityRenderState {
        ALL,
        PLAYERS,
        // WHITELIST,
        SELF,
        NONE;

        EntityRenderState next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Override
        public String toString() {
            return ChatFormatting.AQUA.toString() + name();
        }
    }
}

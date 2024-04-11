package dev.p0ke.greenscreen;

import java.util.HashMap;
import net.minecraft.ChatFormatting;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;


public class Greenscreen {
    private BooleanRenderState enabled;

    private BooleanRenderState blocks;
    private BooleanRenderState particles;
    private EntityRenderState armorStands;

    private EntityRenderState entities;
    private EntityRenderState nameTags;
    private Set<String> whitelist;
    private Set<String> blacklist;
    private HashMap<String, String> nameTagTransforms;

    private BooleanRenderState customSky;
    private Color skyColor;

    public Greenscreen() {
        enabled = BooleanRenderState.DISABLED;

        blocks = BooleanRenderState.DISABLED;
        particles = BooleanRenderState.DISABLED;
        entities = EntityRenderState.BLACKLIST;
        armorStands = EntityRenderState.ALL;
        nameTags = EntityRenderState.SELF;

        whitelist = new HashSet<>();
        blacklist = new HashSet<>();
        nameTagTransforms = new HashMap<>();

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

    public EntityRenderState armorStandRenderState() {
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

    public List<String> getWhitelist() {
        return whitelist.stream().toList();
    }

    public boolean isWhitelisted(String name) {
        return whitelist.contains(name.toLowerCase());
    }

    public List<String> getBlacklist() {
        return blacklist.stream().toList();
    }

    public boolean isBlacklisted(String name) {
        return blacklist.contains(name.toLowerCase());
    }

    public List<String> getNameTagTransformsList() {
        return nameTagTransforms.entrySet().stream().map(e -> e.getKey() + " :: " + e.getValue()).toList();
    }

    public boolean isTransformed(String name) {
        return nameTagTransforms.containsKey(name);
    }

    public String getTransformedNameTag(String name) {
        return nameTagTransforms.getOrDefault(name, name);
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

    public void setArmorStandRendering(EntityRenderState armorStands) {
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

    public EntityRenderState toggleArmorStandRendering() {
        armorStands = armorStands.next();
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

    public void setWhitelist(List<String> names) {
        whitelist.clear();
        names.forEach(n -> {
            if (!n.isEmpty())
                whitelist.add(n.toLowerCase());
        });
    }

    public boolean whitelistAdd(String name) {
        return whitelist.add(name.toLowerCase());
    }

    public boolean whitelistRemove(String name) {
        return whitelist.remove(name.toLowerCase());
    }

    public void whitelistClear() {
        whitelist.clear();
    }

    public void setBlacklist(List<String> names) {
        blacklist.clear();
        names.forEach(n -> {
            if (!n.isEmpty())
                blacklist.add(n.toLowerCase());
        });
    }

    public boolean blacklistAdd(String name) {
        return blacklist.add(name.toLowerCase());
    }

    public boolean blacklistRemove(String name) {
        return blacklist.remove(name.toLowerCase());
    }

    public void blacklistClear() {
        blacklist.clear();
    }

    public void setNameTagTransforms(List<String> transforms) {
        nameTagTransforms.clear();
        transforms.forEach(t -> {
            if (!t.isEmpty()) {
                nameTagTransforms.put(
                        StringUtils.substringBefore(t, "::").trim(),
                        StringUtils.substringAfter(t, "::").trim());
            }
        });
    }

    public void nameTagTransformsClear() {
        nameTagTransforms.clear();
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
        WHITELIST,
        BLACKLIST,
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

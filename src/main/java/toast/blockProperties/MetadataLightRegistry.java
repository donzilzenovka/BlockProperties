package toast.blockProperties;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;

/** Runtime lookup called by transformed world-aware block methods and entity movement code. */
public final class MetadataLightRegistry {
    public static final int NO_OVERRIDE = -1;

    private static final Map<Block, MetadataStats> STATS_BY_BLOCK = new IdentityHashMap<Block, MetadataStats>();

    private MetadataLightRegistry() {
    }

    public static synchronized void clear() {
        STATS_BY_BLOCK.clear();
    }

    public static synchronized void put(Block block, int[] brightness, int[] opacity, float[] hardness,
            float[] resistance, Block.SoundType[] sound, float[] slipperiness) {
        STATS_BY_BLOCK.put(block, new MetadataStats(brightness, opacity, hardness, resistance, sound, slipperiness));
    }

    public static int getLightValue(Block block, IBlockAccess world, int x, int y, int z) {
        MetadataStats stats = getStats(block);
        return stats == null ? NO_OVERRIDE : get(stats.brightness, world, x, y, z, NO_OVERRIDE);
    }

    public static int getLightOpacity(Block block, IBlockAccess world, int x, int y, int z) {
        MetadataStats stats = getStats(block);
        return stats == null ? NO_OVERRIDE : get(stats.opacity, world, x, y, z, NO_OVERRIDE);
    }

    public static float getBlockHardness(Block block, IBlockAccess world, int x, int y, int z) {
        MetadataStats stats = getStats(block);
        return stats == null ? Float.NaN : get(stats.hardness, world, x, y, z, Float.NaN);
    }

    public static float getExplosionResistance(Block block, IBlockAccess world, int x, int y, int z) {
        MetadataStats stats = getStats(block);
        float configured = stats == null ? Float.NaN : get(stats.resistance, world, x, y, z, Float.NaN);
        // BlockStats normalizes JSON resistance to Block's stored resistance value.
        return Float.isNaN(configured) ? Float.NaN : configured / 5.0F;
    }

    public static Block.SoundType getSoundType(Block block, Entity entity, int x, int y, int z) {
        return getSoundType(block, entity == null ? null : entity.worldObj, x, y, z);
    }

    public static Block.SoundType getSoundTypeAtFeet(Block block, Entity entity) {
        if (entity == null) return block.stepSound;
        return getSoundType(block, entity.worldObj, MathHelper.floor_double(entity.posX),
                MathHelper.floor_double(entity.boundingBox.minY) - 1, MathHelper.floor_double(entity.posZ));
    }

    public static float getSlipperinessAtFeet(Block block, Entity entity) {
        if (entity == null) return block.slipperiness;
        MetadataStats stats = getStats(block);
        return stats == null ? block.slipperiness : get(stats.slipperiness, entity.worldObj,
                MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.boundingBox.minY) - 1,
                MathHelper.floor_double(entity.posZ), block.slipperiness);
    }

    private static Block.SoundType getSoundType(Block block, IBlockAccess world, int x, int y, int z) {
        MetadataStats stats = getStats(block);
        if (stats == null || world == null) return block.stepSound;
        int meta = world.getBlockMetadata(x, y, z);
        Block.SoundType value = meta >= 0 && meta < stats.sound.length ? stats.sound[meta] : null;
        return value == null ? block.stepSound : value;
    }

    private static synchronized MetadataStats getStats(Block block) {
        return STATS_BY_BLOCK.get(block);
    }

    private static int get(int[] values, IBlockAccess world, int x, int y, int z, int fallback) {
        if (world == null) return fallback;
        int meta = world.getBlockMetadata(x, y, z);
        return meta >= 0 && meta < values.length ? values[meta] : fallback;
    }

    private static float get(float[] values, IBlockAccess world, int x, int y, int z, float fallback) {
        if (world == null) return fallback;
        int meta = world.getBlockMetadata(x, y, z);
        return meta >= 0 && meta < values.length ? values[meta] : fallback;
    }

    private static final class MetadataStats {
        private final int[] brightness;
        private final int[] opacity;
        private final float[] hardness;
        private final float[] resistance;
        private final Block.SoundType[] sound;
        private final float[] slipperiness;

        private MetadataStats(int[] brightness, int[] opacity, float[] hardness, float[] resistance,
                Block.SoundType[] sound, float[] slipperiness) {
            this.brightness = brightness.clone();
            this.opacity = opacity.clone();
            this.hardness = hardness.clone();
            this.resistance = resistance.clone();
            this.sound = sound.clone();
            this.slipperiness = slipperiness.clone();
        }
    }
}

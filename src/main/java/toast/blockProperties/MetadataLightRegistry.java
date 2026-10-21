package toast.blockProperties;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

/** Runtime lookup called by the transformed world-aware Block light method. */
public final class MetadataLightRegistry {
    public static final int NO_OVERRIDE = -1;

    private static final Map<Block, int[]> LIGHT_BY_BLOCK = new IdentityHashMap<Block, int[]>();

    private MetadataLightRegistry() {
    }

    public static synchronized void clear() {
        LIGHT_BY_BLOCK.clear();
    }

    public static synchronized void put(Block block, int[] brightness) {
        LIGHT_BY_BLOCK.put(block, brightness.clone());
    }

    /** Returns -1 when normal Block behavior should be used. */
    public static int getLightValue(Block block, IBlockAccess world, int x, int y, int z) {
        if (block == null || world == null) {
            return NO_OVERRIDE;
        }
        int[] values;
        synchronized (MetadataLightRegistry.class) {
            values = LIGHT_BY_BLOCK.get(block);
        }
        if (values == null) {
            return NO_OVERRIDE;
        }
        int meta = world.getBlockMetadata(x, y, z);
        return meta >= 0 && meta < values.length ? values[meta] : NO_OVERRIDE;
    }
}

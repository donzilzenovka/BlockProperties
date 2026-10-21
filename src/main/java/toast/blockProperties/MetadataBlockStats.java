package toast.blockProperties;

import java.util.Arrays;

import net.minecraft.block.Block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Metadata-specific block stats which cannot be represented by Block fields. */
public final class MetadataBlockStats {
    private final int[] brightness = new int[16];
    private final int[] opacity = new int[16];
    private final float[] hardness = new float[16];
    private final float[] resistance = new float[16];
    private final Block.SoundType[] sound = new Block.SoundType[16];
    private final boolean[] soundSet = new boolean[16];
    private final float[] slipperiness = new float[16];

    public MetadataBlockStats(String path, JsonArray nodes) {
        Arrays.fill(this.brightness, -1);
        Arrays.fill(this.opacity, -1);
        Arrays.fill(this.hardness, Float.NaN);
        Arrays.fill(this.resistance, Float.NaN);
        Arrays.fill(this.slipperiness, Float.NaN);

        for (int i = 0; i < nodes.size(); i++) {
            String entryPath = path + "\\entry_" + (i + 1);
            JsonObject node;
            try {
                node = nodes.get(i).getAsJsonObject();
            }
            catch (Exception ex) {
                throw new BlockPropertyException("Metadata stats entry must be an object!", entryPath, ex);
            }

            FileHelper.verify(node, entryPath, new String[] { "_meta" }, new String[] {
                    "brightness", "opacity", "hardness", "resistance", "sound", "slipperiness"
            });
            if (!node.has("brightness") && !node.has("opacity") && !node.has("hardness") &&
                    !node.has("resistance") && !node.has("sound") && !node.has("slipperiness")) {
                throw new BlockPropertyException("Metadata stats entry does not define any stats!", entryPath);
            }
            this.readMetadata(node.get("_meta"), entryPath + "\\_meta", node, entryPath);
        }
    }

    private void readMetadata(JsonElement element, String path, JsonObject node, String entryPath) {
        if (element == null || element.isJsonNull()) {
            throw new BlockPropertyException("Missing metadata selector!", path);
        }
        if (element.isJsonArray()) {
            JsonArray values = element.getAsJsonArray();
            if (values.size() == 0) {
                throw new BlockPropertyException("Metadata selector array cannot be empty!", path);
            }
            for (int i = 0; i < values.size(); i++) {
                this.readMetadata(values.get(i), path + "[" + i + "]", node, entryPath);
            }
            return;
        }

        String selector;
        try {
            selector = element.getAsString().trim();
        }
        catch (Exception ex) {
            throw new BlockPropertyException("Invalid metadata selector!", path, ex);
        }
        String[] selectors = selector.split(",");
        for (String part : selectors) {
            part = part.trim();
            int separator = part.indexOf('-');
            if (separator < 0) {
                this.setStats(this.parseMeta(part, path), node, entryPath);
            }
            else {
                int first = this.parseMeta(part.substring(0, separator).trim(), path);
                int last = this.parseMeta(part.substring(separator + 1).trim(), path);
                if (first > last) {
                    throw new BlockPropertyException("Invalid metadata range! (start is greater than end)", path);
                }
                for (int meta = first; meta <= last; meta++) {
                    this.setStats(meta, node, entryPath);
                }
            }
        }
    }

    private int parseMeta(String value, String path) {
        try {
            int meta = Integer.parseInt(value);
            if (meta < 0 || meta > 15) {
                throw new BlockPropertyException("Invalid metadata! (must be 0-15)", path);
            }
            return meta;
        }
        catch (NumberFormatException ex) {
            throw new BlockPropertyException("Invalid metadata! (" + value + ")", path, ex);
        }
    }

    private void setStats(int meta, JsonObject node, String path) {
        if (node.has("brightness")) {
            int value = FileHelper.readInteger(node, path, "brightness", -1);
            if (value < 0 || value > 15) {
                throw new BlockPropertyException("Invalid brightness! (must be 0-15)", path + "\\brightness");
            }
            if (this.brightness[meta] >= 0) {
                throw new BlockPropertyException("Duplicate metadata brightness! (meta: " + meta + ")", path);
            }
            this.brightness[meta] = value;
        }
        if (node.has("opacity")) {
            int value = FileHelper.readInteger(node, path, "opacity", -1);
            if (value < 0 || value > 255) {
                throw new BlockPropertyException("Invalid opacity! (must be 0-255)", path + "\\opacity");
            }
            if (this.opacity[meta] >= 0) {
                throw new BlockPropertyException("Duplicate metadata opacity! (meta: " + meta + ")", path);
            }
            this.opacity[meta] = value;
        }
        if (node.has("hardness")) {
            float value = (float) FileHelper.readDouble(node, path, "hardness");
            if (!Float.isNaN(this.hardness[meta])) {
                throw new BlockPropertyException("Duplicate metadata hardness! (meta: " + meta + ")", path);
            }
            this.hardness[meta] = value;
        }
        if (node.has("resistance")) {
            float value = (float) FileHelper.readDouble(node, path, "resistance");
            if (!Float.isNaN(this.resistance[meta])) {
                throw new BlockPropertyException("Duplicate metadata resistance! (meta: " + meta + ")", path);
            }
            this.resistance[meta] = value;
        }
        if (node.has("sound")) {
            Block.SoundType value = BlockStats.getSound(path, FileHelper.readText(node, path, "sound", ""));
            if (this.soundSet[meta]) {
                throw new BlockPropertyException("Duplicate metadata sound! (meta: " + meta + ")", path);
            }
            this.sound[meta] = value;
            this.soundSet[meta] = true;
        }
        if (node.has("slipperiness")) {
            float value = (float) FileHelper.readDouble(node, path, "slipperiness");
            if (!Float.isNaN(this.slipperiness[meta])) {
                throw new BlockPropertyException("Duplicate metadata slipperiness! (meta: " + meta + ")", path);
            }
            this.slipperiness[meta] = value;
        }
    }

    public void init(Block block) {
        MetadataLightRegistry.put(block, this.brightness, this.opacity, this.hardness,
                this.resistance, this.sound, this.slipperiness);
    }
}

package toast.blockProperties;

import java.util.Arrays;

import net.minecraft.block.Block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Metadata-specific block stats which cannot be represented by Block fields. */
public final class MetadataBlockStats {
    private final int[] brightness = new int[16];

    public MetadataBlockStats(String path, JsonArray nodes) {
        Arrays.fill(this.brightness, -1);

        for (int i = 0; i < nodes.size(); i++) {
            String entryPath = path + "\\entry_" + (i + 1);
            JsonObject node;
            try {
                node = nodes.get(i).getAsJsonObject();
            }
            catch (Exception ex) {
                throw new BlockPropertyException("Metadata stats entry must be an object!", entryPath, ex);
            }

            FileHelper.verify(node, entryPath, new String[] { "_meta", "brightness" }, new String[0]);
            int light = FileHelper.readInteger(node, entryPath, "brightness", -1);
            if (light < 0 || light > 15) {
                throw new BlockPropertyException("Invalid brightness! (must be 0-15)", entryPath + "\\brightness");
            }
            this.readMetadata(node.get("_meta"), entryPath + "\\_meta", light);
        }
    }

    private void readMetadata(JsonElement element, String path, int light) {
        if (element == null || element.isJsonNull()) {
            throw new BlockPropertyException("Missing metadata selector!", path);
        }
        if (element.isJsonArray()) {
            JsonArray values = element.getAsJsonArray();
            if (values.size() == 0) {
                throw new BlockPropertyException("Metadata selector array cannot be empty!", path);
            }
            for (int i = 0; i < values.size(); i++) {
                this.readMetadata(values.get(i), path + "[" + i + "]", light);
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
                this.setBrightness(this.parseMeta(part, path), light, path);
            }
            else {
                int first = this.parseMeta(part.substring(0, separator).trim(), path);
                int last = this.parseMeta(part.substring(separator + 1).trim(), path);
                if (first > last) {
                    throw new BlockPropertyException("Invalid metadata range! (start is greater than end)", path);
                }
                for (int meta = first; meta <= last; meta++) {
                    this.setBrightness(meta, light, path);
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

    private void setBrightness(int meta, int light, String path) {
        if (this.brightness[meta] >= 0) {
            throw new BlockPropertyException("Duplicate metadata brightness! (meta: " + meta + ")", path);
        }
        this.brightness[meta] = light;
    }

    public void init(Block block) {
        MetadataLightRegistry.put(block, this.brightness);
    }
}

package com.syaru.mesubnetbus;

import java.util.function.Function;
import appeng.api.stacks.AEKey;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.nbt.*;

public final class BusSettings {
    private BusSettings() {}
    public static CompoundTag save(BaseSubnetPart part, Function<AEKey, CompoundTag> encode) {
        var tag = new CompoundTag();
        tag.putInt("Schema",1); tag.putInt("Mode",part.mode.ordinal());
        tag.putBoolean("Reversed",part.reversed); tag.putInt("Priority",part.priority);
        tag.putBoolean("Exclude",part.excludeFilter);
        var filters = new ListTag();
        for (var key : part.filters) filters.add(encode.apply(key));
        tag.put("Filters",filters);
        return tag;
    }
    public static void load(BaseSubnetPart part, CompoundTag tag, Function<CompoundTag,AEKey> decode) {
        part.mode = tag.contains("Mode") ? Mode.decode(tag.getInt("Mode")) : Mode.SHARED;
        part.reversed = tag.getBoolean("Reversed"); part.priority = tag.getInt("Priority");
        part.excludeFilter = tag.getBoolean("Exclude"); part.filters.clear();
        var list = tag.getList("Filters",Tag.TAG_COMPOUND);
        for (int i=0; i<Math.min(9,list.size()); i++) {
            var key = decode.apply(list.getCompound(i));
            if (key != null) part.filters.add(key);
            else part.mode = Mode.POWER;
        }
        if (tag.getInt("Schema") > 1 || list.size() > 9) part.mode = Mode.POWER;
    }
}

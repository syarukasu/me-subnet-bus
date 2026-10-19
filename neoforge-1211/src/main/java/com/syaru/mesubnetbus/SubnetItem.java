package com.syaru.mesubnetbus;

import java.util.List;
import appeng.items.parts.PartItem;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;

public final class SubnetItem extends PartItem<SubnetPart> {
    public SubnetItem() { super(new Item.Properties(),SubnetPart.class,SubnetPart::new); }
    private static CompoundTag settings(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA); return data==null?new CompoundTag():data.copyTag();
    }
    private static Mode mode(CompoundTag tag) { return tag.contains("Mode")?Mode.decode(tag.getInt("Mode")):Mode.SHARED; }
    public static float modelIndex(ItemStack stack) { var tag=settings(stack); return mode(tag).ordinal()*2+(tag.getBoolean("Reversed")?1:0); }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable("item.me_subnet_bus.named",Component.translatable(mode(settings(stack)).translation()));
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> lines,TooltipFlag flag) {
        var tag=settings(stack);
        lines.add(Component.translatable(mode(tag).translation()));
        lines.add(Component.translatable("tooltip.me_subnet_bus.source",Component.translatable(tag.getBoolean("Reversed")?"side.me_subnet_bus.outer":"side.me_subnet_bus.inner")));
        lines.add(Component.translatable("tooltip.me_subnet_bus.target",Component.translatable(tag.getBoolean("Reversed")?"side.me_subnet_bus.inner":"side.me_subnet_bus.outer")));
    }
}

package com.syaru.mesubnetbus;

import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Hosts AE2's unconfigured interface logic without local stocking slots. */
final class InterfaceEndpoint implements InterfaceLogicHost {
    private final BaseSubnetPart part;
    private final InterfaceLogic logic;

    InterfaceEndpoint(BaseSubnetPart part) {
        this.part = part;
        logic = new InterfaceLogic(part.target(), this, part.getPartItem().asItem(), 0);
    }

    @Override public InterfaceLogic getInterfaceLogic() { return logic; }
    @Override public BlockEntity getBlockEntity() { return part.blockEntity(); }
    @Override public ItemStack getMainMenuIcon() { return new ItemStack(part.getPartItem()); }
    @Override public void saveChanges() { if (part.host != null) part.host.markForSave(); }
}

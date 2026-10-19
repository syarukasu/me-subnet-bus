package com.syaru.mesubnetbus;

import appeng.api.parts.*;
import appeng.api.stacks.AEKey;
import appeng.parts.PartModel;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import appeng.util.SettingsFrom;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class SubnetPart extends BaseSubnetPart {
    public static final ResourceLocation[] MODELS = new ResourceLocation[8];
    static {
        for(int i=0;i<8;i++) MODELS[i]=ResourceLocation.fromNamespaceAndPath("me_subnet_bus","part/bus_"+i);
    }
    public SubnetPart(IPartItem<?> item) { super(item); }
    @Override public IPartModel getStaticModels() { return new PartModel(MODELS[mode.ordinal()*2+(reversed?1:0)]); }
    public CompoundTag settings() {
        return BusSettings.save(this,key->key.toTagGeneric(blockEntity.getLevel().registryAccess()));
    }
    private void settings(CompoundTag tag, HolderLookup.Provider registries) {
        BusSettings.load(this,tag,key->AEKey.fromTagGeneric(registries,key));
    }
    @Override public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        saveNodes(tag); tag.put("Subnet",settings());
    }
    @Override public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        settings(tag.getCompound("Subnet"),registries); loadNodes(tag);
    }
    @Override public void exportSettings(SettingsFrom from,DataComponentMap.Builder tag) {
        tag.set(DataComponents.CUSTOM_DATA,CustomData.of(settings()));
    }
    @Override public void importSettings(SettingsFrom from,DataComponentMap tag,Player player) {
        Mode previous=mode;
        boolean previousReversed=reversed;
        var custom=tag.get(DataComponents.CUSTOM_DATA); if(custom!=null) settings(custom.copyTag(),blockEntity.getLevel().registryAccess());
        Mode loaded=mode; mode=previous;
        boolean loadedReversed=reversed; reversed=previousReversed;
        configure(loaded,loadedReversed,priority,excludeFilter,java.util.List.copyOf(filters));
    }
    @Override public void writeToStream(RegistryFriendlyByteBuf buf) {
        buf.writeByte(mode.ordinal()); buf.writeBoolean(reversed); buf.writeUtf(status,64);
    }
    @Override public boolean readFromStream(RegistryFriendlyByteBuf buf) {
        mode=Mode.decode(buf.readUnsignedByte()); reversed=buf.readBoolean(); status=buf.readUtf(64); return true;
    }
    @Override public boolean onUseItemOn(ItemStack stack,Player player,InteractionHand hand,Vec3 pos) {
        return hand==InteractionHand.MAIN_HAND && wrench(player,false);
    }
    @Override public boolean onUseWithoutItem(Player player,Vec3 pos) {
        if(player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return false;
        openSettings(player);
        return true;
    }
    @Override public void openSettings(Player player) {
        if(player instanceof ServerPlayer server && canConfigure(player)) {
            var provider=new SimpleMenuProvider((id,inv,p)->new SubnetMenu(SubnetBusMod.MENU.get(),id,inv,this),
                    Component.translatable("item.me_subnet_bus.subnet_bus"));
            server.openMenu(provider);
        }
    }
}

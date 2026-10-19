package com.syaru.mesubnetbus;

import java.util.*;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.*;
import appeng.api.stacks.AEKey;
import appeng.api.storage.IStorageProvider;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalBlockPos;
import appeng.api.util.INetworkToolAware;
import appeng.me.energy.IEnergyOverlayGridConnection;
import appeng.me.service.EnergyService;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public abstract class BaseSubnetPart implements IPart, IActionHost, INetworkToolAware {
    public Mode mode = Mode.SHARED;
    public boolean reversed;
    public int priority;
    public final List<AEKey> filters = new ArrayList<>();
    public boolean excludeFilter;
    public String status = "disconnected";
    boolean valid;
    IGrid lastSource;
    IGrid lastTarget;
    public IManagedGridNode inner;
    public IManagedGridNode outer;
    protected IPartHost host;
    protected BlockEntity blockEntity;
    protected Direction side;
    private final IPartItem<?> item;
    private final SubnetStorage storage = new SubnetStorage();
    private InterfaceEndpoint endpoint;
    private boolean inWorld;
    private final IGridNodeListener<BaseSubnetPart> listener = new IGridNodeListener<>() {
        @Override public void onSaveChanges(BaseSubnetPart owner, IGridNode node) { if (host != null) host.markForSave(); }
        @Override public void onGridChanged(BaseSubnetPart owner, IGridNode node) {
            if (endpoint != null && node == target().getNode() && target().getGrid() != null) {
                endpoint.getInterfaceLogic().gridChanged();
            }
            LinkTopology.changed(owner);
        }
        @Override public void onStateChanged(BaseSubnetPart owner, IGridNode node, State state) { LinkTopology.changed(owner); }
        @Override public void onInWorldConnectionChanged(BaseSubnetPart owner, IGridNode node) { LinkTopology.changed(owner); }
    };

    protected BaseSubnetPart(IPartItem<?> item) { this.item = item; makeNodes(new CompoundTag()); }
    private IManagedGridNode makeNode(boolean outside) {
        var node = GridHelper.createManagedNode(this, listener)
                .setTagName(outside ? "outer" : "inner").setVisualRepresentation(item)
                .setIdlePowerUsage(mode.storage() ? 0.5 : 0)
                .setInWorldNode(outside)
                .setExposedOnSides(outside && side != null ? EnumSet.of(side) : EnumSet.noneOf(Direction.class));
        node.setFlags(mode.storage() ? new GridFlags[]{GridFlags.REQUIRE_CHANNEL}
                : new GridFlags[]{GridFlags.CANNOT_CARRY});
        node.addService(IEnergyOverlayGridConnection.class, () -> {
            var other = outside ? inner : outer;
            if (other == null || other.getGrid() == null) return List.of();
            return List.of((EnergyService) other.getGrid().getEnergyService());
        });
        node.addService(IStorageProvider.class, mounts -> {
            if (usable() && (outside == reversed)) mounts.mount(storage, priority);
        });
        return node;
    }
    private void makeNodes(CompoundTag saved) {
        storage.disconnect();
        endpoint = null;
        inner = makeNode(false); outer = makeNode(true);
        if (mode.storage()) endpoint = new InterfaceEndpoint(this);
        inner.loadFromNBT(saved); outer.loadFromNBT(saved);
    }
    public IManagedGridNode source() { return reversed ? outer : inner; }
    public IManagedGridNode target() { return reversed ? inner : outer; }
    public boolean usable() { return valid && connectionStatus().equals("ready"); }
    void disconnectStorage() { valid = false; storage.disconnect(); }
    void refreshStorage() {
        if (usable() && endpoint != null) storage.connect(this, endpoint.getInterfaceLogic().getInventory());
        else storage.disconnect();
    }
    String locationKey() { return blockEntity.getLevel().dimension().location() + "/" + blockEntity.getBlockPos().asLong() + "/" + side; }
    String connectionStatus() {
        if (!mode.storage()) return "power";
        if (!inWorld || inner.getNode() == null || outer.getNode() == null
                || inner.getNode().getConnections().isEmpty() || outer.getNode().getConnections().isEmpty()) return "disconnected";
        if (inner.getGrid() == outer.getGrid()) return "same_grid";
        if (!inner.isPowered() || !outer.isPowered()) return "no_power";
        if (!inner.isActive() || !outer.isActive()) return "no_channel";
        return "ready";
    }
    @Override public IPartItem<?> getPartItem() { return item; }
    @Override public IGridNode getGridNode() { return inner.getNode(); }
    @Override public IGridNode getActionableNode() { return source().getNode(); }
    @Override public IGridNode getExternalFacingNode() { return outer.getNode(); }
    @Override public void setPartHostInfo(Direction side, IPartHost host, BlockEntity be) {
        this.side = side; this.host = host; this.blockEntity = be;
        outer.setExposedOnSides(EnumSet.of(side));
    }
    @Override public void onPlacement(Player player) { inner.setOwningPlayer(player); outer.setOwningPlayer(player); }
    @Override public void addToWorld() {
        inWorld = true;
        inner.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        outer.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (!blockEntity.getLevel().isClientSide()) LinkTopology.add(this);
    }
    @Override public void removeFromWorld() {
        inWorld = false; disconnectStorage();
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) LinkTopology.remove(this);
        var saved = new CompoundTag(); saveNodes(saved);
        inner.destroy(); outer.destroy();
        makeNodes(saved);
    }
    public void saveNodes(CompoundTag tag) { inner.saveToNBT(tag); outer.saveToNBT(tag); }
    public void loadNodes(CompoundTag tag) { makeNodes(tag); }
    public void configure(Mode nextMode, boolean flip, int nextPriority, boolean exclude, List<AEKey> keys) {
        boolean rebuild = mode.storage() != nextMode.storage() || reversed != flip;
        if (keys.size() > 9) throw new IllegalArgumentException("At most nine filters");
        boolean live = inWorld;
        if (rebuild && live) removeFromWorld();
        mode = nextMode; reversed = flip; priority = nextPriority; excludeFilter = exclude;
        filters.clear(); filters.addAll(keys);
        if (rebuild) {
            var saved = new CompoundTag(); saveNodes(saved); makeNodes(saved);
            if (live) {
                addToWorld();
                var cable = host.getPart(null);
                if (inner.getNode()!=null && cable != null && cable.getGridNode() != null) GridHelper.createConnection(inner.getNode(), cable.getGridNode());
            }
        }
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) LinkTopology.changed(this);
        if (host != null) { host.markForSave(); host.markForUpdate(); }
    }
    public boolean canConfigure(Player player) {
        return !player.isSpectator() && player.mayBuild()
                && Platform.hasPermissions(new DimensionalBlockPos(blockEntity), player);
    }
    public boolean wrench(Player player, boolean left) {
        ItemStack held = player.getMainHandItem();
        if (!InteractionUtil.canWrenchRotate(held) && !InteractionUtil.canWrenchDisassemble(held)) return false;
        if (!blockEntity.getLevel().isClientSide() && canConfigure(player)) {
            if (!left && player.isShiftKeyDown()) {
                List<ItemStack> drops = new ArrayList<>(); addPartDrop(drops, true);
                host.removePart(this); host.cleanup();
                for (ItemStack stack : drops) if (!player.getInventory().add(stack)) player.drop(stack, false);
            } else {
                configure(left && player.isShiftKeyDown() ? mode : mode.next(left ? -1 : 1),
                        left && player.isShiftKeyDown() ? !reversed : reversed, priority, excludeFilter, List.copyOf(filters));
                player.displayClientMessage(Component.translatable(mode.translation()), true);
            }
        }
        return true;
    }
    @Override public boolean onClicked(Player player, Vec3 pos) { return wrench(player, true); }
    @Override public boolean onShiftClicked(Player player, Vec3 pos) { return wrench(player, true); }
    @Override public boolean showNetworkInfo(UseOnContext context) {
        if (context.getPlayer() != null) openSettings(context.getPlayer());
        return false;
    }
    public abstract void openSettings(Player player);
    @Override public void getBoxes(IPartCollisionHelper helper) { helper.addBox(4,4,13,12,12,16); }
    @Override public float getCableConnectionLength(AECableType cable) { return 16; }
    public Direction side() { return side; }
    public BlockEntity blockEntity() { return blockEntity; }
}

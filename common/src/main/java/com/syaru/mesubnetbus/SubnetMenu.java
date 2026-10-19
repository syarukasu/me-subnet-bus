package com.syaru.mesubnetbus;

import java.util.ArrayList;
import appeng.api.stacks.GenericStack;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class SubnetMenu extends AbstractContainerMenu {
    public static final int FLIP = 5;
    public static final int INCLUDE = 6;
    public static final int EXCLUDE = 7;
    public static final int SELECT_MODE = 10;
    private final BaseSubnetPart part;
    private final SimpleContainer filters = new SimpleContainer(9);
    private final ContainerData data = new SimpleContainerData(6);
    public static final String[] STATUSES={"ready","power","disconnected","same_grid","cycle","duplicate_path","no_power","no_channel"};
    public SubnetMenu(MenuType<?> type, int id, Inventory inventory, BaseSubnetPart part) {
        super(type,id); this.part=part;
        for(int i=0;i<9;i++) addSlot(new Slot(filters,i,43+i*18,93) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for(int y=0;y<3;y++) for(int x=0;x<9;x++) addSlot(new Slot(inventory,9+y*9+x,43+x*18,158+y*18));
        for(int x=0;x<9;x++) addSlot(new Slot(inventory,x,43+x*18,216));
        addDataSlots(data); refresh();
    }
    public int priority() { return (data.get(1)&65535) | ((data.get(2)&65535)<<16); }
    public boolean exclude() { return data.get(3)!=0; }
    public boolean reversed() { return data.get(4)!=0; }
    public int mode() { return data.get(0); }
    public String status() { return STATUSES[Math.max(0,Math.min(STATUSES.length-1,data.get(5)))]; }
    private void refresh() {
        if(part==null) return;
        data.set(0,part.mode.ordinal()); data.set(1,part.priority&65535); data.set(2,part.priority>>>16);
        data.set(3,part.excludeFilter?1:0); data.set(4,part.reversed?1:0);
        data.set(5,java.util.Arrays.asList(STATUSES).indexOf(part.status));
        for(int i=0;i<9;i++) filters.setItem(i,i<part.filters.size()?part.filters.get(i).wrapForDisplayOrFilter():ItemStack.EMPTY);
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if(slot>=0 && slot<9) {
            if(part!=null && stillValid(player) && (type==ClickType.PICKUP || type==ClickType.QUICK_MOVE)) {
                var keys=new ArrayList<>(part.filters);
                if(button==1 || getCarried().isEmpty()) { if(slot<keys.size()) keys.remove(slot); }
                else {
                    var stack=GenericStack.fromItemStack(getCarried());
                    if(stack!=null) { if(slot<keys.size()) keys.set(slot,stack.what()); else if(keys.size()<9) keys.add(stack.what()); }
                }
                part.configure(part.mode,part.reversed,part.priority,part.excludeFilter,keys);
                broadcastChanges();
            }
            return;
        }
        super.clicked(slot,button,type,player);
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        if(part==null || !stillValid(player)) return false;
        long priority=part.priority;
        Mode mode=part.mode;
        boolean reversed=part.reversed;
        boolean exclude=part.excludeFilter;
        if(id==0) priority--; else if(id==1) priority++;
        else if(id==3) priority-=100; else if(id==4) priority+=100;
        else if(id==FLIP) reversed=!reversed;
        else if(id==INCLUDE) exclude=false;
        else if(id==EXCLUDE) exclude=true;
        else if(id>=SELECT_MODE && id<SELECT_MODE+Mode.values().length) mode=Mode.values()[id-SELECT_MODE];
        else if(id!=2) return false;
        part.configure(mode,reversed,(int)Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,priority)),
                id==2?!part.excludeFilter:exclude,new ArrayList<>(part.filters));
        broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return part==null || (part.blockEntity()!=null && !part.blockEntity().isRemoved()
                && part.host.getPart(part.side())==part && part.canConfigure(player)
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(part.blockEntity().getBlockPos()))<=64);
    }
}

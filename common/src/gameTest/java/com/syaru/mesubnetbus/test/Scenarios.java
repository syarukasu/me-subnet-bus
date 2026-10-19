package com.syaru.mesubnetbus.test;

import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.*;
import appeng.api.stacks.*;
import appeng.api.storage.*;
import appeng.api.util.AEColor;
import appeng.core.definitions.*;
import appeng.me.helpers.MachineSource;
import com.syaru.mesubnetbus.*;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

public final class Scenarios {
    private Scenarios() {}
    public record Setup(SubnetPart bus, IPart leftPart, IPart rightPart, Storage storage, IActionSource source) {
        public IGridNode left() { return leftPart.getGridNode(); }
        public IGridNode right() { return rightPart.getGridNode(); }
    }
    public static Setup place(GameTestHelper h,Direction direction) {
        return place(h,direction,new BlockPos(4,4,4));
    }
    public static Setup place(GameTestHelper h,Direction direction,BlockPos relative) {
        var level=h.getLevel(); var pos=h.absolutePos(relative);
        var left=PartHelper.setPart(level,pos,null,null,AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        var right=PartHelper.setPart(level,pos.relative(direction),null,null,AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        var bus=PartHelper.setPart(level,pos,direction,null,SubnetBusMod.ITEM.get());
        h.assertTrue(left!=null && right!=null && bus!=null,"Both cables and subnet part must be placeable");
        // A creative energy cell on the inner cable powers both networks via the actual energy overlay.
        level.setBlockAndUpdate(pos.relative(direction.getOpposite()),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        return new Setup(bus,left,right,new Storage(),new MachineSource(bus));
    }
    public static void direction(GameTestHelper h,Direction direction) {
        var setup=place(h,direction);
        h.runAfterDelay(80,()->{
            var bus=setup.bus();
            h.assertTrue(bus.inner.getGrid()!=bus.outer.getGrid(),"Subnet grids must remain independent");
            h.assertTrue(bus.usable(),"Both sides must be powered and active: "+bus.status);
            h.assertTrue(bus.inner.getNode().getUsedChannels()==1 && bus.outer.getNode().getUsedChannels()==1,"Each side consumes one channel");
            setup.right().getGrid().getStorageService().addGlobalStorageProvider(mounts->mounts.mount(setup.storage(),0));
            var key=AEItemKey.of(Items.IRON_INGOT); setup.storage().count.add(key,100);
            var inv=setup.left().getGrid().getStorageService().getInventory();
            h.assertTrue(inv.getAvailableStacks().get(key)==100,"The remote stock is exposed exactly once");
            h.assertTrue(inv.extract(key,70,Actionable.SIMULATE,setup.source())==70,"Simulation observes available stock");
            h.assertTrue(setup.storage().count.get(key)==100,"Simulation must not consume stock");
            h.assertTrue(inv.extract(key,150,Actionable.MODULATE,setup.source())==100,"Extraction capped by actual stock");
            h.assertTrue(setup.storage().count.get(key)==0,"No duplicate inventory remains");
            h.assertTrue(inv.insert(key,Long.MAX_VALUE,Actionable.MODULATE,setup.source())==Long.MAX_VALUE,"Long transfer stays exact");
            h.assertTrue(setup.storage().lastSource==setup.source(),"Original action source is preserved");
            h.succeed();
        });
    }
    public static void modes(GameTestHelper h) {
        var s=place(h,Direction.EAST);
        h.runAfterDelay(80,()->{
            var bus=s.bus();
            h.assertTrue(bus.usable(),"Storage initially active: "+bus.status);
            var key=AEItemKey.of(Items.IRON_INGOT); var water=AEFluidKey.of(Fluids.WATER);
            s.right().getGrid().getStorageService().addGlobalStorageProvider(m->m.mount(s.storage(),0));
            s.storage().count.add(key,100); s.storage().count.add(water,16000);
            bus.configure(Mode.EXTRACT,false,7,false,List.of(key));
            LinkTopology.tick();
            var inv=s.left().getGrid().getStorageService().getInventory();
            h.assertTrue(inv.insert(key,4,Actionable.MODULATE,s.source())==0,"Extract-only rejects insert");
            h.assertTrue(inv.extract(water,1,Actionable.MODULATE,s.source())==0,"Filter rejects fluid");
            h.assertTrue(inv.extract(key,10,Actionable.MODULATE,s.source())==10,"Extract-only permits exact extraction");
            bus.configure(Mode.INSERT,false,7,false,List.of()); LinkTopology.tick();
            h.assertTrue(inv.extract(key,1,Actionable.MODULATE,s.source())==0,"Insert-only rejects extraction");
            h.assertTrue(inv.getAvailableStacks().get(key)==0,"Insert-only does not advertise extractable stock");
            h.assertTrue(inv.insert(water,8000,Actionable.MODULATE,s.source())==8000,"Generic fluid insert works");
            bus.configure(Mode.SHARED,true,7,true,List.of(key)); LinkTopology.tick();
            h.assertTrue(s.left().getGrid().getStorageService().getInventory().getAvailableStacks().get(water)==0,"Reversal unmounts old provider");
            bus.configure(Mode.POWER,false,7,false,List.of());
            h.runAfterDelay(60,()->{
                h.assertTrue(bus.inner.getGrid()!=bus.outer.getGrid(),"Power mode preserves grid separation");
                h.assertTrue(bus.inner.isPowered() && bus.outer.isPowered(),"Power shares in zero-channel mode");
                h.assertTrue(bus.inner.getNode().getUsedChannels()==0 && bus.outer.getNode().getUsedChannels()==0,"Power mode uses no channels");
                bus.configure(Mode.SHARED,false,7,false,List.of());
                h.runAfterDelay(60,()->{
                    h.assertTrue(bus.usable(),"Storage restored after node recreation: "+bus.status);
                    h.assertTrue(bus.inner.getNode().getUsedChannels()==1 && bus.outer.getNode().getUsedChannels()==1,"Channels restored");
                    h.succeed();
                });
            });
        });
    }
    public static void cycles(GameTestHelper h) {
        var a=place(h,Direction.EAST,new BlockPos(3,4,3));
        var b=place(h,Direction.EAST,new BlockPos(3,4,6));
        h.runAfterDelay(80,()->{
            GridHelper.createConnection(a.left(),b.right());
            GridHelper.createConnection(a.right(),b.left());
            h.runAfterDelay(60,()->{
                h.assertTrue(a.bus().usable()!=b.bus().usable(),"Exactly one of two cyclic exposure links must be rejected");
                h.assertTrue(a.bus().status.equals("cycle") || b.bus().status.equals("cycle"),"Cycle status must be reported");
                GridHelper.createConnection(a.left(),a.right());
                h.runAfterDelay(60,()->{
                    h.assertTrue(!a.bus().usable() && !b.bus().usable(),"Same-grid links must unmount storage");
                    h.assertTrue(a.bus().status.equals("same_grid") && b.bus().status.equals("same_grid"),"Same-grid warning required");
                    h.succeed();
                });
            });
        });
    }
    public static void nativeParity(GameTestHelper h) {
        var subnet = place(h, Direction.EAST, new BlockPos(2,4,2));
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(2,4,6));
        var cable = PartHelper.setPart(level,pos,null,null,AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        level.setBlockAndUpdate(pos.east(),AEBlocks.INTERFACE.block().defaultBlockState());
        var remote = PartHelper.setPart(level,pos.east(2),null,null,AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        var nativeBus = PartHelper.setPart(level,pos,Direction.EAST,null,AEParts.STORAGE_BUS.asItem());
        level.setBlockAndUpdate(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        level.setBlockAndUpdate(pos.east(3),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.runAfterDelay(100,()->{
            h.assertTrue(subnet.bus().usable() && nativeBus.getMainNode().isActive(),"Both fixtures must be active");
            h.assertTrue(cable.getGridNode().getGrid()!=remote.getGridNode().getGrid(),"Native fixture must use separate grids");
            var nativeStock = new Storage();
            remote.getGridNode().getGrid().getStorageService().addGlobalStorageProvider(m->m.mount(nativeStock,0));
            subnet.right().getGrid().getStorageService().addGlobalStorageProvider(m->m.mount(subnet.storage(),0));
            var actual = subnet.left().getGrid().getStorageService().getInventory();
            var reference = cable.getGridNode().getGrid().getStorageService().getInventory();
            var referenceSource = new MachineSource(nativeBus);
            var iron = AEItemKey.of(Items.IRON_INGOT);
            var water = AEFluidKey.of(Fluids.WATER);
            var keys = List.<AEKey>of(iron,water);
            for (var key : keys) { nativeStock.count.add(key,100); subnet.storage().count.add(key,100); }
            for (boolean filtered : new boolean[]{false,true}) {
                nativeBus.getConfig().setStack(0,filtered ? new GenericStack(iron,0) : null);
                for (var mode : List.of(Mode.SHARED,Mode.EXTRACT,Mode.INSERT)) {
                    var access = switch(mode) {
                        case EXTRACT -> appeng.api.config.AccessRestriction.READ;
                        case INSERT -> appeng.api.config.AccessRestriction.WRITE;
                        default -> appeng.api.config.AccessRestriction.READ_WRITE;
                    };
                    nativeBus.getConfigManager().putSetting(appeng.api.config.Settings.ACCESS,access);
                    subnet.bus().configure(mode,false,0,false,filtered ? List.of(iron) : List.of());
                    LinkTopology.tick();
                    for (var key : keys) {
                        h.assertTrue(actual.getAvailableStacks().get(key)==reference.getAvailableStacks().get(key),"Native listing parity: "+mode);
                        for (var action : Actionable.values()) for (long amount : new long[]{1,37,Long.MAX_VALUE}) {
                            long a=actual.extract(key,amount,action,subnet.source());
                            long b=reference.extract(key,amount,action,referenceSource);
                            h.assertTrue(a==b,"Native extraction parity: "+mode+" / "+action+" / "+amount);
                            a=actual.insert(key,amount,action,subnet.source());
                            b=reference.insert(key,amount,action,referenceSource);
                            h.assertTrue(a==b,"Native insertion parity: "+mode+" / "+action+" / "+amount);
                            h.assertTrue(nativeStock.count.get(key)==subnet.storage().count.get(key),"Native backing stock parity");
                        }
                    }
                }
            }
            h.succeed();
        });
    }
    public static void reversalAndDetach(GameTestHelper h) {
        var s=place(h,Direction.EAST);
        var local=new Storage();
        IStorageProvider remoteProvider=m->m.mount(s.storage(),0);
        var iron=AEItemKey.of(Items.IRON_INGOT);
        var gold=AEItemKey.of(Items.GOLD_INGOT);
        h.runAfterDelay(80,()->{
            s.left().getGrid().getStorageService().addGlobalStorageProvider(m->m.mount(local,0));
            s.right().getGrid().getStorageService().addGlobalStorageProvider(remoteProvider);
            local.count.add(iron,100); local.count.add(gold,50);
            s.storage().count.add(iron,200);
            s.bus().configure(Mode.SHARED,true,0,true,List.of(gold));
            h.runAfterDelay(60,()->{
                h.assertTrue(s.bus().usable(),"Reversed native interface must reconnect");
                var left=s.left().getGrid().getStorageService().getInventory();
                var right=s.right().getGrid().getStorageService().getInventory();
                h.assertTrue(left.getAvailableStacks().get(iron)==100,"Old exposure is removed");
                h.assertTrue(right.getAvailableStacks().get(iron)==300,"Reversed side exposes new target exactly once");
                h.assertTrue(right.getAvailableStacks().get(gold)==0,"Blacklist hides target stock");
                // Isolate the bus path: an unrelated local storage may legitimately accept gold.
                s.right().getGrid().getStorageService().removeGlobalStorageProvider(remoteProvider);
                h.assertTrue(right.extract(gold,1,Actionable.MODULATE,s.source())==0,"Blacklist rejects extraction");
                h.assertTrue(right.insert(gold,1,Actionable.MODULATE,s.source())==0,"Blacklist rejects insertion");
                h.assertTrue(local.count.get(gold)==50,"Filtered target stock remains untouched");
                s.right().getGrid().getStorageService().addGlobalStorageProvider(remoteProvider);
                h.assertTrue(right.extract(iron,250,Actionable.MODULATE,s.source())==250,"Reversed transfer uses both real inventories");
                h.assertTrue(local.count.get(iron)+s.storage().count.get(iron)==50,"Reversed extraction preserves quantity");
                var host=PartHelper.getPartHost(h.getLevel(),s.bus().blockEntity().getBlockPos());
                host.removePart(s.bus());
                h.assertTrue(right.getAvailableStacks().get(iron)==s.storage().count.get(iron),"Detached handler cannot expose old target");
                long before=local.count.get(iron);
                right.extract(iron,Long.MAX_VALUE,Actionable.MODULATE,s.source());
                h.assertTrue(local.count.get(iron)==before,"Detached handler cannot extract old target stock");
                h.succeed();
            });
        });
    }
    public static void physicalChainedSubnet(GameTestHelper h) {
        var s=place(h,Direction.EAST,new BlockPos(2,4,4));
        var level=h.getLevel();
        var pos=s.bus().blockEntity().getBlockPos();
        var terminal=PartHelper.setPart(level,pos,Direction.SOUTH,null,AEParts.TERMINAL.asItem());
        var far=PartHelper.setPart(level,pos.east(2),null,null,AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        var second=PartHelper.setPart(level,pos.east(),Direction.EAST,null,SubnetBusMod.ITEM.get());
        level.setBlockAndUpdate(pos.east(3),net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        var chest=(net.minecraft.world.Container)level.getBlockEntity(pos.east(3));
        chest.setItem(0,new net.minecraft.world.item.ItemStack(Items.IRON_INGOT,64));
        var nativeBus=PartHelper.setPart(level,pos.east(2),Direction.EAST,null,AEParts.STORAGE_BUS.asItem());
        h.runAfterDelay(100,()->{
            var a=s.left().getGrid(); var b=s.right().getGrid(); var c=far.getGridNode().getGrid();
            h.assertTrue(a!=b && a!=c && b!=c,"Three chained subnet grids remain independent");
            h.assertTrue(s.bus().usable() && second.usable(),"Both links powered from a single source");
            h.assertTrue(terminal.getMainNode().isActive() && nativeBus.getMainNode().isActive(),"Native terminal and storage bus have channels");
            // In an ad-hoc grid a node reports routed channels, not its own consumption.
            // A: terminal + endpoint, B: two endpoints, C: endpoint + native storage bus.
            for(var grid:List.of(a,b,c)) {
                h.assertTrue(grid.getPathingService().getUsedChannels()==2,"Each independent grid uses exactly two local channels");
            }
            var inv=a.getStorageService().getInventory(); var key=AEItemKey.of(Items.IRON_INGOT);
            h.assertTrue(inv.getAvailableStacks().get(key)==64,"Physical chest appears once through both links");
            h.assertTrue(inv.extract(key,16,Actionable.SIMULATE,s.source())==16,"Chained simulation succeeds");
            h.assertTrue(chest.countItem(Items.IRON_INGOT)==64,"Simulation leaves real chest unchanged");
            h.assertTrue(inv.extract(key,16,Actionable.MODULATE,s.source())==16,"Chained extraction succeeds");
            h.assertTrue(chest.countItem(Items.IRON_INGOT)==48,"Real chest loses exactly sixteen");
            h.assertTrue(inv.insert(key,8,Actionable.MODULATE,s.source())==8,"Chained insertion succeeds");
            h.assertTrue(chest.countItem(Items.IRON_INGOT)==56,"Real chest gains exactly eight");
            h.runAfterDelay(2,()->{
                h.assertTrue(a.getStorageService().getCachedInventory().get(key)==56,"Source inventory cache updates through chain");
                s.bus().configure(Mode.POWER,false,0,false,List.of());
                h.runAfterDelay(60,()->{
                    h.assertTrue(s.bus().inner.isPowered() && second.outer.isPowered(),"Power still reaches far subnet");
                    h.assertTrue(s.bus().inner.getNode().getUsedChannels()==0 && s.bus().outer.getNode().getUsedChannels()==0,"Power-only link consumes no channels");
                    h.assertTrue(s.left().getGrid().getStorageService().getInventory().getAvailableStacks().get(key)==0,"Power-only link exposes no chest stock");
                    h.assertTrue(chest.countItem(Items.IRON_INGOT)==56,"Mode change preserves physical stock");
                    h.succeed();
                });
            });
        });
    }
    public static void menuControls(GameTestHelper h,net.minecraft.world.entity.player.Player player) {
        var s=place(h,Direction.EAST);
        var pos=s.bus().blockEntity().getBlockPos();
        player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
        var iron=AEItemKey.of(Items.IRON_INGOT);
        var local=new Storage();
        h.runAfterDelay(80,()->{
            s.right().getGrid().getStorageService().addGlobalStorageProvider(m->m.mount(s.storage(),0));
            s.left().getGrid().getStorageService().addGlobalStorageProvider(m->m.mount(local,0));
            s.storage().count.add(iron,64); local.count.add(iron,32);
            s.bus().configure(Mode.SHARED,false,17,false,List.of(iron));
            var menu=new SubnetMenu(SubnetBusMod.MENU.get(),1,player.getInventory(),s.bus());
            h.assertTrue(menu.stillValid(player),"Nearby player can configure menu");
            h.assertTrue(!menu.clickMenuButton(player,999),"Unknown button must be rejected");
            for(int repeat=0;repeat<2;repeat++) h.assertTrue(menu.clickMenuButton(player,SubnetMenu.EXCLUDE),"Explicit exclusion accepted");
            h.assertTrue(menu.exclude() && s.bus().excludeFilter,"Repeated exclusion must not toggle back");
            for(int repeat=0;repeat<2;repeat++) h.assertTrue(menu.clickMenuButton(player,SubnetMenu.INCLUDE),"Explicit inclusion accepted");
            h.assertTrue(!menu.exclude() && !s.bus().excludeFilter,"Repeated inclusion must not toggle back");
            h.assertTrue(menu.clickMenuButton(player,SubnetMenu.SELECT_MODE+Mode.EXTRACT.ordinal()),"Direct extract selection accepted");
            LinkTopology.tick();
            var inv=s.left().getGrid().getStorageService().getInventory();
            h.assertTrue(menu.mode()==Mode.EXTRACT.ordinal(),"Menu sync reflects server mode");
            h.assertTrue(s.bus().mode==Mode.EXTRACT && !s.bus().reversed,"Button selects requested mode without cycling");
            h.assertTrue(menu.clickMenuButton(player,SubnetMenu.SELECT_MODE+Mode.INSERT.ordinal()),"Direct insert selection accepted");
            LinkTopology.tick();
            h.assertTrue(inv.getAvailableStacks().get(iron)==32,"Insert-only hides remote stock but retains local stock");
            h.assertTrue(menu.clickMenuButton(player,SubnetMenu.SELECT_MODE+Mode.SHARED.ordinal()),"Shared selection accepted");
            LinkTopology.tick();
            h.assertTrue(inv.getAvailableStacks().get(iron)==96,"Shared mode exposes exact combined stock");
            h.assertTrue(menu.clickMenuButton(player,SubnetMenu.FLIP),"GUI direction reversal accepted");
            h.runAfterDelay(60,()->{
                menu.broadcastChanges();
                h.assertTrue(menu.stillValid(player) && menu.reversed(),"Menu stays valid and syncs reversal after node rebuild");
                h.assertTrue(s.bus().usable() && s.bus().source().getGrid()==s.right().getGrid(),"Reversal uses adjacent network as source");
                h.assertTrue(s.left().getGrid().getStorageService().getInventory().getAvailableStacks().get(iron)==32,"Reversal removes old exposure");
                h.assertTrue(s.right().getGrid().getStorageService().getInventory().getAvailableStacks().get(iron)==96,"Reversal exposes new target once");
                h.assertTrue(menu.clickMenuButton(player,SubnetMenu.SELECT_MODE+Mode.POWER.ordinal()),"GUI power mode accepted");
                h.runAfterDelay(60,()->{
                    menu.broadcastChanges();
                    h.assertTrue(menu.mode()==Mode.POWER.ordinal() && menu.reversed(),"Power-mode menu state synchronizes");
                    h.assertTrue(s.bus().inner.isPowered() && s.bus().outer.isPowered(),"GUI power selection preserves shared energy");
                    h.assertTrue(s.bus().inner.getGrid()!=s.bus().outer.getGrid(),"GUI never merges grids");
                    h.assertTrue(s.storage().count.get(iron)==64 && local.count.get(iron)==32,"GUI changes never move or duplicate stock");
                    h.assertTrue(s.bus().priority==17 && s.bus().filters.equals(List.of(iron)),"Mode and reversal preserve filters and priority");
                    player.getAbilities().mayBuild=false;
                    h.assertTrue(!menu.clickMenuButton(player,SubnetMenu.FLIP),"Unauthorized menu mutation is rejected");
                    h.assertTrue(s.bus().reversed,"Rejected request leaves direction unchanged");
                    player.getAbilities().mayBuild=true;
                    player.setPos(pos.getX()+30,pos.getY(),pos.getZ());
                    h.assertTrue(!menu.clickMenuButton(player,SubnetMenu.FLIP),"Out-of-range mutation is rejected");
                    player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
                    var host=PartHelper.getPartHost(h.getLevel(),pos);
                    host.removePart(s.bus());
                    h.assertTrue(!menu.clickMenuButton(player,SubnetMenu.FLIP),"Removed part cannot be configured through stale menu");
                    h.succeed();
                });
            });
        });
    }
    public static final class Storage implements MEStorage {
        final KeyCounter count=new KeyCounter();
        IActionSource lastSource;
        @Override public long insert(AEKey key,long amount,Actionable action,IActionSource source) {
            long accepted=Math.min(amount,Long.MAX_VALUE-count.get(key));
            if(action==Actionable.MODULATE) count.add(key,accepted);
            lastSource=source; return accepted;
        }
        @Override public long extract(AEKey key,long amount,Actionable action,IActionSource source) {
            long extracted=Math.min(amount,count.get(key));
            if(action==Actionable.MODULATE) count.remove(key,extracted);
            lastSource=source; return extracted;
        }
        @Override public void getAvailableStacks(KeyCounter out) { out.addAll(count); }
        @Override public Component getDescription() { return Component.literal("Test storage"); }
    }
}

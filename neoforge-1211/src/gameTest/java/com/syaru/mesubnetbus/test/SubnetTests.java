package com.syaru.mesubnetbus.test;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.Direction;
@GameTestHolder("me_subnet_bus_test")
@PrefixGameTestTemplate(false)
public final class SubnetTests {
    @GameTest(template="empty",timeoutTicks=300) public static void menu_controls(GameTestHelper h) { Scenarios.menuControls(h,h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL)); }
    @GameTest(template="empty",timeoutTicks=300) public static void physical_chained_subnet(GameTestHelper h) { Scenarios.physicalChainedSubnet(h); }
    @GameTest(template="empty",timeoutTicks=250) public static void native_storage_bus_parity(GameTestHelper h) { Scenarios.nativeParity(h); }
    @GameTest(template="empty",timeoutTicks=250) public static void reversal_and_detach(GameTestHelper h) { Scenarios.reversalAndDetach(h); }
    @GameTest(template="empty",timeoutTicks=250)
    public static void settings_drop_and_reload(GameTestHelper h) {
        var s=Scenarios.place(h,Direction.EAST);
        h.runAfterDelay(80,()->{
            var bus=s.bus();
            var key=appeng.api.stacks.AEItemKey.of(net.minecraft.world.item.Items.DIAMOND);
            bus.configure(com.syaru.mesubnetbus.core.Mode.EXTRACT,true,Integer.MIN_VALUE,true,java.util.List.of(key));
            var saved=new net.minecraft.nbt.CompoundTag();
            bus.writeToNBT(saved,h.getLevel().registryAccess());
            var restored=com.syaru.mesubnetbus.SubnetBusMod.ITEM.get().createPart();
            restored.readFromNBT(saved,h.getLevel().registryAccess());
            h.assertTrue(restored.mode==bus.mode && restored.reversed && restored.priority==Integer.MIN_VALUE
                    && restored.excludeFilter && restored.filters.equals(bus.filters),"World NBT must restore exact settings");
            var drops=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            bus.addPartDrop(drops,true);
            h.assertTrue(drops.size()==1 && drops.get(0).getCount()==1,"Exactly one configured part must drop");
            var host=appeng.api.parts.PartHelper.getPartHost(h.getLevel(),bus.blockEntity().getBlockPos());
            var pos=bus.blockEntity().getBlockPos();
            host.removePart(bus);
            var replacement=appeng.api.parts.PartHelper.setPart(h.getLevel(),pos,Direction.EAST,null,com.syaru.mesubnetbus.SubnetBusMod.ITEM.get());
            replacement.importSettings(appeng.util.SettingsFrom.DISMANTLE_ITEM,drops.get(0).getComponents(),null);
            h.assertTrue(replacement.mode==com.syaru.mesubnetbus.core.Mode.EXTRACT && replacement.reversed
                    && replacement.priority==Integer.MIN_VALUE && replacement.filters.equals(java.util.List.of(key)),"Dropped-item settings must survive placement");
            h.runAfterDelay(60,()->{
                h.assertTrue(replacement.usable(),"Replacement must reconnect");
                h.succeed();
            });
        });
    }

    @GameTest(template="empty",timeoutTicks=250) public static void direction_north(GameTestHelper h) { Scenarios.direction(h,Direction.NORTH); }
    @GameTest(template="empty",timeoutTicks=250) public static void direction_south(GameTestHelper h) { Scenarios.direction(h,Direction.SOUTH); }
    @GameTest(template="empty",timeoutTicks=250) public static void direction_west(GameTestHelper h) { Scenarios.direction(h,Direction.WEST); }
    @GameTest(template="empty",timeoutTicks=250) public static void direction_east(GameTestHelper h) { Scenarios.direction(h,Direction.EAST); }
    @GameTest(template="empty",timeoutTicks=250) public static void direction_up(GameTestHelper h) { Scenarios.direction(h,Direction.UP); }
    @GameTest(template="empty",timeoutTicks=250) public static void direction_down(GameTestHelper h) { Scenarios.direction(h,Direction.DOWN); }
    @GameTest(template="empty",timeoutTicks=350) public static void modes_filters_rebuild(GameTestHelper h) { Scenarios.modes(h); }
    @GameTest(template="empty",timeoutTicks=350) public static void cycles_and_same_grid(GameTestHelper h) { Scenarios.cycles(h); }
}

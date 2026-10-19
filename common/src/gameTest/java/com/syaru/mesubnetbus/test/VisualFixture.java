package com.syaru.mesubnetbus.test;

import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AEColor;
import appeng.core.definitions.*;
import com.mojang.brigadier.CommandDispatcher;
import com.syaru.mesubnetbus.*;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.commands.*;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Loaded only as a separate development test mod. Never packaged in release jars. */
public final class VisualFixture {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("subnetqa").executes(ctx->{
            var player=ctx.getSource().getPlayerOrException(); var level=player.serverLevel();
            player.setGameMode(GameType.CREATIVE);
            var origin=new BlockPos(0,100,0);
            for(int x=-3;x<=15;x++) for(int z=-3;z<=6;z++) level.setBlockAndUpdate(origin.offset(x,-1,z),Blocks.SMOOTH_STONE.defaultBlockState());
            for(int i=0;i<4;i++) {
                var pos=origin.offset(i*4,0,0);
                PartHelper.setPart(level,pos,null,player,AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
                PartHelper.setPart(level,pos.north(),null,player,AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
                var bus=PartHelper.setPart(level,pos,Direction.NORTH,player,SubnetBusMod.ITEM.get());
                bus.configure(Mode.values()[i],false,0,false,java.util.List.of(AEItemKey.of(Items.IRON_INGOT)));
                level.setBlockAndUpdate(pos.below(),AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
                PartHelper.setPart(level,pos,Direction.SOUTH,player,AEParts.TERMINAL.asItem());
                var chestPos=pos.north(2);
                level.setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());
                ((net.minecraft.world.Container)level.getBlockEntity(chestPos)).setItem(0,new ItemStack(Items.IRON_INGOT,64));
                PartHelper.setPart(level,pos.north(),Direction.NORTH,player,AEParts.STORAGE_BUS.asItem());
                var drop=new ItemStack(SubnetBusMod.ITEM.get());
                var drops=new java.util.ArrayList<ItemStack>(); bus.addPartDrop(drops,true);
                player.getInventory().setItem(i,drops.get(0));
            }
            player.getInventory().setItem(4,AEItems.NETHER_QUARTZ_WRENCH.stack());
            player.getInventory().setItem(5,AEItems.NETWORK_TOOL.stack());
            player.getInventory().setItem(6,new ItemStack(Items.IRON_INGOT,64));
            player.connection.teleport(4.5,100,3.5,180,20);
            return 1;
        }).then(Commands.literal("check").executes(ctx->{
            var player=ctx.getSource().getPlayerOrException();
            for(int i=0;i<4;i++) {
                var part=PartHelper.getPart(player.level(),new BlockPos(i*4,100,0),Direction.NORTH);
                if(!(part instanceof BaseSubnetPart bus)) throw new IllegalStateException("Missing QA bus");
                var source=bus.source(); var target=bus.target();
                long amount=source.getGrid().getStorageService().getInventory().getAvailableStacks().get(AEItemKey.of(Items.IRON_INGOT));
                String result="SUBNET_QA mode="+bus.mode+" split="+(source.getGrid()!=target.getGrid())
                        +" powered="+(source.isPowered() && target.isPowered())+" gridChannels="+source.getGrid().getPathingService().getUsedChannels()
                        +"/"+target.getGrid().getPathingService().getUsedChannels()+" iron="+amount+" status="+bus.status;
                ctx.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(result),false);
                System.getLogger("SubnetQA").log(System.Logger.Level.INFO,result);
            }
            return 1;
        })).then(Commands.literal("menu").executes(ctx->{
            var player=ctx.getSource().getPlayerOrException();
            var part=PartHelper.getPart(player.level(),new BlockPos(4,100,0),Direction.NORTH);
            if(part instanceof BaseSubnetPart bus) bus.openSettings(player);
            return 1;
        })));
    }
}

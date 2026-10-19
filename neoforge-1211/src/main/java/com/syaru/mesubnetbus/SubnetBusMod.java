package com.syaru.mesubnetbus;

import appeng.api.parts.IPartHost;
import appeng.api.parts.PartModels;
import appeng.items.tools.NetworkToolItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import appeng.api.ids.AECreativeTabIds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(SubnetBusMod.ID)
public final class SubnetBusMod {
    public static final String ID="me_subnet_bus";
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ID);
    public static final DeferredHolder<Item,SubnetItem> ITEM=ITEMS.register("subnet_bus",SubnetItem::new);
    public static final DeferredHolder<MenuType<?>,MenuType<SubnetMenu>> MENU=MENUS.register("subnet_bus",
        ()->new MenuType<>((id,inv)->new SubnetMenu(SubnetBusMod.MENU.get(),id,inv,null),FeatureFlags.DEFAULT_FLAGS));
    public SubnetBusMod(IEventBus modBus) {

        ITEMS.register(modBus); MENUS.register(modBus);
        PartModels.registerModels(SubnetPart.MODELS);
        modBus.addListener(this::creative);
        NeoForge.EVENT_BUS.register(SubnetBusMod.class);
        if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) ClientRegistration.register(modBus);
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==AECreativeTabIds.MAIN) event.accept(ITEM.get());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        LinkTopology.tick();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LinkTopology.clear(); }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void right(PlayerInteractEvent.RightClickBlock event) {
        if(event.getHand()!=InteractionHand.MAIN_HAND || event.getEntity().isSpectator()) return;
        var be=event.getLevel().getBlockEntity(event.getPos());
        if(!(be instanceof IPartHost host)) return;
        var selected=host.selectPartWorld(event.getHitVec().getLocation());
        if(!(selected.part instanceof BaseSubnetPart part)) return;
        if(event.getItemStack().getItem() instanceof NetworkToolItem && !event.getEntity().isShiftKeyDown()) {
            part.openSettings(event.getEntity());
        } else if(!part.wrench(event.getEntity(),false)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void left(PlayerInteractEvent.LeftClickBlock event) {
        var be=event.getLevel().getBlockEntity(event.getPos());
        if(!(be instanceof IPartHost host)) return;
        var hit=event.getEntity().pick(6,0,false);
        if(!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(event.getPos())) return;
        var selected=host.selectPartWorld(blockHit.getLocation());
        if(!(selected.part instanceof BaseSubnetPart part)) return;
        var stack=event.getEntity().getMainHandItem();
        if(!appeng.util.InteractionUtil.canWrenchRotate(stack) && !appeng.util.InteractionUtil.canWrenchDisassemble(stack)) return;
        if(event.getAction()==PlayerInteractEvent.LeftClickBlock.Action.START) part.wrench(event.getEntity(),true);
        event.setCanceled(true);
    }
}

package com.syaru.mesubnetbus;
import com.syaru.mesubnetbus.client.SubnetScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
final class ClientRegistration {
    static void register(IEventBus bus) { bus.addListener(ClientRegistration::setup); bus.addListener(ClientRegistration::properties); }
    private static void properties(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(SubnetBusMod.ITEM.get(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SubnetBusMod.ID,"mode"),
                (stack,level,entity,seed)->SubnetItem.modelIndex(stack)));
    }
    private static void setup(RegisterMenuScreensEvent event) {
        event.register(SubnetBusMod.MENU.get(),SubnetScreen::new);
    }
}

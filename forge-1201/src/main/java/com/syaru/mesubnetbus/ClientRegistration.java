package com.syaru.mesubnetbus;
import com.syaru.mesubnetbus.client.SubnetScreen;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
final class ClientRegistration {
    static void register(IEventBus bus) { bus.addListener(ClientRegistration::setup); }
    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->{
            MenuScreens.register(SubnetBusMod.MENU.get(),SubnetScreen::new);
            net.minecraft.client.renderer.item.ItemProperties.register(SubnetBusMod.ITEM.get(),
                    new net.minecraft.resources.ResourceLocation(SubnetBusMod.ID,"mode"),
                    (stack,level,entity,seed)->SubnetItem.modelIndex(stack));
        });
    }
}

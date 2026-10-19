package com.syaru.mesubnetbus.test;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="me_subnet_bus_test", value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class MenuRenderEvents {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.END) MenuRenderSmoke.tick();
    }
}

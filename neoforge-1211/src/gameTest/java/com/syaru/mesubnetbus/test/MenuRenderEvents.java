package com.syaru.mesubnetbus.test;

@net.neoforged.fml.common.EventBusSubscriber(modid="me_subnet_bus_test", value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class MenuRenderEvents {
    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        MenuRenderSmoke.tick();
    }
}

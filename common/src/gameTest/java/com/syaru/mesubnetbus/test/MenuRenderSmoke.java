package com.syaru.mesubnetbus.test;

import com.syaru.mesubnetbus.SubnetBusMod;
import com.syaru.mesubnetbus.SubnetMenu;
import com.syaru.mesubnetbus.client.SubnetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

/** Render-only fixture. Server-authoritative changes are covered by menu_controls. */
public final class MenuRenderSmoke {
    private static int ticks;
    private static int state;
    private static boolean configured;
    private static boolean screenshotPending;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static SubnetScreen screen;
    private static final String RUN = Long.toString(System.currentTimeMillis());

    public static void tick() {
        if (Boolean.getBoolean("subnet.artSmoke")) { ArtRenderSmoke.tick(); return; }
        if (!Boolean.getBoolean("subnet.menuSmoke")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getOverlay() != null) return;
        if (!configured) {
            configured = true;
            mc.options.pauseOnLostFocus = false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            mc.getLanguageManager().setSelected("ja_jp");
            reload = mc.reloadResourcePacks();
            return;
        }
        if (!reload.isDone() || screenshotPending) return;
        if (state >= 8) {
            System.getLogger("SubnetGUI").log(System.Logger.Level.INFO,"GUI_RENDER_SMOKE_COMPLETE run="+RUN);
            mc.stop();
            return;
        }
        if (screen == null) {
            var menu = new SubnetMenu(SubnetBusMod.MENU.get(), 0, mc.player.getInventory(), null);
            menu.setData(0, state / 2);
            int priority = state == 2 ? 0 : state == 3 ? 17 : state % 2 == 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            menu.setData(1, priority & 65535);
            menu.setData(2, priority >>> 16);
            menu.setData(3, state % 2);
            menu.setData(4, state % 2);
            menu.setData(5, state < 2 ? 1 : state == 7 ? 4 : 0);
            menu.getSlot(0).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
            screen = new SubnetScreen(menu, mc.player.getInventory(), Component.translatable("item.me_subnet_bus.subnet_bus"));
            mc.setScreen(screen);
            ticks = 0;
        }
        if (mc.screen != screen || ++ticks < 20) return;
        screenshotPending = true;
        Screenshot.grab(mc.gameDirectory,"subnet-menu-"+RUN+"-"+state+".png",mc.getMainRenderTarget(),message ->
                mc.execute(() -> {
                    System.getLogger("SubnetGUI").log(System.Logger.Level.INFO,message.getString());
                    state++;
                    screen = null;
                    screenshotPending = false;
                }));
    }
}

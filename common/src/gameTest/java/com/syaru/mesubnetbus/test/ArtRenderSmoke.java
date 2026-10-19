package com.syaru.mesubnetbus.test;

import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEParts;
import com.syaru.mesubnetbus.SubnetBusMod;
import com.syaru.mesubnetbus.core.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Development-only item and placed-model rendering, isolated from production worlds. */
public final class ArtRenderSmoke {
    private static int ticks;
    private static int stage;
    private static boolean started;
    private static boolean pending;
    private static final String RUN = Long.toString(System.currentTimeMillis());

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getOverlay() != null || pending) return;
        if (!started) {
            started = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            var server = mc.getSingleplayerServer();
            if (server == null) throw new IllegalStateException("Art fixture requires an isolated integrated server");
            var id = mc.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                var origin = new BlockPos(30,100,0);
                for (int x=-2;x<=9;x++) for (int z=-3;z<=6;z++) {
                    level.setBlockAndUpdate(origin.offset(x,-1,z),Blocks.SEA_LANTERN.defaultBlockState());
                    for(int y=0;y<5;y++) level.setBlockAndUpdate(origin.offset(x,y,z),Blocks.AIR.defaultBlockState());
                }
                for (int i=0;i<8;i++) {
                    var pos = origin.offset((i/2)*2,0,(i%2)*3);
                    PartHelper.setPart(level,pos,null,player,AEParts.GLASS_CABLE.item(AEColor.PURPLE));
                    PartHelper.setPart(level,pos.north(),null,player,AEParts.GLASS_CABLE.item(AEColor.PURPLE));
                    var bus = PartHelper.setPart(level,pos,Direction.NORTH,player,SubnetBusMod.ITEM.get());
                    bus.configure(Mode.values()[i/2],i%2!=0,0,false,java.util.List.of());
                    var drops = new java.util.ArrayList<ItemStack>();
                    bus.addPartDrop(drops,true);
                    System.getLogger("SubnetArt").log(System.Logger.Level.INFO,"server variant="+i+" model="+com.syaru.mesubnetbus.SubnetItem.modelIndex(drops.get(0)));
                    player.getInventory().setItem(i,drops.get(0));
                }
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
                player.connection.teleport(33.5,102,6,180,30);
                level.setDayTime(6000);
            });
        }
        if (++ticks < 100) return;
        if (stage == 0) {
            for(int i=0;i<8;i++) {
                var item=mc.player.getInventory().getItem(i);
                var model=mc.getItemRenderer().getModel(item,mc.level,mc.player,0);
                var faces=model.getQuads(null,null,net.minecraft.util.RandomSource.create()).stream()
                        .filter(q->q.getDirection()==Direction.SOUTH).map(q->q.getSprite().contents().name().toString()).distinct().toList();
                var expected="me_subnet_bus:part/"+(i<2?"power":i%2==0?"storage":"interface");
                if(com.syaru.mesubnetbus.SubnetItem.modelIndex(item)!=i || !faces.contains(expected))
                    throw new IllegalStateException("Wrong item artwork for variant "+i+": "+faces);
                System.getLogger("SubnetArt").log(System.Logger.Level.INFO,"client variant="+i+" model="+com.syaru.mesubnetbus.SubnetItem.modelIndex(item)+" south="+faces);
            }
            mc.setScreen(new Sheet());
            stage++; ticks=0; return;
        }
        if (stage > 2) { mc.stop(); return; }
        pending = true;
        Screenshot.grab(mc.gameDirectory,"subnet-art-"+RUN+"-"+stage+".png",mc.getMainRenderTarget(),message -> mc.execute(() -> {
            System.getLogger("SubnetArt").log(System.Logger.Level.INFO,message.getString());
            mc.setScreen(null);
            stage++; ticks=0; pending=false;
        }));
    }

    private static final class Sheet extends Screen {
        Sheet() { super(Component.literal("ME Subnet Bus")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
            g.fill(0,0,width,height,0xff25282d);
            g.drawCenteredString(font,"ME Subnet Bus / dev.5",width/2,10,0xffeeeeee);
            String[] names={"Power","Shared","Extract","Insert"};
            int start=(width-400)/2;
            for(int mode=0;mode<4;mode++) {
                int x=start+mode*100;
                g.drawCenteredString(font,names[mode],x+50,31,0xffdddddd);
                for(int rev=0;rev<2;rev++) {
                    int y=49+rev*88;
                    var item=minecraft.player.getInventory().getItem(mode*2+rev);
                    g.pose().pushPose(); g.pose().translate(x+18,y,0); g.pose().scale(4,4,4);
                    g.renderItem(item,0,0); g.pose().popPose();
                    g.drawCenteredString(font,rev==0?"Normal":"Reversed",x+50,y+65,0xffaebbc1);
                }
            }
        }
    }
}

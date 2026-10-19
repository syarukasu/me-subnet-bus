package com.syaru.mesubnetbus.test;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
@Mod("me_subnet_bus_test")
public final class TestMod {
    public TestMod(IEventBus bus) {

        bus.addListener((RegisterGameTestsEvent e)->e.register(SubnetTests.class));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::starting);
        if(Boolean.getBoolean("subnet.visual")) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.RegisterCommandsEvent event)->VisualFixture.register(event.getDispatcher()));
    }
    private void starting(ServerStartingEvent e) {
        try {
            var tag=TagParser.parseTag("{size:[9,9,9],entities:[],blocks:[],palette:[{Name:\"minecraft:air\"}]}");
            e.getServer().getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("me_subnet_bus_test","empty"))
                .load(e.getServer().registryAccess().lookupOrThrow(Registries.BLOCK),tag);
            if(System.getProperty("subnet.report")==null) return;
            var path=java.nio.file.Path.of(System.getProperty("subnet.report")).toAbsolutePath();
            java.nio.file.Files.createDirectories(path.getParent());
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(path.toFile()));
        } catch(Exception error) { throw new IllegalStateException(error); }
    }
}

package com.kookykraftmc.lunchbox;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedPlayerList;

import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;


import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.plugin.PluginLoadOrder;

import static java.util.logging.Level.INFO;

import java.util.logging.Logger;

/**
 * Created by TimeTheCat on 4/28/2016.
 */
@Mod(name = "LunchBox", modid = "lunchbox", serverSideOnly = true, version = "0.0.1")
public class LunchBox {
    public static boolean craftWorldLoading = false;
    public Logger logger = Logger.getLogger("lunchbox");
    private static CraftServer server;
    public World[] worlds;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) throws Exception {
        this.server = new CraftServer(MinecraftServer.getServer(), (DedicatedPlayerList) MinecraftServer.getServer().getConfigurationManager().playerEntityList);
        //todo: will work on registering events later...
        //logger.log(INFO, "Registering events...");

        logger.log(INFO, "Loading plugins...");
        server.loadPlugins();
    }

    @Mod.EventHandler
    public void onAboutStart(FMLServerAboutToStartEvent event) throws Exception {
        logger.log(INFO, "Startup enabling plugins...");
        server.enablePlugins(PluginLoadOrder.STARTUP);
        /*
        int w;
        int g = 0;
        w = MinecraftServer.getServer().worldServers.length;
        WorldServer[] ws = MinecraftServer.getServer().worldServers;
        while (w <= g) {
            CraftWorld world = new CraftWorld(ws[w], new , CraftWorld.Environment.getEnvironment(ws[w].getWorldType().getWorldTypeID()))
        }
        */
    }

    @Mod.EventHandler
    public void onStarting(FMLServerStartingEvent event) throws Exception {
        logger.log(INFO, "Postworld enabling plugins...");
        server.enablePlugins(PluginLoadOrder.POSTWORLD);
    }

    @Mod.EventHandler
    public void onStopping(FMLServerStoppingEvent event) throws Exception {
        logger.log(INFO, "Disabling plugins...");
        server.disablePlugins();
    }

    public static CraftServer getServer() {
        return server;
    }
}

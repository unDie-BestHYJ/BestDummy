package com.plugins.bestdummy;

import com.plugins.bestdummy.command.BestDummyCommand;
import com.plugins.bestdummy.listener.PlayerJoinListener;
import com.plugins.bestdummy.manager.DummyManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BestDummy extends JavaPlugin {

    private DummyManager dummyManager;

    @Override
    public void onEnable() {
        this.dummyManager = new DummyManager(this);

        getCommand("bestdummy").setExecutor(new BestDummyCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        dummyManager.loadDummies();
    }

    @Override
    public void onDisable() {
        dummyManager.saveDummies();
    }

    public DummyManager getDummyManager() {
        return dummyManager;
    }
}

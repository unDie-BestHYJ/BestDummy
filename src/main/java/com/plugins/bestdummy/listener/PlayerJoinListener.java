package com.plugins.bestdummy.listener;

import com.plugins.bestdummy.BestDummy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final BestDummy plugin;

    public PlayerJoinListener(BestDummy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int dummyCount = plugin.getDummyManager().getDummyCount();
        player.sendMessage("欢迎来到服务器！当前假人人数: " + dummyCount);
    }
}

package com.plugins.bestdummy.manager;

import com.plugins.bestdummy.BestDummy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DummyManager {

    private final BestDummy plugin;  // 插件实例，用于访问插件的功能和配置
    private final List<ArmorStand> dummies;  // 存储所有创建的假人实体
    private final ScoreboardManager manager;  // 处理 Minecraft 的计分板管理器
    private final Scoreboard scoreboard;  // 主要的计分板对象，用于管理团队和得分
    private Team dummyTeam;  // 用于假人的计分板团队，用于控制假人在玩家列表中的显示
    private final File dummiesFile;  // 存储假人信息的配置文件
    private final FileConfiguration dummiesConfig;  // 配置文件的配置对象，用于读取和写入假人信息
    private final Map<ArmorStand, String> dummyOwners = new HashMap<>();

    public DummyManager(BestDummy plugin) {
        this.plugin = plugin;
        this.dummies = new ArrayList<>();
        this.manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager.getMainScoreboard();

        this.dummyTeam = scoreboard.getTeam("dummies");
        if (this.dummyTeam == null) {
            Bukkit.getLogger().info("已注册新Team dummies");
            this.dummyTeam = scoreboard.registerNewTeam("dummies");
        }

        this.dummyTeam.setPrefix("假人 ");

        this.dummiesFile = new File(plugin.getDataFolder(), "dummies.yml");
        if (!dummiesFile.exists()) {
            try {
                dummiesFile.getParentFile().mkdirs();
                dummiesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.dummiesConfig = YamlConfiguration.loadConfiguration(dummiesFile);
    }

    public void loadDummies() {
        if (dummiesConfig.contains("dummies")) {
            for (String dummyName : dummiesConfig.getConfigurationSection("dummies").getKeys(false)) {
                String world = dummiesConfig.getString("dummies." + dummyName + ".world");
                double x = dummiesConfig.getDouble("dummies." + dummyName + ".x");
                double y = dummiesConfig.getDouble("dummies." + dummyName + ".y");
                double z = dummiesConfig.getDouble("dummies." + dummyName + ".z");

                Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                ArmorStand dummy = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                dummy.setCustomName(dummyName);
                dummy.setCustomNameVisible(true);
                dummy.setVisible(false);
                dummy.setInvulnerable(true);
                dummy.setGravity(false);
                dummy.setBasePlate(false);
                dummy.setArms(false);

                dummies.add(dummy);
                Bukkit.getLogger().info("已加载假人: " + dummy.getCustomName());
                dummyTeam.addEntry(dummy.getCustomName());

                updateDummyAndPlayerCount(dummyName);
            }
        }
    }

    public void saveDummies() {
        for (ArmorStand dummy : dummies) {
            Location loc = dummy.getLocation();
            String playerName = dummyOwners.getOrDefault(dummy, "unknown");
            saveDummyInfo(dummy, loc, playerName);
        }
        saveDummiesConfig();
    }

    public ArmorStand createDummy(String id, Location loc, String playerName) {
        ArmorStand dummy = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        dummy.setCustomName(id);
        dummy.setCustomNameVisible(true);
        dummy.setVisible(false);
        dummy.setInvulnerable(true);
        dummy.setGravity(false);
        dummy.setBasePlate(false);
        dummy.setArms(false);

        dummies.add(dummy);
        dummyTeam.addEntry(dummy.getCustomName());
        dummyOwners.put(dummy, playerName);

        saveDummyInfo(dummy, loc, playerName);
        updateDummyAndPlayerCount(id);
        return dummy;
    }

    public boolean removeDummy(String id) {
        ArmorStand dummyToRemove = null;
        for (ArmorStand dummy : dummies) {
            if (dummy.getCustomName().equals(id)) {
                dummyToRemove = dummy;
                break;
            }
        }

        if (dummyToRemove != null) {
            dummyToRemove.remove();
            dummies.remove(dummyToRemove);
            dummyTeam.removeEntry(id);
            dummiesConfig.set("dummies." + id, null);
            saveDummiesConfig();
            updateDummyAndPlayerCount(null);
            return true;
        }
        return false;
    }

    public void removeAllDummies() {
        for (ArmorStand dummy : dummies) {
            dummy.remove();
        }
        dummies.clear();
        dummyTeam.getEntries().clear();
        dummiesConfig.set("dummies", null);
        saveDummiesConfig();
        updateDummyAndPlayerCount(null);
    }

    private void saveDummyInfo(ArmorStand dummy, Location loc, String playerName) {
        String path = "dummies." + dummy.getCustomName();
        dummiesConfig.set(path + ".world", loc.getWorld().getName());
        dummiesConfig.set(path + ".x", loc.getX());
        dummiesConfig.set(path + ".y", loc.getY());
        dummiesConfig.set(path + ".z", loc.getZ());
        dummiesConfig.set(path + ".player", playerName);
        saveDummiesConfig();
    }

    private void saveDummiesConfig() {
        try {
            dummiesConfig.save(dummiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDummyAndPlayerCount(String dummyName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                int dummyCount = dummies.size();
                dummyTeam.setPrefix("假人 " + dummyCount + " ");

                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.setScoreboard(scoreboard);
                });
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L);
    }

    public int getDummyCount() {
        return dummies.size();
    }
}

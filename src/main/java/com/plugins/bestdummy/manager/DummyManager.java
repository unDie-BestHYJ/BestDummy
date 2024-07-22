package com.plugins.bestdummy.manager;

import com.plugins.bestdummy.BestDummy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
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
            for (String key : dummiesConfig.getConfigurationSection("dummies").getKeys(false)) {
                String world = dummiesConfig.getString("dummies." + key + ".world");
                double x = dummiesConfig.getDouble("dummies." + key + ".x");
                double y = dummiesConfig.getDouble("dummies." + key + ".y");
                double z = dummiesConfig.getDouble("dummies." + key + ".z");

                // 创建一个 Location 对象，指定假人将要生成的位置
                Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                // 在指定位置生成一个 ArmorStand 实体，并将其强制转换为 ArmorStand 类型
                ArmorStand dummy = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                // 设置假人的自定义名称
                dummy.setCustomName(key);
                // 设置假人名称在玩家视野中可见
                dummy.setCustomNameVisible(true);
                // 设置假人不可见（即隐藏其身体）
                dummy.setVisible(false);
                // 设置假人无敌（即不会受到伤害）
                dummy.setInvulnerable(true);
                // 取消假人的重力效果（即不会受到重力影响）
                dummy.setGravity(false);
                // 取消假人的基座（即底座板）显示
                dummy.setBasePlate(false);
                // 取消假人的手臂显示（即不显示手臂）
                dummy.setArms(false);

                dummies.add(dummy);
                dummyTeam.addEntry(dummy.getCustomName());

                updatePlayerCount(key);
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
        updatePlayerCount(id);
        return dummy;
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

    private void updatePlayerCount(String dummyName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // 获取当前假人的数量
                int dummyCount = dummies.size();

                // 更新假人名称显示在 Tab 列表中的计分板团队
                if (dummyTeam != null) {
                    dummyTeam.setPrefix(dummyName + ": " + dummyCount + " ");
                }

                // 更新所有在线玩家的计分板，以确保他们看到最新的假人信息
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.setScoreboard(scoreboard);
                });
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L); // 每分钟更新一次
    }


    public int getDummyCount() {
        return dummies.size();
    }

}

package com.plugins.bestdummy.manager;

import com.plugins.bestdummy.BestDummy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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
    private List<ArmorStand> dummies;  // 存储所有创建的假人实体
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
        // 检查是否已经存在相同 ID 的盔甲架
        for (ArmorStand dummy : dummies) {
            if (dummy.getCustomName().equals(id)) {
                return null; // 如果 ID 已经存在，返回 null
            }
        }

        // 调整位置以确保盔甲架生成在方块顶部
        Location adjustedLocation = loc.clone().add(0.5, 0, 0.5); // 方块中心
        adjustedLocation.setY(adjustedLocation.getBlockY() + 1); // 设定到方块顶部

        // 创建新的盔甲架
        ArmorStand dummy = (ArmorStand) loc.getWorld().spawnEntity(adjustedLocation, EntityType.ARMOR_STAND);
        dummy.setCustomName(id); // 设置自定义名称
        dummy.setCustomNameVisible(true); // 显示自定义名称
        dummy.setVisible(true); // 确保盔甲架可见
        dummy.setInvulnerable(true); // 盔甲架不可被破坏
        dummy.setGravity(false); // 盔甲架不受重力影响
        dummy.setBasePlate(true); // 显示底座
        dummy.setArms(true); // 显示手臂

        // 将盔甲架添加到列表和计分板中
        dummies.add(dummy);
        dummyTeam.addEntry(dummy.getCustomName());
        dummyOwners.put(dummy, playerName);

        // 保存盔甲架信息并更新计数
        saveDummyInfo(dummy, adjustedLocation, playerName);
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
            Location location = dummyToRemove.getLocation();
            dummyToRemove.remove();
            dummies.remove(dummyToRemove);
            dummyTeam.removeEntry(id);
            dummiesConfig.set("dummies." + id, null);

            // 删除周围半径1个方块区域的盔甲架
            String command = String.format("kill @e[type=armor_stand,x=%.2f,y=%.2f,z=%.2f,r=1]",
                    location.getX(), location.getY(), location.getZ());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            saveDummiesConfig();
            updateDummyAndPlayerCount(null);
            return true;
        }
        return false;
    }

    public void removeAllDummies() {
        // 创建一个可变的副本
        List<ArmorStand> dummiesToRemove = new ArrayList<>(dummies);

        // 删除所有假人
        for (ArmorStand dummy : dummiesToRemove) {
            Location location = dummy.getLocation();
            dummy.remove();

            // 删除周围半径1个方块区域的盔甲架
            String command = String.format("kill @e[type=armor_stand,x=%.2f,y=%.2f,z=%.2f,r=1]",
                    location.getX(), location.getY(), location.getZ());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        // 清空假人列表
        dummies.clear();

        // 使用迭代器来移除所有条目，避免对不可变集合的操作
        for (String entry : new ArrayList<>(dummyTeam.getEntries())) {
            dummyTeam.removeEntry(entry);
        }

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

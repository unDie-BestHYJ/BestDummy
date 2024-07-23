package com.plugins.bestdummy.command;

import com.plugins.bestdummy.BestDummy;
import com.plugins.bestdummy.manager.DummyManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;

public class BestDummyCommand implements CommandExecutor {

    private final BestDummy plugin;

    public BestDummyCommand(BestDummy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("此指令只能由玩家执行");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("用法: /bestdummy <subcommand> <其它参数>");
            return true;
        }

        Player player = (Player) sender;
        DummyManager dummyManager = plugin.getDummyManager();

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage("用法: /bestdummy create <假人英文id>");
                    return true;
                }

                Block targetBlock = player.getTargetBlock(null, 100);
                if (targetBlock.getType() == Material.AIR) {
                    player.sendMessage("请指向一个方块");
                    return true;
                }

                ArmorStand dummy = dummyManager.createDummy(args[1], targetBlock.getLocation(), player.getName());

                if (dummy != null) {
                    player.sendMessage("假人 " + args[1] + " 已生成");
                } else {
                    player.sendMessage("假人 " + args[1] + " 已存在, 生成失败");
                }
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage("用法: /bestdummy remove <假人英文id>");
                    return true;
                }
                boolean removed = dummyManager.removeDummy(args[1]);
                if (removed) {
                    player.sendMessage("假人 " + args[1] + " 已删除");
                } else {
                    player.sendMessage("假人 " + args[1] + " 不存在");
                }
                break;

            case "removeall":
                dummyManager.removeAllDummies();
                player.sendMessage("所有假人已删除");
                break;

            default:
                player.sendMessage("未知子命令。用法: /bestdummy <create|remove|removeall>");
                break;
        }

        return true;
    }
}

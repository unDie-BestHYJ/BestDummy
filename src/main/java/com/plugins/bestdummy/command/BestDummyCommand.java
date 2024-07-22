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

        if (args.length != 1) {
            sender.sendMessage("用法: /bestdummy <假人英文id>");
            return true;
        }

        Player player = (Player) sender;
        // null: 第一个参数（transparent）通常是一个 Set<Material>，用于指定视线中可以穿透的方块。如果传入 null，表示没有特别指定的可穿透方块，默认行为是允许穿透所有方块。
        // 100: 第二个参数（maxDistance）是一个整数，表示从玩家视线起点开始，最多可以检测的距离。这里设置为 100，表示最多检测 100 个方块距离内的方块。
        Block targetBlock = player.getTargetBlock(null, 100);
        Location targetLocation = targetBlock.getLocation().add(0, 1, 0);

        if (targetBlock.getType() == Material.AIR) {
            player.sendMessage("请指向一个方块");
            return true;
        }

        DummyManager dummyManager = plugin.getDummyManager();
        ArmorStand dummy = dummyManager.createDummy(args[0], targetLocation, player.getName());

        if (dummy != null) {
            player.sendMessage("假人 " + args[0] + " 已生成");
        } else {
            player.sendMessage("假人生成失败");
        }

        return true;
    }
}

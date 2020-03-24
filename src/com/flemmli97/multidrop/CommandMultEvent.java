package com.flemmli97.multidrop;

import java.util.Collections;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.flemmli97.multidrop.MultiDrop.DropEvent;
import com.google.common.collect.Lists;

public class CommandMultEvent implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String al, String[] args) {
		if(args.length > 0){
			if(args[0].equals("get")){
				if(sender instanceof Player) {
					DropEvent event = MultiDrop.inst.currentEvent();
					if(event == null){
						((Player) sender).sendMessage(MultiDrop.getNone);
					}else
						((Player) sender).sendMessage(String.format(MultiDrop.getEvent, event.getEventMultiplier() + 1, event.secondsLeft()));
				}
				return true;
			}else if(sender.isOp() || sender.hasPermission("multidrop.event")){
				if(args[0].equals("start") && args.length > 2){
					if(MultiDrop.inst.currentEvent() == null){
						double mult = Double.parseDouble(args[1]) - 1;
						int sec = Integer.parseInt(args[2]);
						MultiDrop.inst.setEvent(new DropEvent(mult, sec).assignTask(new EventRunnable().runTaskTimer(MultiDrop.inst, 0, 20)));
						sender.getServer().broadcastMessage(String.format(MultiDrop.startEvent, sender.getName(), mult + 1, sec));
						sender.getServer().getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1));
						return true;
					}else{
						if(sender instanceof Player)
							((Player) sender).sendMessage(MultiDrop.runningEvent);
						return true;
					}
				}else if(args[0].equals("stop")){
					if(MultiDrop.inst.currentEvent() != null){
						MultiDrop.inst.currentEvent().trackedTask().cancel();
						MultiDrop.inst.setEvent(null);
						sender.getServer().broadcastMessage(MultiDrop.stopEvent);
						return true;
					}else{
						if(sender instanceof Player)
							((Player) sender).sendMessage(MultiDrop.getNone);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(sender.isOp() && args.length == 1)
			return Lists.newArrayList("start", "stop", "get");
		else if(args.length == 1)
			return Lists.newArrayList("get");
		return Collections.emptyList();
	}

}

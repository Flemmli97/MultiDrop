package com.flemmli97.multidrop;

import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

//https://github.com/Flemmli97/MultiDrop
public class EventRunnable extends BukkitRunnable {

	@Override
	public void run() {
		MultiDrop.DropEvent event = MultiDrop.inst.currentEvent();
		if(event == null){
			this.cancel();
			return;
		}
		if(event.secondsLeft() <= 0){
			event.trackedTask().cancel();
			MultiDrop.inst.setEvent(null);
			MultiDrop.inst.getServer().broadcastMessage(MultiDrop.endEvent);
			MultiDrop.inst.getServer().getOnlinePlayers().forEach(player -> {
				player.sendTitle(MultiDrop.endEvent, "", 10, 50, 20);
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
			});
		}
		event.decreaseTime();
		if(event.secondsLeft() == 300 || event.secondsLeft() == 600 || event.secondsLeft() == 60){
			MultiDrop.inst.getServer().getOnlinePlayers().forEach(player -> {
				player.sendTitle("§6Drop Multiplier", String.format(MultiDrop.timeEvent, event.secondsLeft()), 7, 50, 15);
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
			});
		}
	}

}

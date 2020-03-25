package com.flemmli97.multidrop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.Maps;

public class MultiDrop extends JavaPlugin {

	public static MultiDrop inst;
	public final Logger log = Logger.getLogger("MultiDrop");

	public final FileConfiguration config = this.getConfig();

	public Map<String, PlayerPlaceHandler> place = Maps.newHashMap();

	private Map<String, Double> multMap = Maps.newHashMap();

	private DropEvent event;

	public static String startEvent;
	public static String stopEvent;
	public static String runningEvent;
	public static String getNone;
	public static String getEvent;
	public static String endEvent;
	public static String timeEvent;

	@Override
	public void onEnable() {
		inst = this;
		this.event = null;
		
		this.getServer().getPluginManager().registerEvents(new BlockHandler(), this);
		this.getCommand("multidrop").setExecutor(new CommandMultEvent());

		try{
			File file = new File(this.getDataFolder(), "dropEvent");
			if(!file.exists())
				return;
			DataInputStream data = new DataInputStream(new FileInputStream(file));
			if(data.readByte() != 0){
				int sec = data.readInt();
				double mult = data.readDouble();
				this.event = new DropEvent(mult, sec).assignTask(new EventRunnable().runTaskTimer(this, 0, 20));
			}
			data.close();
		}catch(IOException e){
			log.log(Level.WARNING, "Error writing event data");
			e.printStackTrace();
		}

		this.getServer().getWorlds().forEach(world -> {
			PlayerPlaceHandler handl = new PlayerPlaceHandler();
			handl.load(world);
			this.place.put(world.getName(), handl);
		});
		
		this.reloadConfig();
		this.config.getStringList("Drops").forEach(s -> {
			String[] ss = s.split("-");
			this.multMap.put(ss[0], Double.parseDouble(ss[1]));
		});
		startEvent = this.config.getString("event.start");
		stopEvent = this.config.getString("event.stop");
		runningEvent = this.config.getString("event.run");
		getNone = this.config.getString("event.get.none");
		getEvent = this.config.getString("event.get");
		endEvent = this.config.getString("event.end");
		timeEvent = this.config.getString("event.time.get");

		this.saveDefaultConfig();
	}

	@Override
	public void onDisable() {
		try{
			File file = new File(this.getDataFolder(), "dropEvent");
			if(!file.exists())
				file.createNewFile();
			DataOutputStream data = new DataOutputStream(new FileOutputStream(file));
			if(this.event != null){
				data.writeByte(1);
				data.writeInt(this.event.sec);
				data.writeDouble(this.event.multiplier);
			}else
				data.writeByte(0);
			data.close();
		}catch(IOException e){
			log.log(Level.WARNING, "Error reading saved event data");
			e.printStackTrace();
		}
		this.getServer().getWorlds().forEach(world -> {
			PlayerPlaceHandler handl = this.place.get(world.getName());
			handl.save(world);
		});
		
	}

	public PlayerPlaceHandler fromWorld(World world) {
		PlayerPlaceHandler handl = this.place.get(world.getName());
		if(handl == null)
			log.log(Level.WARNING, "Nullpointer for " + world.getName() + " in map " + this.place);
		return handl;
	}

	public boolean shouldModify(Block block) {
		return this.multMap.containsKey(block.getType().getKey().toString());
	}

	public boolean shouldModify(String block) {
		return this.multMap.containsKey(block);
	}

	public double getDropMultiplier(String block) {
		return this.multMap.getOrDefault(block, 1.0) - 1;
	}

	public void setEvent(DropEvent event) {
		this.event = event;
	}

	@Nullable
	public DropEvent currentEvent() {
		return this.event;
	}

	public static class DropEvent {

		private double multiplier;
		private int sec;

		private BukkitTask eventTask;

		public DropEvent(double mult, int sec) {
			this.multiplier = mult;
			this.sec = sec;
		}

		public DropEvent assignTask(BukkitTask task) {
			this.eventTask = task;
			return this;
		}

		public BukkitTask trackedTask() {
			return this.eventTask;
		}

		public void decreaseTime() {
			this.sec--;
		}

		public int secondsLeft() {
			return this.sec;
		}

		public double getEventMultiplier() {
			return this.multiplier;
		}

		public void setEventMultiplier(double mult) {
			this.multiplier = mult;
		}
	}
}

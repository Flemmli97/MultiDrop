package com.flemmli97.multidrop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.LongSet;

import com.google.common.collect.Maps;

public class PlayerPlaceHandler {

	private static final int NUM_XZ_BITS = (int) (1 + Math.ceil(Math.log(30000000) / Math.log(2)));//26
	private static final int NUM_Y_BITS = 64 - 2 * NUM_XZ_BITS;
	private static final int Y_SHIFT = 0 + NUM_XZ_BITS;
	private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
	private static final long XZ_MASK = (1L << NUM_XZ_BITS) - 1L;
	private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
	private Long2ObjectMap<LongSet> map = new Long2ObjectOpenHashMap<LongSet>();
	private Map<Location, String> brokenBlocks = Maps.newHashMap();

	public boolean isPlayerPlaced(Block block) {
		LongSet set = map.get(fromChunk(block.getChunk()));
		if(set != null){
			return set.contains(fromPos(block.getX(), block.getY(), block.getZ()));
		}
		return false;
	}

	public void tryBreakBlock(Block block) {
		this.brokenBlocks.put(block.getLocation(), block.getType().getKey().toString());
	}

	public String getBrokenBlock(Location loc) {
		return this.brokenBlocks.getOrDefault(loc, "");
	}

	public void finishBreakingBlock(Block block) {
		this.brokenBlocks.remove(block.getLocation());
	}

	public void addPlayerPlacedBlock(Block block) {
		this.map.compute(fromChunk(block.getChunk()), (v, k) -> k == null ? add(new LongOpenHashSet(), fromPos(block.getX(), block.getY(), block.getZ())) : add(k, fromPos(block.getX(), block.getY(), block.getZ())));
	}

	public void removePlayerPlacedBlock(Block block) {
		LongSet set = map.get(fromChunk(block.getChunk()));
		if(set != null){
			set.remove(fromPos(block.getX(), block.getY(), block.getZ()));
		}
	}

	public void save(World world) {
		try{
			File file = new File(world.getWorldFolder(), "placedBlocks");
			if(!file.exists())
				file.createNewFile();
			DataOutputStream data = new DataOutputStream(new FileOutputStream(file));
			this.map.forEach((key, set) -> {
				if(!set.isEmpty())
					try{
						data.writeByte(1);
						data.writeInt(set.size());
						data.writeLong(key);
						for(long l : set){
							data.writeLong(l);
						}
					}catch(IOException e){
						e.printStackTrace();
					}
			});
			data.writeByte(0);
			data.close();
		}catch(IOException e){
			MultiDrop.inst.log.log(Level.WARNING, "Error saving placed block data");
			e.printStackTrace();
		}
	}

	public void load(World world) {
		this.map.clear();
		try{
			File file = new File(world.getWorldFolder(), "placedBlocks");
			if(!file.exists())
				return;
			DataInputStream data = new DataInputStream(new FileInputStream(file));
			while(data.readByte() != 0){
				int size = data.readInt();
				long key = data.readLong();
				LongSet set = new LongOpenHashSet();
				for(int i = 0; i < size; i++)
					set.add(data.readLong());
				this.map.put(key, set);
			}
			data.close();
		}catch(IOException e){
			MultiDrop.inst.log.log(Level.WARNING, "Error reading placed block data");
			e.printStackTrace();
		}
	}

	private static long fromChunk(Chunk chunk) {
		return (long) chunk.getX() & 0xFFFFFFFFL | ((long) chunk.getZ() & 0xFFFFFFFFL) << 32;
	}

	private static long fromPos(int x, int y, int z) {
		return ((long) x & XZ_MASK) << X_SHIFT | ((long) y & Y_MASK) << Y_SHIFT | ((long) z & XZ_MASK) << 0;
	}

	private static LongSet add(LongSet set, long l) {
		set.add(l);
		return set;
	}
}

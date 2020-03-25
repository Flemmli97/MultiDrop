package com.flemmli97.multidrop;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Maps;

public class BlockHandler implements Listener {

	@EventHandler
	public void onDrops(BlockDropItemEvent event) {
		PlayerPlaceHandler placeHandler = MultiDrop.inst.fromWorld(event.getBlock().getWorld());
		String brokenBlock = placeHandler.getBrokenBlock(event.getBlock().getLocation());
		if(MultiDrop.inst.shouldModify(brokenBlock) && !placeHandler.isPlayerPlaced(event.getBlock())){
			Map<ItemStack, Integer> unique = Maps.newHashMap();
			for(Item item : event.getItems()){
				ItemStack stack = item.getItemStack();
				unique.compute(stack.clone(), (k, v) -> v == null ? stack.getAmount() : v + stack.getAmount());
			}
			for(Entry<ItemStack, Integer> entry : unique.entrySet()){
				ItemStack stack = entry.getKey();
				double eventMult = MultiDrop.inst.currentEvent() != null ? MultiDrop.inst.currentEvent().getEventMultiplier() : 1;
				stack.setAmount((int) (entry.getValue() * MultiDrop.inst.getDropMultiplier(brokenBlock) * eventMult));
				event.getPlayer().getWorld().dropItemNaturally(event.getBlock().getLocation(), stack);
			}
			placeHandler.finishBreakingBlock(event.getBlock());
		}
		placeHandler.removePlayerPlacedBlock(event.getBlock());
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if(MultiDrop.inst.shouldModify(event.getBlock())){
			MultiDrop.inst.fromWorld(event.getBlock().getWorld()).tryBreakBlock(event.getBlock());
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if(MultiDrop.inst.shouldModify(event.getBlockPlaced())){
			MultiDrop.inst.fromWorld(event.getBlock().getWorld()).addPlayerPlacedBlock(event.getBlockPlaced());
		}
	}

	@EventHandler
	public void pistonExtend(BlockPistonExtendEvent event) {
		PlayerPlaceHandler placeHandler = MultiDrop.inst.fromWorld(event.getBlock().getWorld());
		event.getBlocks().forEach(block -> {
			if(placeHandler.isPlayerPlaced(block)){
				placeHandler.removePlayerPlacedBlock(block);
				placeHandler.addPlayerPlacedBlock(block.getRelative(event.getDirection()));
			}
		});
	}

	@EventHandler
	public void pistonRetract(BlockPistonRetractEvent event) {
		PlayerPlaceHandler placeHandler = MultiDrop.inst.fromWorld(event.getBlock().getWorld());
		event.getBlocks().forEach(block -> {
			if(placeHandler.isPlayerPlaced(block)){
				placeHandler.removePlayerPlacedBlock(event.getBlock());
				placeHandler.addPlayerPlacedBlock(block.getRelative(event.getDirection()));
			}
		});
	}
}

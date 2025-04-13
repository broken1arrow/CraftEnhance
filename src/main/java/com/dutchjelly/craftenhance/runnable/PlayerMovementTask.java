package com.dutchjelly.craftenhance.runnable;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.runnable.BrewingTask.BrewingLogic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class PlayerMovementTask implements Runnable {
	private final CraftEnhance plugin = self();
	private final Map<Location, BrewingLogic> map = new HashMap<>();
	private BukkitTask task;

	public void start() {
		task = Bukkit.getScheduler().runTaskTimer(this.plugin, this, 20, 20 * 2);
	}

	public void run() {

		for (Player player : Bukkit.getOnlinePlayers()) {
			final Block block = player.getTargetBlock(null, 20);
			if (block.getType() == Material.BREWING_STAND) {
				Map<Location, BrewingLogic> brewCache = self().getBrewingTask().getMap();
				BrewingLogic brewing = brewCache.get(block.getLocation());
				if (brewing != null) {
					Messenger.Message("progress: " + (brewing.getRemainingMillis() / 1000), player);
				}

			}
		}

	}


}

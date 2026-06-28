package com.dutchjelly.craftenhance.runnable;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.runnable.BrewingTask.BrewingLogic;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class PlayerCheckTask implements Runnable {
	private final CraftEnhance plugin = self();
	private final Map<Location, BrewingLogic> map = new HashMap<>();
	private BukkitTask task;

	public void start() {
		final FileConfiguration config = plugin.getConfig();
		if(!config.getBoolean("enable-brewing-progressbar") || !config.getBoolean("enable-brewing-recipes"))
			return;
		task = Bukkit.getScheduler().runTaskTimer(this.plugin, this, 20, 20 * 2);
	}

	public void cancel() {
		if (this.isRunning()) {
			Bukkit.getScheduler().cancelTask(task.getTaskId());
		}
	}

	public boolean isRunning() {
		if (task == null) return false;
		final int taskId = task.getTaskId();
		return taskId > 0 &&
				(Bukkit.getScheduler().isCurrentlyRunning(taskId) ||
						Bukkit.getScheduler().isQueued(taskId));
	}

	public void run() {
		final FileConfiguration config = plugin.getConfig();
		if(!config.getBoolean("enable-brewing-progressbar") || !config.getBoolean("enable-brewing-recipes")) {
			Bukkit.getScheduler().cancelTask(task.getTaskId());
			return;
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			final Block block = player.getTargetBlock(null, 10);
			if (block.getType() == Material.BREWING_STAND) {
				Map<Location, BrewingLogic> brewCache = self().getBrewingTask().getMap();
				BrewingLogic brewing = brewCache.get(block.getLocation());
				if (brewing != null) {
					sendActionBar(player,"Brewing: ", (int) (brewing.getRemainingMillis() / 1000),brewing.getBrewingRecipe().getDuration());
				}
			}
		}
	}

	public void sendActionBar(Player player, String message, int current, int total) {
		final VersionChecker versionChecker = self().getVersionChecker();

		final int progress = (int) ((current / (double) total) * 100);
		final String progressBar = this.createProgressBar(progress);
		message = message + progressBar + " " + progress + "%";

		if (versionChecker.newerThan(ServerVersion.v1_12)) {
			player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
			return;
		}
		try {
			String version = getServerVersion();

			Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);

			Class<?> chatComponentTextClass = Class.forName("net.minecraft.server." + version + ".ChatComponentText");
			Object chatComponent = chatComponentTextClass.getConstructor(String.class).newInstance(message);

			Class<?> iChatBaseComponent = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");

			Object packet;
			Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");

			if (versionChecker.newerThan(ServerVersion.v1_8)) {
				// Use ChatMessageType enum
				Class<?> chatMessageTypeClass = Class.forName("net.minecraft.server." + version + ".ChatMessageType");
				Object chatMessageType = Arrays.stream(chatMessageTypeClass.getEnumConstants())
						.filter(e -> e.toString().equals("GAME_INFO")) // ActionBar
						.findFirst()
						.orElse(null);

				packet = packetPlayOutChatClass
						.getConstructor(iChatBaseComponent, chatMessageTypeClass)
						.newInstance(chatComponent, chatMessageType);
			} else {
				packet = packetPlayOutChatClass
						.getConstructor(iChatBaseComponent, byte.class)
						.newInstance(chatComponent, (byte) 2);
			}

			Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".Packet");
			playerConnection.getClass().getMethod("sendPacket", packetClass).invoke(playerConnection, packet);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private String createProgressBar(int percentage) {
		final int totalBlocks = 20; // Length of the progress bar
		final int filledBlocks = (int) (totalBlocks * (percentage / 100.0));
		final StringBuilder bar = new StringBuilder();

		for (int i = 0; i < filledBlocks; i++)
			bar.append(ChatColor.GREEN).append("█");
		for (int i = filledBlocks; i < totalBlocks; i++)
			bar.append(ChatColor.RED).append("█");

		return bar.toString();
	}

	private String getServerVersion() {
		return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	}



}

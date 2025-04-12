package com.dutchjelly.craftenhance.runnable;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.crafthandling.recipes.BrewingRecipe;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Debug.Type;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static com.dutchjelly.craftenhance.CraftEnhance.self;


public class BrewingTask implements Runnable {
	private BukkitTask task;
	private final CraftEnhance plugin = self();
	private final Map<Location, BrewingLogic> map = new HashMap<>();

	public void start() {
		task = Bukkit.getScheduler().runTaskTimer(this.plugin, this, 20, 20);
	}

	public void run() {
		Set<Location> remove = new HashSet<>();

		for (Entry<Location, BrewingLogic> entry : map.entrySet()) {
			Location location = entry.getKey();
			BrewingLogic logic = entry.getValue();
			BlockState state = location.getBlock().getState();
			if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
				Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] Chunk unloaded, will check again later.");
				continue;
			}
			if (!(state instanceof BrewingStand)) {
				Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] Is not a brewing stand, will just abort the brewing.");
				remove.add(location);
				continue;
			}

			final BrewingStand brewer = (BrewingStand) state;
			int fuel = brewer.getFuelLevel();
			if (fuel <= 0) {
				Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] No fuel left, will just stop the processes to it gets filled up.");
				continue;
			}

			if (!logic.isBrewingStared()) {
				logic.startTime();
				brewer.setFuelLevel(fuel - 1);
				brewer.update();
			}

			BrewerInventory inv = brewer.getInventory();
			ItemStack[] contents = Arrays.copyOfRange(inv.getContents(), 0, 3);
			final ItemStack item = inv.getItem(3);

			if (Arrays.stream(contents).allMatch(Objects::isNull) || item == null) {
				if (item == null) {
					Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] All result slots is empty or ingredient is removed, cancel the brewing.");
				} else {
					Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] All result slots is empty, cancel the brewing.");
				}
				remove.add(location);
				continue;
			}

			if (logic.getRemainingMillis() / 200 % 2 == 0) {
				Particle particle = getParticle("WITCH");
				if (particle != null)
					location.getWorld().spawnParticle(particle, location.clone().add(0.5, 1, 0.5), 8, 0.1, 0.1, 0.1, 3);
				Sound brewSound = getSound("BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT", "ENTITY_GENERIC_DRINK");
				location.getWorld().playSound(location, brewSound, 0.4f, 1.0f);
			}

			Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] Brewing and the time left: " + logic.getRemainingMillis() / 1000);
			if (logic.isBrewingDone()) {
				if(item.getAmount() < 1 || item.getType() == Material.AIR){
					Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] Can't remove ingredient that is AIR or the amount is zero or less.");
					remove.add(location);
					continue;
				}
				item.setAmount(item.getAmount() - 1);

				BrewingRecipe recipe = logic.getBrewingRecipe();
				for (int i = 0; i < contents.length; i++) {
					if (contents[i] != null) {
						inv.setItem(i, recipe.getResultItem(i));
					}
				}
				Sound brewCompleteSound = getSound("BLOCK_BREWING_STAND_BREW_COMPLETE", "BLOCK_BREWING_STAND_BREW");
				location.getWorld().playSound(location, brewCompleteSound, 1.0f, 1.0f);
				Debug.Send(Type.Brewing, () -> "[" + logic.getBrewingRecipe().getKey() + "] Just completed the brewing and the result should be set inside the inventory.");
				remove.add(location);
			}
		}
		if (!remove.isEmpty())
			remove.forEach(map::remove);
	}

	public void addTask(final Location location, final BrewingRecipe brewingRecipe) {
		this.addTask(location, brewingRecipe, (brewingLogic) -> {
		});
	}

	public void addTask(final Location location, final BrewingRecipe brewingRecipe, final Consumer<BrewingLogic> brewingStand) {
		BrewingLogic brewingLogic = map.get(location);
		if (brewingLogic == null)
			brewingLogic = new BrewingLogic(brewingRecipe);
		brewingStand.accept(brewingLogic);
		map.put(location, brewingLogic);
	}

	public Particle getParticle(String type) {
		Particle[] particles = Particle.values();

		for (Particle particle : particles) {
			if (particle.name().equals(type.toUpperCase()))
				return particle;
			if (particle.name().contains(type.toUpperCase())) {
				return particle;
			}
		}

		return Arrays.stream(particles).findFirst().orElse(null);
	}

	public Sound getSound(String modernName, String legacyName) {
		try {
			if (self().getVersionChecker().newerThan(ServerVersion.v1_19)) {
				Class<?> soundEnumClass = Sound.class;
				Field field = soundEnumClass.getField(modernName);
				return (Sound) field.get(null);
			} else {
				return Sound.valueOf(modernName);
			}
		} catch (IllegalArgumentException e) {
			return Sound.valueOf(legacyName);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			try {
				if (self().getVersionChecker().newerThan(ServerVersion.v1_19)) {
					Class<?> soundEnumClass = Sound.class;
					Field field = soundEnumClass.getField(legacyName);
					return (Sound) field.get(null);
				} else {
					return Sound.valueOf(legacyName);
				}
			} catch (NoSuchFieldException | IllegalAccessException ignore) {
			}
		}
		return null;
	}

	public static class BrewingLogic {
		private final BrewingRecipe brewingRecipe;
		private long startTimeMillis = -1;

		public BrewingLogic(final BrewingRecipe brewingRecipe) {
			this.brewingRecipe = brewingRecipe;
		}

		public BrewingRecipe getBrewingRecipe() {
			return brewingRecipe;
		}

		public void startTime() {
			this.startTimeMillis = System.currentTimeMillis();
		}

		public boolean isBrewingStared() {
			return startTimeMillis > 0;
		}

		public boolean isBrewingDone() {
			if (startTimeMillis < 0) return false;
			long elapsed = System.currentTimeMillis() - startTimeMillis;
			return elapsed >= (brewingRecipe.getDuration() * 1000L);
		}

		public long getRemainingMillis() {
			if (startTimeMillis < 0) return brewingRecipe.getDuration() * 1000L;
			long remaining = (brewingRecipe.getDuration() * 1000L) - (System.currentTimeMillis() - startTimeMillis);
			return Math.max(remaining, 0);
		}

	}
}

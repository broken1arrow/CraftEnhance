package com.dutchjelly.craftenhance.files.blockowner;

import com.dutchjelly.craftenhance.files.util.SimpleYamlHelper;
import com.dutchjelly.craftenhance.util.LocationWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;

public class BlockOwnerCache extends SimpleYamlHelper {
	private final Map<String, BlockOwnerData> containerOwners = new HashMap<>();

	public BlockOwnerCache() {
		super("container-owners.yml", true, true);

	}

	public Map<String, BlockOwnerData> getContainerOwnersRaw() {
		return containerOwners;
	}

	public Map<Location, BlockOwnerData> getContainerOwners() {
		Map<Location, BlockOwnerData> mapConverted = new HashMap<>();
		containerOwners.forEach((locString, blockOwnerData) ->
				mapConverted.put(this.retriveLocation(locString), blockOwnerData)
		);
		return mapConverted;
	}

	@Nullable
	public BlockOwnerData getContainerOwner(@NotNull final Location location) {
		final LocationWrapper locationWrapper = new LocationWrapper(location, location.getWorld().getUID());
		return containerOwners.get(locationWrapper.toString());
	}

	public void getContainerOwner(final Location location, @NotNull final Consumer<BlockOwnerData> callback) {
		if (location == null)
			return;
		final LocationWrapper locationWrapper = new LocationWrapper(location, location.getWorld().getUID());
		BlockOwnerData blockOwnerData = containerOwners.get(locationWrapper.toString());
		if (blockOwnerData != null) {
			callback.accept(blockOwnerData);
		}
	}

	public void putContainerOwner(@NotNull final Location location, @NotNull final Consumer<BlockOwnerData> callback) {
		final LocationWrapper locationWrapper = new LocationWrapper(location, location.getWorld().getUID());
		final BlockOwnerData blockOwnerData = new BlockOwnerData(locationWrapper);

		callback.accept(blockOwnerData);
		this.containerOwners.put(locationWrapper.toString(), blockOwnerData);
	}

	public void putContainerOwner(@NotNull final LocationWrapper locationWrapper, @NotNull final Consumer<BlockOwnerData> callback) {
		final BlockOwnerData blockOwnerData = new BlockOwnerData(locationWrapper);

		callback.accept(blockOwnerData);
		this.containerOwners.put(locationWrapper.toString(), blockOwnerData);
	}

	public void remove(final Location location) {
		final LocationWrapper locationWrapper = new LocationWrapper(location, location.getWorld().getUID());
		this.containerOwners.remove(locationWrapper.toString());
	}

	@Override
	protected void saveDataToFile(final File file) {
		FileConfiguration fileConfiguration = this.getCustomConfig();
		if (fileConfiguration == null)
			fileConfiguration = YamlConfiguration.loadConfiguration(file);
		fileConfiguration.set("Containers", null);
		for (final Entry<String, BlockOwnerData> entry : containerOwners.entrySet())
			this.setData(file, "Containers." + entry.getKey(), entry.getValue());
	}

	@Override
	protected void loadSettingsFromYaml(final File file) {
		final ConfigurationSection templateConfig = this.getCustomConfig().getConfigurationSection("Containers");
		if (templateConfig == null) return;
		for (final String category : templateConfig.getKeys(false)) {
			final BlockOwnerData categoryData = this.getData("Containers." + category, BlockOwnerData.class);
			if (categoryData == null) continue;
			containerOwners.put(category, categoryData);
		}
	}

	private Location retriveLocation(@Nonnull final String key) {
		final String[] parsedKey = key.split(",");
		Location loc = new Location(null, 0, 0, 0);
		if (parsedKey.length > 3) {
			UUID worldUIDD = UUID.fromString(parsedKey[3]);
			final World world = Bukkit.getServer().getWorld(worldUIDD);
			loc = new Location(
					world,
					Integer.parseInt(parsedKey[0]),
					Integer.parseInt(parsedKey[1]),
					Integer.parseInt(parsedKey[2]));
		}


		return loc;
	}

}

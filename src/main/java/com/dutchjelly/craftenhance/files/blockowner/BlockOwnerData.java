package com.dutchjelly.craftenhance.files.blockowner;

import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.util.LocationWrapper;
import lombok.NonNull;
import org.broken.arrow.library.serialize.utility.serialize.ConfigurationSerializable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockOwnerData implements ConfigurationSerializable {

	private UUID currentOwner;
	private double experiences;
	private LocationWrapper locationWrapper;

	public BlockOwnerData(@NonNull final LocationWrapper locationWrapper) {
		this.locationWrapper = locationWrapper;
	}

	public static BlockOwnerData deserialize(final Map<String, Object> map) {
		final String[] parsedKey = ((String) map.getOrDefault("location", "")).split(",");
		UUID worldUIDD = UUID.randomUUID();
		Location loc = new Location(null, 0, 0, 0);
		if (parsedKey.length > 3) {
			worldUIDD = UUID.fromString(parsedKey[3]);
			final World world = Bukkit.getServer().getWorld(worldUIDD);
			loc = new Location(
					world,
					Integer.parseInt(parsedKey[0]),
					Integer.parseInt(parsedKey[1]),
					Integer.parseInt(parsedKey[2]));
		}
		LocationWrapper locationWrapper = new LocationWrapper(loc, worldUIDD);
		BlockOwnerData blockOwnerData = new BlockOwnerData(locationWrapper);
		blockOwnerData.setCurrentOwner(UUID.fromString((String) map.get("owner")));

		final Object experiences = map.get("experiences");
		if (experiences instanceof String) {
			try {
				blockOwnerData.setExperiences(Double.parseDouble((String) experiences));
			} catch (NumberFormatException exception) {
				Debug.error("The set experiences is not valid number. The input: " + experiences);
			}
		} else if (experiences instanceof Number)
			try {
				blockOwnerData.setExperiences((double) experiences);
			} catch (ClassCastException | NumberFormatException exception) {
				Debug.error("The set experiences is not valid number. The input: " + experiences);
			}

		return blockOwnerData;
	}

	public UUID getCurrentOwner() {
		return currentOwner;
	}

	public BlockOwnerData setCurrentOwner(final UUID currentOwner) {
		this.currentOwner = currentOwner;
		return this;
	}

	public double getExperiences() {
		return experiences;
	}

	public BlockOwnerData setExperiences(final double experiences) {
		this.experiences = experiences;
		return this;
	}

	@Nullable
	public Location getLocation() {
		if (locationWrapper == null)
			return null;
		return locationWrapper.getLocation();
	}

	public BlockOwnerData setLocationWrapper(@NonNull final LocationWrapper locationWrapper) {
		this.locationWrapper = locationWrapper;
		return this;
	}

	@Override
	public Map<String, Object> serialize() {
		return new HashMap<String, Object>() {{
			put("location", locationWrapper.toString());
			put("owner", currentOwner);
			put("experiences", experiences);
		}};
	}
}

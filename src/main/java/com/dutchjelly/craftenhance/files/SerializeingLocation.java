package com.dutchjelly.craftenhance.files;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class SerializeingLocation {

	public static String serializeLoc(final Location loc) {
		String name = loc.getWorld() + "";
		if (loc.getWorld() != null)
			name = loc.getWorld().getName();
		return name + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + (loc.getPitch() != 0F || loc.getYaw() != 0F ? " " + Math.round(loc.getYaw()) + " " + Math.round(loc.getPitch()) : "");
	}


	public static Location deserializeLoc(Object rawLoc) {
		if (rawLoc == null) return null;

		String[] parts;
		if (rawLoc instanceof Location) {
			return (Location) rawLoc;
		} else  if (!rawLoc.toString().contains(" ")) {
			return null;
		}else {
			int length = (parts = rawLoc.toString().split(" ")).length;
			if (length == 4) {
				final String world = parts[0];
				final World bukkitWorld = Bukkit.getWorld(world);
				if (bukkitWorld == null)
					return null;
				if (!parts[1].matches("[-+]?\\d+") && !parts[2].matches("[-+]?\\d+") && !parts[3].matches("[-+]?\\d+"))
					return null;
				else {
					int x = Integer.parseInt(parts[1]), y = Integer.parseInt(parts[2]), z = Integer.parseInt(parts[3]);
					return new Location(bukkitWorld, x, y, z);
				}
			}
		}
		return null;
	}
}

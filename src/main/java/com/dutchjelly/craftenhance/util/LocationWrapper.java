package com.dutchjelly.craftenhance.util;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public class LocationWrapper {
    private final Location location;
    private final UUID worldUUID;  

    public LocationWrapper(@NonNull final Location location,@NonNull final UUID world) {
        this.location = location;
        this.worldUUID = world;
    }

    public Location getLocation() {
        World world = Bukkit.getServer().getWorld(worldUUID);
        if (world != null) {
            return new Location(world, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        }
        return location;  // Return the original location if the world is not loaded
    }

    // Store the world as a string (UUID) in the key
    @Override
    public String toString() {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + "," + worldUUID;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LocationWrapper that = (LocationWrapper) obj;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }
}
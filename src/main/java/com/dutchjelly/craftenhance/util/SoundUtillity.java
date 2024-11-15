package com.dutchjelly.craftenhance.util;

public class SoundUtillity {

	public static org.bukkit.Sound getSound(String sound) {
		if (sound == null) return null;
		final org.bukkit.Sound[] sounds = org.bukkit.Sound.values();
		sound = sound.toUpperCase();

		for (final org.bukkit.Sound sound1 : sounds) {
			if (sound1.name().equals(sound))
				return sound1;
		}
		return null;
	}
}


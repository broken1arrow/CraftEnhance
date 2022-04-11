package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import org.bukkit.Material;

import java.util.Random;

import static com.dutchjelly.craftenhance.CraftEnhance.self;


public class FurnaceDefultValues {

	public static int getExp(Material material) {
		Random random = new Random();
		int percent = getChanceForExp(material);
		if (random.nextDouble() * 100D < percent)
			return 1;
		return 0;
	}

	public static int getChanceForExp(Material material) {
		if (self().getVersionChecker().newerThan(VersionChecker.ServerVersion.v1_13))
			return matrialAfter_1_13(material);
		else
			return matrialBefore_1_13(material);
	}

	private static int matrialAfter_1_13(Material material) {
		switch (material) {
			case BEEF:
			case CHICKEN:
			case COD:
			case CLAY:
			case SALMON:
			case POTATO:
			case PORKCHOP:
			case MUTTON:
			case RABBIT:
				return 35;
			case IRON_INGOT:
			case REDSTONE_WIRE:
				return 70;
			case GOLD_INGOT:
			case DIAMOND:
			case EMERALD:
				return 100;
			case LAPIS_LAZULI:
			case NETHERITE_INGOT:
				return 20;
			case CHARCOAL:
				return 15;
			case SAND:
			case COBBLESTONE:
			case NETHERRACK:
			case STONE_BRICKS:
			case COAL:
			case IRON_NUGGET:
			case GOLD_NUGGET:
				return 10;
			case CLAY_BALL:
				return 30;
			default:
				return 1;
		}
	}

	private static int matrialBefore_1_13(Material material) {
		switch (material) {
			case LEGACY_RAW_BEEF:
			case LEGACY_RAW_CHICKEN:
			case LEGACY_RAW_FISH:
			case LEGACY_CLAY:
			case LEGACY_POTATO:
			case LEGACY_PORK:
			case LEGACY_MUTTON:
			case LEGACY_RABBIT:
				return 35;
			case LEGACY_IRON_INGOT:
			case LEGACY_REDSTONE_ORE:
				return 70;
			case LEGACY_GOLD_INGOT:
			case LEGACY_DIAMOND:
			case LEGACY_EMERALD:
				return 100;
			case LEGACY_LAPIS_ORE:
			case LEGACY_QUARTZ:
				return 20;
			case LEGACY_SAND:
			case LEGACY_COBBLESTONE:
			case LEGACY_NETHER_BRICK:
			case STONE_BRICKS:
			case LEGACY_COAL:
			case LEGACY_IRON_NUGGET:
			case LEGACY_GOLD_NUGGET:
				return 10;
			case LEGACY_CLAY_BALL:
				return 30;
			default:
				return 1;
		}
	}
}


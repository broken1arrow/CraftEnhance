package com.dutchjelly.craftenhance.util;

import org.bukkit.Material;

import java.util.Random;


public class FurnaceDefultValues {

	public static int getExp(Material material) {
		Random random = new Random();
		int percent = getChanceForExp(material);
		if (random.nextDouble() * 100D < percent)
			return 1;
		return 0;
	}

	public static int getChanceForExp(Material material) {
		return matrialAfter_1_13(material.name());
	}

	private static int matrialAfter_1_13(String materials) {
		final Material material = Material.getMaterial(materials);
		if (material != null)
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
				case TERRACOTTA:
					return 35;
				case IRON_INGOT:
				case COPPER_INGOT:
					return 70;
				case GOLD_INGOT:
				case DIAMOND:
				case EMERALD:
				case NETHERITE_SCRAP:
				case CACTUS:
					return 100;
				case LAPIS_LAZULI:
				case QUARTZ:
					return 20;
				case SPONGE:
				case CHARCOAL:
					return 15;
				case SAND:
				case POPPED_CHORUS_FRUIT:
				case NETHERRACK:
				case NETHER_BRICK:
				case CRACKED_NETHER_BRICKS:
				case CRACKED_DEEPSLATE_BRICKS:
				case CRACKED_DEEPSLATE_TILES:
				case CRACKED_POLISHED_BLACKSTONE_BRICKS:
				case CRACKED_STONE_BRICKS:
				case WHITE_GLAZED_TERRACOTTA:
				case ORANGE_GLAZED_TERRACOTTA:
				case MAGENTA_GLAZED_TERRACOTTA:
				case LIGHT_BLUE_GLAZED_TERRACOTTA:
				case YELLOW_GLAZED_TERRACOTTA:
				case LIME_GLAZED_TERRACOTTA:
				case PINK_GLAZED_TERRACOTTA:
				case GRAY_GLAZED_TERRACOTTA:
				case LIGHT_GRAY_GLAZED_TERRACOTTA:
				case CYAN_GLAZED_TERRACOTTA:
				case PURPLE_GLAZED_TERRACOTTA:
				case BLUE_GLAZED_TERRACOTTA:
				case BROWN_GLAZED_TERRACOTTA:
				case RED_GLAZED_TERRACOTTA:
				case BLACK_GLAZED_TERRACOTTA:
				case STONE_BRICKS:
				case SANDSTONE:
				case RED_SANDSTONE:
				case SMOOTH_STONE:
				case SMOOTH_QUARTZ:
				case SMOOTH_BASALT:
				case COAL:
				case IRON_NUGGET:
				case GOLD_NUGGET:
				case KELP:
				case LIME_DYE:
					return 10;
				case REDSTONE:
				case CLAY_BALL:
					return 30;
				default:
					return 1;
			}
		return 1;
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


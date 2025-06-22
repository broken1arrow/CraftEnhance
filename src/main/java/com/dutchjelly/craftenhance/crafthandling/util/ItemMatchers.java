package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.util.StripColors;
import lombok.Getter;
import org.broken.arrow.localization.library.builders.PluginMessages;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.util.StringUtil.capitalizeFully;

public class ItemMatchers {

	private static boolean backwardsCompatibleMatching = false;

	public static void init(final boolean backwardsCompatibleMatching) {
		ItemMatchers.backwardsCompatibleMatching = backwardsCompatibleMatching;
	}

	public static boolean matchItems(final ItemStack a, final ItemStack b) {
		if (a == null || b == null) return a == null && b == null;
		return a.equals(b);
	}

	public static boolean matchModelData(final ItemStack a, final ItemStack b) {
		final ItemMeta am = a.getItemMeta();
		final ItemMeta bm = b.getItemMeta();
		if (am == null) return bm == null || !bm.hasCustomModelData();
		if (bm == null) return am == null || !am.hasCustomModelData();
		return am.hasCustomModelData() == bm.hasCustomModelData() && (!am.hasCustomModelData() || am.getCustomModelData() == bm.getCustomModelData());
	}

	public static boolean matchMeta(final ItemStack a, final ItemStack b) {
		if (a == null || b == null) return a == null && b == null;
		final boolean canUseModeldata = Adapter.canUseModeldata();

		if (backwardsCompatibleMatching) {
			return a.getType().equals(b.getType()) && getDamage(a.getItemMeta()) == getDamage(b.getItemMeta()) &&
					a.hasItemMeta() == b.hasItemMeta() && (!a.hasItemMeta() || (a.getItemMeta().toString().equals(b.getItemMeta().toString()))
					&& (!canUseModeldata || matchModelData(a, b)));
		}

		return a.isSimilar(b) && (!canUseModeldata || matchModelData(a, b));
	}

	public static boolean matchType(final ItemStack a, final ItemStack b) {
		if (a == null || b == null) return a == null && b == null;
		return a.getType().equals(b.getType());
	}

	@SafeVarargs
	public static <T> IMatcher<T> constructIMatcher(final IMatcher<T>... matchers) {
		return (a, b) -> Arrays.stream(matchers).allMatch(x -> x.match(a, b));
	}

	public static boolean matchCustomModelData(final ItemStack a, final ItemStack b) {
		if (a == null || b == null) return a == null && b == null;

		if (a.hasItemMeta() && b.hasItemMeta()) {
			final ItemMeta itemMetaA = a.getItemMeta();
			final ItemMeta itemMetaB = b.getItemMeta();
			if (itemMetaA != null && itemMetaB != null && itemMetaA.hasCustomModelData() && itemMetaB.hasCustomModelData())
				return itemMetaA.getCustomModelData() == itemMetaB.getCustomModelData();
			return itemMetaA != null && itemMetaB != null && (!itemMetaA.hasCustomModelData() && !itemMetaB.hasCustomModelData());
		}
		return false;
	}

	public static boolean matchTypeData(final ItemStack a, final ItemStack b) {

		if (a == null || b == null) return a == null && b == null;

		if (self().getVersionChecker().olderThan(VersionChecker.ServerVersion.v1_14)) {
			if (a.getData() == null && b.getData() == null) return matchType(a, b);
			return a.getData().equals(b.getData());
		} else {
			if (a.hasItemMeta() && b.hasItemMeta()) {
				return matchCustomModelData(a, b) || matchType(a, b);
			}
			return matchType(a, b);
		}
	}

	public static boolean matchName(final ItemStack a, final ItemStack b) {
		if (a.hasItemMeta() && b.hasItemMeta()) {
			return a.getItemMeta().getDisplayName().equals(b.getItemMeta().getDisplayName());
		}
		//neither has item meta, and type has to match
		return a.hasItemMeta() == b.hasItemMeta() && a.getType() == b.getType();
	}

	public static boolean matchNameLore(final ItemStack a, final ItemStack b) {
		if (a.getType() != b.getType()) return false;

		if (a.hasItemMeta() && b.hasItemMeta()) {
			final ItemMeta itemMetaA = a.getItemMeta();
			final ItemMeta itemMetaB = b.getItemMeta();

			if (itemMetaA != null && itemMetaB != null) {
				boolean hasSameLore = itemMetaA.getLore() == null || itemMetaA.getLore().equals(itemMetaB.getLore());
				if (!hasSameLore)
					hasSameLore = StripColors.stripLore(itemMetaA.getLore()).equals(StripColors.stripLore(itemMetaB.getLore()));

				return itemMetaA.getDisplayName().equals(itemMetaB.getDisplayName()) && hasSameLore;
			}
		}
		//neither has item meta, and type has to match
		return a.hasItemMeta() == b.hasItemMeta() && a.getType() == b.getType();
	}

	/**
	 * Compares two ItemStacks based on their core characteristics without performing a deep metadata match.
	 * This includes:
	 * - Material type
	 * - Display name
	 * - Lore (with optional color stripping)
	 * - Enchantments (same keys and levels)
	 * - Damage value (durability)
	 * - Custom model data (if supported)
	 * <p>
	 * Does NOT perform full ItemMeta equality; instead, it aims to determine whether items are visually
	 * and functionally similar for most gameplay use cases (e.g., crafting, GUI matching, etc.).
	 *
	 * @param a the first ItemStack
	 * @param b the second ItemStack
	 * @return true if both items are considered similar based on core metadata; false otherwise
	 */
	public static boolean matchCoreMeta(final ItemStack a, final ItemStack b) {
		if (a.getType() != b.getType()) return false;

		final boolean hasMetaA = a.hasItemMeta();
		final boolean hasMetaB = b.hasItemMeta();

		if (hasMetaA && hasMetaB) {
			final ItemMeta metaA = a.getItemMeta();
			final ItemMeta metaB = b.getItemMeta();
			return checkMetaIsSame(metaA, metaB);
		}
		final boolean canUseModelData = Adapter.canUseModeldata();

		if (backwardsCompatibleMatching) {
			return a.getType().equals(b.getType()) && getDamage(a.getItemMeta()) == getDamage(b.getItemMeta()) &&
					a.hasItemMeta() == b.hasItemMeta() && (!a.hasItemMeta() || (a.getItemMeta().toString().equals(b.getItemMeta().toString()))
					&& (!canUseModelData || matchModelData(a, b)));
		}
		if (canUseModelData) {
			return hasMetaA == hasMetaB && a.getType() == b.getType() && matchModelData(a, b);
		}

		return hasMetaA == hasMetaB && a.getType() == b.getType() && getDamage(a.getItemMeta()) == getDamage(b.getItemMeta());
	}


	private static boolean checkMetaIsSame(final ItemMeta metaA, final ItemMeta metaB) {
		if (metaA != null && metaB != null) {
			if (metaA.hasAttributeModifiers() != metaB.hasAttributeModifiers())
				return false;

			if (metaA.hasEnchants() != metaB.hasEnchants())
				return false;

			if (metaA.hasDisplayName() != metaB.hasDisplayName())
				return false;

			if (metaA.hasLore() != metaB.hasLore())
				return false;

			boolean sameLore = Objects.equals(metaA.getLore(), metaB.getLore());
			if (metaA.hasLore() && !metaB.hasLore()) {
				sameLore = false;
			} else if (!sameLore) {
				sameLore = StripColors.stripLore(metaA.getLore()).equals(StripColors.stripLore(metaB.getLore()));
			}

			boolean sameEnchants = metaA.hasEnchants() && metaB.hasEnchants();
			if (sameEnchants) {
				sameEnchants = metaA.getEnchants().entrySet().stream().allMatch(entry -> Objects.equals(metaB.getEnchants().get(entry.getKey()), entry.getValue()));
			}

			return sameEnchants && Objects.equals(metaA.getDisplayName(), metaB.getDisplayName()) && sameLore && getDamage(metaA) == getDamage(metaB);
		}
		return false;
	}

	private static int getDamage(final ItemMeta itemMeta) {
		if (itemMeta instanceof Damageable && ((Damageable) itemMeta).hasDamage())
			return ((Damageable) itemMeta).getDamage();
		return 0;
	}

	public enum MatchType {

		MATCH_TYPE(constructIMatcher(ItemMatchers::matchType), "Match only the type of item."),
		MATCH_META(constructIMatcher(ItemMatchers::matchMeta), "Performs a full metadata match on the item.",
				"However, due to hidden NBT data, this might not always detect items as equal.",
				"For safer comparisons, consider using basic meta matching instead.",
				"Note: Starting from Minecraft 1.20, the way items are translated to and from inventories",
				"has changed, making matchMeta less reliable in some cases."),
		MATCH_NAME(constructIMatcher(ItemMatchers::matchName), "Match only on the item display name"),
		MATCH_MODELDATA_AND_TYPE(constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData), "Match modeldata and type of item."),
		MATCH_NAME_LORE(constructIMatcher(ItemMatchers::matchNameLore), "Match name, lore and type on the item."),
		MATCH_BASIC_META(constructIMatcher(ItemMatchers::matchCoreMeta), "Similar to matching metadata, but less strict.");
		//        MATCH_ITEMSADDER()
//        MATCH_NAME_AND_TYPE(constructIMatcher(ItemMatchers::matchName, ItemMatchers::matchType), "match name and type");

		@Getter
		private final IMatcher<ItemStack> matcher;

		@Getter
		private final List<String> description;


		MatchType(final IMatcher<ItemStack> matcher, final String... description) {
			this.matcher = matcher;
			this.description = Arrays.asList(description);
		}

		public Object getMatchName() {
			Object name;
			switch (this) {
				case MATCH_TYPE:
					name = getText("match_type");
					break;
				case MATCH_META:
					name = getText("match_meta");
					break;
				case MATCH_NAME:
					name = getText("match_name");
					break;
				case MATCH_MODELDATA_AND_TYPE:
					name = getText("match_modeldata_and_type");
					break;
				case MATCH_NAME_LORE:
					name = getText("name_lore");
					break;
				case MATCH_BASIC_META:
					name = getText("basic_meta");
					break;
				default:
					name = capitalizeFully(this.name());
			}
			if (name == null || (name.equals("") || name instanceof List && ((List<?>) name).isEmpty()))
				name = capitalizeFully(this.name());
			return name;
		}

		public Object getMatchDescription() {
			Object description;
			switch (this) {
				case MATCH_TYPE:
					description = getText("match_type_match_type");
					break;
				case MATCH_META:
					description = getText("match_type_match_meta");
					break;
				case MATCH_NAME:
					description = getText("match_type_match_name");
					break;
				case MATCH_MODELDATA_AND_TYPE:
					description = getText("match_type_match_modeldata_and_type");
					break;
				case MATCH_NAME_LORE:
					description = getText("match_type_name_lore");
					break;
				case MATCH_BASIC_META:
					description = getText("match_type_basic_meta");
					break;
				default:
					description = this.getDescription();
			}
			if (description == null || (description.equals("") || description instanceof List && ((List<?>) description).isEmpty()))
				description = this.getDescription();
			return description;
		}

		public Object getText(String key) {
			final PluginMessages pluginMessages = self().getLocalizationCache().getLocalization().getPluginMessages();
			if (pluginMessages == null)
				return "";
			return pluginMessages.getMessage(key);
		}
	}
//    public static boolean matchItemsadderItems(ItemStack a, ItemStack b) {
//        CustomStack stack = CustomStack.byItemStack(myItemStack);
//        CustomStack
//    }
}

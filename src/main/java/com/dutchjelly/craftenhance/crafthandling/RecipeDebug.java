package com.dutchjelly.craftenhance.crafthandling;

import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@SuppressWarnings("deprecation")
public class RecipeDebug {

	public static String recipeIngredientsDebug(final WBRecipe wbRecipe, final ItemStack[] matrix) {
		if (wbRecipe.matches(matrix)) {
			return ""; // Nothing to debug if the recipe matches perfectly
		}
		Set<ItemStack> itemStackSet = new LinkedHashSet<>(Arrays.asList(wbRecipe.getContent()));

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("\n================ ingredient mismatch debug ================\n");
		stringBuilder.append("The recipe result type: ").append(wbRecipe.getResult().getType());
		for (ItemStack expectedItem : itemStackSet) {
			if (expectedItem == null || expectedItem.getType() == Material.AIR) continue;

			ItemMeta expectedMeta = expectedItem.getItemMeta();
			Map<ItemStack, TrackStacks> matchingInvItems = getMatchingItemsWithSlots(matrix, expectedItem);

			if (matchingInvItems != null && !matchingInvItems.isEmpty()) {
				stringBuilder.append("\ningredients: \n");

				String expectedName = (expectedMeta != null && expectedMeta.hasDisplayName()) ? expectedMeta.getDisplayName() : "none";
				String expectedLore = (expectedMeta != null && expectedMeta.hasLore()) ? expectedMeta.getLore().toString() : "none";
				stringBuilder.append("  expected_type=").append(expectedItem.getType().name()).append("\n");
				stringBuilder.append("  expected_meta: {name='").append(expectedName)
						.append("', lore=").append(expectedLore).append("}\n");
				stringBuilder.append("  candidates:\n");

				appendCandidateIngredients(matchingInvItems, stringBuilder, expectedMeta);
			}
		}

		return stringBuilder.toString();
	}

	public static String convertItemStackArrayToString(final Collection<ItemStack> collection) {
		final StringBuilder stringBuilder = new StringBuilder();
		final Map<ItemStack, Integer> map = new LinkedHashMap<>();
		final ItemStack[] matrix = collection.toArray(new ItemStack[0]);
		final Boolean[] checkMetadata = new Boolean[matrix.length];
		Arrays.fill(checkMetadata, false);
		for (int i = 0; i < matrix.length; i++) {
			ItemStack invItemStack = matrix[i];
			checkMetadata[i] = isNoMetadata(invItemStack);
			map.merge(invItemStack, 1, Integer::sum);
		}
		final boolean noItemWithMeta = Arrays.stream(checkMetadata).anyMatch(x -> x);
		stringBuilder.append("\n____________ingredient matrix_____________");

		if (!noItemWithMeta && !map.isEmpty()) {
			stringBuilder.append("\nIngredient type= ");
		}
		map.forEach((itemStack, integer) -> {
			formatStack(itemStack, stringBuilder, noItemWithMeta);
			if (integer > 1) {
				if (!noItemWithMeta) {
					stringBuilder.append("x").append(integer).append(", ");
				} else {
					stringBuilder.append("amount: ").append(integer);
				}
			}
		});
		stringBuilder.append("\n____________ingredient matrix_____________\n");
		return stringBuilder + "";
	}

	public static String convertItemStackArrayToString(final ItemStack[] matrix) {
		final StringBuilder stringBuilder = new StringBuilder();
		final Map<ItemStack, Integer> map = new LinkedHashMap<>();
		final Boolean[] checkMetadata = new Boolean[matrix.length];
		Arrays.fill(checkMetadata, false);

		for (int i = 0; i < matrix.length; i++) {
			ItemStack invItemStack = matrix[i];
			checkMetadata[i] = isNoMetadata(invItemStack);
			map.merge(invItemStack, 1, Integer::sum);
		}
		final boolean noItemWithMeta = Arrays.stream(checkMetadata).anyMatch(x -> x);

		stringBuilder.append("\n____________ingredient matrix_____________");
		if (!noItemWithMeta && !map.isEmpty()) {
			stringBuilder.append("\nIngredient type= ");
		}
		map.forEach((itemStack, integer) -> {
			formatStack(itemStack, stringBuilder, noItemWithMeta);
			if (!noItemWithMeta) {
				stringBuilder.append("x").append(integer).append(", ");
			} else {
				stringBuilder.append("amount: ").append(integer);
			}
		});
		stringBuilder.append("\n____________ingredient matrix_____________\n");
		return stringBuilder + "";
	}

	public static String formatOneStack(final ItemStack stack) {
		if (stack == null) return "empty";
		StringBuilder stringBuilder = new StringBuilder();
		boolean checkMetadata = isNoMetadata(stack);
		if (!checkMetadata)
			stringBuilder.append("\nIngredient type= ");
		formatStack(stack, stringBuilder, checkMetadata);
		stringBuilder.append("________________________________");
		return stringBuilder + "";
	}

	public static void formatStack(final ItemStack stack, final StringBuilder stringBuilder, boolean hasMetadata) {
		if (!hasMetadata) {
			if (stack != null) {
				stringBuilder.append(stack.getType());
			} else {
				stringBuilder.append("AIR");
			}
			return;
		}

		if (stack != null) {
			final ItemMeta itemMeta = stack.getItemMeta();
			stringBuilder.append("\nIngredient type= ").append(stack.getType());
			if (itemMeta != null) {
				final String displayName = itemMeta.getDisplayName();
				final boolean haveDisplayName = displayName == null || displayName.isEmpty() || displayName.equals("null");
				if (haveDisplayName && itemMeta.getLore() == null) {
					stringBuilder.append("\nMetadata= {no name and lore set}");
				} else {
					final String name = haveDisplayName ? "none" : "'" + displayName + "'";
					stringBuilder.append("\nMetadata= ").append("{name=").append(name);
					if (itemMeta.getLore() != null)
						stringBuilder.append(", lore=").append(itemMeta.getLore()).append("}");
					else stringBuilder.append(", lore=none set}");
				}
			} else {
				stringBuilder.append("\nMetadata= {not set}");
			}
			stringBuilder.append("\n");
		} else {
			stringBuilder.append("\nIngredient type= AIR\n");
		}
	}


	private static FieldResult findMismatchIndex(String expectedName, String str2) {
		if (expectedName == null && str2 == null)
			return new FieldResult(FieldMatch.EXACT, -1);
		if (expectedName == null || str2 == null) {
			if (expectedName != null) {
				return new FieldResult(FieldMatch.NULL_INPUT, -3);
			}
			return new FieldResult(FieldMatch.NULL_INPUT, -4);
		}
		int minLength = Math.min(expectedName.length(), str2.length());

		for (int i = 0; i < minLength; i++) {
			if (expectedName.charAt(i) != str2.charAt(i)) {
				return new FieldResult(FieldMatch.MISMATCH_AT_POS, i);
			}
		}
		if (expectedName.length() != str2.length()) {
			return new FieldResult(FieldMatch.LENGTH_MISMATCH, -2);
		}
		return new FieldResult(FieldMatch.EXACT, -1);
	}


	private static List<ItemStack> getItemStack(final ItemStack[] matrix, final ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR) return null;
		List<ItemStack> items = new ArrayList<>();
		for (ItemStack invItemStack : matrix) {
			if (invItemStack != null && invItemStack.getType() == itemStack.getType()) {
				items.add(invItemStack);
			}
		}
		return items.isEmpty() ? null : items;
	}

	private static boolean isNoMetadata(final ItemStack stack) {
		if (stack == null) return false;
		if (!stack.hasItemMeta()) return false;
		final ItemMeta itemMeta = stack.getItemMeta();
		final String displayName = itemMeta.getDisplayName();
		final boolean haveDisplayName = displayName == null || displayName.isEmpty() || displayName.equals("null");
		return !haveDisplayName || itemMeta.getLore() != null;
	}

	private static void appendCandidateIngredients(final Map<ItemStack, TrackStacks> candidates, final StringBuilder sb, final ItemMeta expectedMeta) {
		String expectedName = (expectedMeta != null && expectedMeta.hasDisplayName()) ? expectedMeta.getDisplayName() : null;
		String expectedLore = (expectedMeta != null && expectedMeta.hasLore()) ? expectedMeta.getLore().toString() : null;

		for (Entry<ItemStack, TrackStacks> entry : candidates.entrySet()) {
			ItemStack actualItem = entry.getKey();
			TrackStacks trackStacks = entry.getValue();

			ItemMeta actualMeta = actualItem == null ? null : actualItem.getItemMeta();

			String typeName = actualItem == null ? "AIR" : actualItem.getType().name();
			final boolean hasDisplayName = actualMeta != null && actualMeta.hasDisplayName() && actualMeta.getDisplayName() != null;
			String actualName = hasDisplayName ? actualMeta.getDisplayName() : null;
			String actualLore = (actualMeta != null && actualMeta.hasLore()) ? actualMeta.getLore().toString() : null;
			MatchStatus result = getMatchStatus(expectedName, actualName, expectedLore, actualLore);

			sb.append("    ingredient:");
			sb.append("\n      slots=").append(trackStacks.slots);
			sb.append("\n      item=").append(typeName).append(" ")
					.append("x").append(trackStacks.amount)
					.append("\n      reason=").append(result.matchStatus)
					.append("\n      description=").append(result.description);

			if (!result.matchStatus.equals("MATCH_EXACT")) {
				sb.append("\n").append("      meta=");
				if (actualName != null && actualLore != null) {
					sb.append(" | {name='").append(actualName).append("' , lore=").append(actualLore).append("}");
				} else {
					sb.append("{no name and lore set}");
				}
			}
			sb.append("\n");
		}
	}

	private static class MatchStatus {
		public final String matchStatus;
		public final String description;

		public MatchStatus(final String matchStatus, final String description) {
			this.matchStatus = matchStatus;
			this.description = description;
		}
	}

	private static Map<ItemStack, TrackStacks> getMatchingItemsWithSlots(final ItemStack[] matrix, final ItemStack expectedItem) {
		if (expectedItem == null || expectedItem.getType() == Material.AIR) return null;

		Map<ItemStack, TrackStacks> items = new LinkedHashMap<>();
		for (int i = 0; i < matrix.length; i++) {
			ItemStack invItemStack = matrix[i];
			if (invItemStack != null && invItemStack.getType() == expectedItem.getType()) {

				items.computeIfAbsent(invItemStack, stack -> new TrackStacks())
						.addSlot(i)
						.addAmount();
			}
		}
		return items.isEmpty() ? null : items;
	}

	static class TrackStacks {
		List<Integer> slots = new ArrayList<>();
		int amount;

		public TrackStacks addSlot(int slot) {
			slots.add(slot);
			return this;
		}

		public TrackStacks addAmount() {
			this.amount++;
			return this;
		}
	}

	@Nonnull
	private static MatchStatus getMatchStatus(String expectedName, String actualName, String expectedLore, String actualLore) {
		final FieldResult display = findMismatchIndex(expectedName, actualName);
		final FieldResult lore = findMismatchIndex(expectedLore, actualLore);

		MatchStatusType type;
		String description;

		final FieldMatch displayMatch = display.getMatch();
		final FieldMatch loreMatch = lore.getMatch();
		final boolean displayBad = displayMatch != FieldMatch.EXACT;
		final boolean loreBad = loreMatch != FieldMatch.EXACT;


		if (displayMatch == FieldMatch.NULL_INPUT || loreMatch == FieldMatch.NULL_INPUT) {

			if (displayMatch == FieldMatch.NULL_INPUT && loreMatch == FieldMatch.NULL_INPUT) {
				type = MatchStatusType.DISPLAY_LORE_NULL;
				description = "Both display name and lore are missing for the input item.";
			} else if (displayMatch == FieldMatch.NULL_INPUT) {
				type = MatchStatusType.DISPLAY_NULL;
				description = "Expected display name is missing input item.";
			} else {
				type = MatchStatusType.LORE_NULL;
				description = "Expected lore is missing input item.";
			}

			return new MatchStatus(type.name(), description);
		}
		if (displayMatch == FieldMatch.LENGTH_MISMATCH ||
				loreMatch == FieldMatch.LENGTH_MISMATCH) {

			if (displayMatch == FieldMatch.LENGTH_MISMATCH &&
					loreMatch == FieldMatch.LENGTH_MISMATCH) {

				type = MatchStatusType.DISPLAY_LORE_LENGTH_MISMATCH;
				description = "Both display name and lore length differ.";
			} else if (displayMatch == FieldMatch.LENGTH_MISMATCH) {
				type = MatchStatusType.DISPLAY_LENGTH_MISMATCH;
				description = "Display name length mismatch.";
			} else {
				type = MatchStatusType.LORE_LENGTH_MISMATCH;
				description = "Lore length mismatch.";
			}

			return new MatchStatus(type.name(), description);
		}
		if (displayBad || loreBad) {
			if (displayBad && loreBad) {
				type = MatchStatusType.DISPLAY_LORE_MISMATCH_AT_POS;
				description = "Display and lore mismatch in the set display name at:'" + display.getPosition() + "'pos and at lore pos:'" + lore.getPosition() + "'.";
			} else if (displayBad) {
				type = MatchStatusType.DISPLAY_MISMATCH_AT_POS;
				description = "Display mismatch at position " + display.getPosition();
			} else {
				type = MatchStatusType.LORE_MISMATCH_AT_POS;
				description = "Lore mismatch at position " + lore.getPosition();
			}

			return new MatchStatus(type.name(), description);
		}
		return new MatchStatus(MatchStatusType.EXACT.name(), "Exact match");
	}

	private static final class FieldResult {

		private final FieldMatch match;
		private final int position;

		public FieldResult(FieldMatch match, int position) {
			this.match = match;
			this.position = position;
		}

		public FieldMatch getMatch() {
			return match;
		}

		public int getPosition() {
			return position;
		}

		@Override
		public String toString() {
			return "FieldResult{" +
					"match=" + match +
					", position=" + position +
					'}';
		}
	}

	enum FieldMatch {
		EXACT,
		LENGTH_MISMATCH,
		MISMATCH_AT_POS,
		NULL_INPUT,
	}

	enum MatchStatusType {
		EXACT,

		DISPLAY_LENGTH_MISMATCH,
		LORE_LENGTH_MISMATCH,
		DISPLAY_LORE_LENGTH_MISMATCH,

		DISPLAY_MISMATCH_AT_POS,
		LORE_MISMATCH_AT_POS,
		DISPLAY_LORE_MISMATCH_AT_POS,

		DISPLAY_NULL,
		LORE_NULL,
		DISPLAY_LORE_NULL
	}
}

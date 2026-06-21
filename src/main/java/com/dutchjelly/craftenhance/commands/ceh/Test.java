package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.RecipeAdapter;
import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.RecipeLoader;
import com.dutchjelly.craftenhance.crafthandling.livedata.RecipeWrapper;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.crafthandling.util.ItemMatchers;
import com.dutchjelly.craftenhance.crafthandling.util.ServerRecipeTranslator;
import com.dutchjelly.craftenhance.crafthandling.util.WBRecipeComparer;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

@CommandRoute(cmdPath = "ceh.test", perms = "ceh.debug")
public class Test implements ICommand {
	@Override
	public String getDescription() {
		return "Unit tests for the plugin.";
	}

	@Override
	public void handlePlayerCommand(Player p, String[] args) {
		if (args.length != 1)
			p.sendMessage("Please specify a unit test index.");
		unitTests(Integer.parseInt(args[0]), p);
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
		if (args.length != 1)
			sender.sendMessage("Please specify a unit test index.");
//        try{
//            unitTests(Integer.parseInt(args[0]), sender);
//        }catch(NumberFormatException e) {
//            sender.sendMessage("Use a number as input!!!!");
//        }
	}


	private void unitTests(int index, CommandSender p) {

		switch (index) {
			case 1:
				testShapedComparer(p);
				break;
			case 2:
				testServerRecipeTranslator(p);
				break;
			case 3:
				testLoader(p);
				break;
			case 4:
				testShapeless(p);
				break;
			case 5:
				test2X2Grid(p);
				break;
			default:
				p.sendMessage("no test specified");
				break;
		}
	}

	private void test2X2Grid(final CommandSender p) {
		final ItemStack itemStack = new ItemStack(Material.ACACIA_BOAT);
		ItemStack[] customContent = new ItemStack[]{null, null, null, itemStack, null, itemStack, itemStack, null, itemStack};
		p.sendMessage("Test 4 items=" + Arrays.toString(RecipeAdapter.compressGrid(customContent)));
		customContent = new ItemStack[]{itemStack, null, null, itemStack, null, null, null, null, null};
		p.sendMessage("Test 2 items straight line=" + Arrays.toString(RecipeAdapter.compressGrid(customContent)));
		customContent = new ItemStack[]{itemStack, itemStack, null, null, null, null, null, null, null};
		p.sendMessage("Test 2 items right next to each other=" + Arrays.toString(RecipeAdapter.compressGrid(customContent)));
		customContent = new ItemStack[]{null, itemStack, null, null, itemStack, null, null, null, null};
		p.sendMessage("Test 2 items diagonally=" + Arrays.toString(RecipeAdapter.compressGrid(customContent)));
		customContent = new ItemStack[]{itemStack, null, null, null, itemStack, null, null, null, null};
		p.sendMessage("Test 2 items diagonally second test=" + Arrays.toString(RecipeAdapter.compressGrid(customContent)));
		customContent = new ItemStack[]{null, itemStack, null, null, null, null, itemStack, null, null};
		p.sendMessage("Test 2 items diagonally bigger gap=" + Arrays.toString(RecipeAdapter.compressGrid(customContent)));
	}

	private void testShapedComparer(CommandSender p) {
		ItemStack i = new ItemStack(Material.DIAMOND);

		p.sendMessage("testing some cornercases");

		//Test shape compare first.
		ItemStack[] a = buildRecipe("--i--i--i");
		ItemStack[] b = buildRecipe("i--i--i--");
		ItemStack[] c = buildRecipe("-i--i--i-");

		Assert(WBRecipeComparer.shapeMatches(a, b, ItemMatchers::matchType));
		Assert(WBRecipeComparer.shapeMatches(a, c, ItemMatchers::matchType));
		Assert(WBRecipeComparer.shapeMatches(b, c, ItemMatchers::matchType));

		a = buildRecipe("----i-i--");
		b = buildRecipe("--i-i----");
		c = buildRecipe("-i-i-----");

		Assert(WBRecipeComparer.shapeMatches(a, b, ItemMatchers::matchType));
		Assert(WBRecipeComparer.shapeMatches(a, c, ItemMatchers::matchType));
		Assert(WBRecipeComparer.shapeMatches(b, c, ItemMatchers::matchType));

		a = buildRecipe("--ii-----");
		b = buildRecipe("-----ii--");
		c = buildRecipe("---i-i---");
		Assert(WBRecipeComparer.shapeMatches(a, b, ItemMatchers::matchType));
		Assert(!WBRecipeComparer.shapeMatches(a, c, ItemMatchers::matchType));
		Assert(!WBRecipeComparer.shapeMatches(b, c, ItemMatchers::matchType));

		/* Random tests */
		p.sendMessage("testing randomly generated recipes");
		final int randomtestcount = 50;
		Random r = new Random();
		for (int t = 0; t < randomtestcount; t++) {
			String shape = "";
			int counter = 0; //count amount of items randomly generated
			for (int j = 0; j < 9; j++) {
				shape += r.nextBoolean() ? '-' : 'i';
				if (shape.endsWith("i")) {
					counter++;
				}
			}
			if (counter == 0) continue;
			a = buildRecipe(shape);
			String othershape = shiftToLeftTop(shape);

			b = buildRecipe(othershape);
			Bukkit.getLogger().log(Level.INFO, shape + "\n testing " + othershape);
			Assert(WBRecipeComparer.shapeMatches(a, b, ItemMatchers::matchType));

			//Try to shuffle elements. and check if they match with randomly generated one
			for (int matchtest = 0; matchtest < 200; matchtest++) {
				List<String> shuffeledShape = Arrays.asList(shape.split(""));
				Collections.shuffle(shuffeledShape, r);
				String s = String.join("", shuffeledShape);
				boolean matches = shiftToLeftTop(s).equals(shiftToLeftTop(shape));
				c = buildRecipe(s);
				boolean success = (matches && WBRecipeComparer.shapeIterationMatches(a, c, ItemMatchers::matchType, 3))
						|| (!matches && !WBRecipeComparer.shapeIterationMatches(a, c, ItemMatchers::matchType, 3));
				if (!success) Bukkit.getLogger().log(Level.INFO, s + "\n" + (matches ? "matches" : "!matches"));
				Assert(success);
			}
		}


		/* test meta matching */
//        p.sendMessage("testing for special item meta and item quantity");
//        //Other types of itemmeta
//        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short)SkullType.PLAYER.ordinal());
//        SkullMeta sMeta = (SkullMeta)item.getItemMeta();
//        sMeta.setOwner("DutchJelly");
//        item.setItemMeta(sMeta);
//        ItemStack item2 = new ItemStack(Material.SKULL_ITEM, 1, (short)SkullType.PLAYER.ordinal());
//        SkullMeta sMeta2 = (SkullMeta)item2.getItemMeta();
//        sMeta2.setOwner("GamerGamer1000");
//        item2.setItemMeta(sMeta2);
//
//        Arrays.fill(a, item);
//        Arrays.fill(b, item2);
//        Arrays.fill(c, item);
//
//        Assert(!WBRecipeComparer.shapeMatches(a,b, ItemMatchers::matchMeta));
//        Assert(WBRecipeComparer.shapeMatches(a,b, ItemMatchers::matchType));
//        Assert(WBRecipeComparer.shapeMatches(a,c, ItemMatchers::matchMeta));
//
//
//        Assert(WBRecipeComparer.shapeMatches(a,b, ItemMatchers::matchType)
//                && !WBRecipeComparer.shapeMatches(a,b, ItemMatchers::matchMeta));
//
//        //quantity shouldn't be matched
//        item.setAmount(2);
//        Arrays.fill(a, item);
//        Assert(WBRecipeComparer.shapeMatches(a,c, ItemMatchers::matchMeta));

		p.sendMessage("All tests executed!");
	}

	private ItemStack[] buildRecipe(String string) {
		ItemStack it = new ItemStack(Material.DIAMOND);
		ItemStack[] built = new ItemStack[9];
		for (int i = 0; i < string.length(); i++) {
			built[i] = string.charAt(i) == '-' ? null : it;
		}
		return built;
	}

	//Inefficient algorithm for comparing shapes so we can test the efficient algorithm.
	private String shiftToLeftTop(String s) {
		if (s.trim() == "") return s;
		if (s.length() != 9) return s;
		while (s.startsWith("---")) s = s.substring(3) + "---";
		while (s.length() > 0 && s.charAt(0) == '-' && s.charAt(3) == '-' && s.charAt(6) == '-')
			s = s.substring(1) + '-';
		return s;
	}



	private void testServerRecipeTranslator(CommandSender p) {


		final int testAmount = 10;
		ItemStack[] items = new ItemStack[]{new ItemStack(Material.DIAMOND), new ItemStack(Material.LADDER), new ItemStack(Material.STICK)};
		Random r = new Random();
		p.sendMessage("testing randomly generated shaped recipes translations");
		for (int i = 0; i < testAmount; i++) {
			ItemStack[] recipe = new ItemStack[9];
			for (int j = 0; j < 9; j++) {
				if (r.nextBoolean()) {
					recipe[j] = items[r.nextInt(3)];
				} else recipe[j] = null;
			}
			ShapedRecipe sr = ServerRecipeTranslator.translateShapedEnhancedRecipe(recipe, new ItemStack(Material.DIAMOND), "test");
			if (sr == null) continue;
			ItemStack[] original = ServerRecipeTranslator.translateShapedRecipe(sr);
			p.sendMessage(Arrays.stream(original).filter(x -> x != null && x.getData() != null).map(x -> x.getData().toString()).collect(Collectors.joining(", ")));
			p.sendMessage(Arrays.stream(recipe).filter(x -> x != null && x.getData() != null).map(x -> x.toString().toString()).collect(Collectors.joining(", ")));
			Assert(WBRecipeComparer.shapeMatches(original, recipe, ItemMatchers::matchType));
		}

		p.sendMessage("All successful!");

	}

	private void testLoader(CommandSender p) {

		ItemStack supahDiamondChestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
		ItemStack supahDiamond = new ItemStack(Material.DIAMOND);
		ItemMeta supahDiamondMeta = supahDiamond.getItemMeta();
		supahDiamondMeta.addEnchant(Enchantment.AQUA_AFFINITY, 10, true);
		supahDiamond.setItemMeta(supahDiamondMeta);
		ItemStack[] customContent = new ItemStack[]{
				supahDiamond, null, supahDiamond,
				supahDiamond, supahDiamond, supahDiamond,
				supahDiamond, supahDiamond, supahDiamond
		};

		WBRecipe recipe = new WBRecipe();
		recipe.setKey("testing123");
		recipe.setPermission("");
		recipe.setContent(customContent);
		recipe.setResult(supahDiamondChestplate);

		RecipeLoader.getInstance().loadRecipe(recipe);
		List<RecipeWrapper> recipeWrappers = RecipeLoader.getInstance().findMatchingRecipe(recipe.getType(), recipe.getContent());
		Assert(recipeWrappers.size() == 1); //there's only one similar server recipe
		Assert(recipeWrappers.stream().anyMatch(recipeWrapper -> recipeWrapper.getRecipeKey().equals(recipe.getKey())));
		p.sendMessage("showing loaded groups...");
		showLoadedRecipeGroups(p);
		Assert(recipeWrappers.stream().anyMatch(x -> {
			WBRecipe wbRecipe = x.getRecipe(WBRecipe.class).orElse(null);
			if (wbRecipe != null) {
				return wbRecipe.getResult().getType().equals(Material.DIAMOND_CHESTPLATE);
			}
			return false;
		}));

		RecipeLoader.getInstance().unloadRecipe(recipe);
		recipeWrappers = RecipeLoader.getInstance().findMatchingRecipe(recipe.getType(), recipe.getContent());
		Assert(recipeWrappers.isEmpty());

		//boots
		customContent = new ItemStack[]{null, null, null, supahDiamond, null, supahDiamond, supahDiamond, null, supahDiamond};
		recipe.setContent(customContent);
		RecipeLoader.getInstance().loadRecipe(recipe);
		recipeWrappers = RecipeLoader.getInstance().findMatchingRecipe(recipe.getType(), recipe.getContent());
		Assert(recipeWrappers.size() == 1); //there's only one similar server recipe

		Assert(recipeWrappers.stream().anyMatch(recipeWrapper -> recipeWrapper.getRecipeKey().equals(recipe.getKey())));
		Assert(recipeWrappers.stream().anyMatch(x -> {
			WBRecipe wbRecipe = x.getRecipe(WBRecipe.class).orElse(null);
			if (wbRecipe != null) {
				return wbRecipe.getResult().getType().equals(Material.DIAMOND_CHESTPLATE);
			}
			return false;
		}));

		WBRecipe recipe2 = new WBRecipe();
		recipe2.setKey("testing1234");
		recipe2.setPermission("");
		recipe2.setContent(customContent);
		recipe2.setResult(new ItemStack(Material.EMERALD));

		RecipeLoader.getInstance().loadRecipe(recipe2);
		RecipeLoader.getInstance().unloadRecipe(recipe2);
		RecipeLoader.getInstance().unloadRecipe(recipe);

		recipeWrappers = RecipeLoader.getInstance().findMatchingRecipe(recipe2.getType(), recipe2.getContent());
		Assert(recipeWrappers.stream().noneMatch(recipeWrapper -> recipeWrapper.getRecipeKey().equals(recipe.getKey())));
		recipeWrappers = RecipeLoader.getInstance().findMatchingRecipe(recipe.getType(), recipe.getContent());
		Assert(recipeWrappers.stream().noneMatch(recipeWrapper -> recipeWrapper.getRecipeKey().equals(recipe.getKey())));


		customContent = new ItemStack[]{null, supahDiamond, null, null, null, null, null, null, null};
		recipe.setContent(customContent);
		RecipeLoader.getInstance().loadRecipe(recipe);

		recipeWrappers = RecipeLoader.getInstance().findMatchingRecipe(recipe.getType(), recipe.getContent());
		Assert(recipeWrappers.isEmpty()); //there's no similar server recipe

		Assert(recipeWrappers.stream().anyMatch(recipeWrapper -> recipeWrapper.getRecipeKey().equals(recipe.getKey())));
		Assert(recipeWrappers.stream().anyMatch(x -> {
			WBRecipe wbRecipe = x.getRecipe(WBRecipe.class).orElse(null);
			if (wbRecipe != null) {
				return wbRecipe.getResult().getType().equals(Material.EMERALD);
			}
			return false;
		}));

		RecipeLoader.getInstance().unloadRecipe(recipe);
		p.sendMessage("test successfully completed!");
	}

	private void showLoadedRecipeGroups(CommandSender p) {
		p.sendMessage("printed debug info in the console (assuming that your debug mode is on)");
		RecipeLoader.getInstance().printGroupsDebugInfo();
	}


	private void testShapeless(CommandSender p) {
		final int nRandomTests = 500;
		ItemStack[] items = Arrays.asList(Material.values()).stream().map(ItemStack::new).toArray(ItemStack[]::new);
		Random r = new Random();
		for (int i = 0; i < nRandomTests; i++) {
			List<ItemStack> ingredients = new ArrayList<>();
			for (int j = 0; j < r.nextInt(8) + 1; j++)
				ingredients.add(items[r.nextInt(items.length)]);
			for (int j = 0; j < 50; j++) {
				List<ItemStack> shuffled = new ArrayList<>(ingredients);
				Collections.shuffle(shuffled, r);
				Assert(WBRecipeComparer.ingredientsMatch(ingredients.stream().toArray(ItemStack[]::new), shuffled.stream().toArray(ItemStack[]::new), ItemMatchers::matchMeta));
				List<ItemStack> lessIngredients = new ArrayList<>(ingredients);
				lessIngredients.stream().filter(x -> x != null && r.nextInt(4) == 1).collect(Collectors.toList());
				lessIngredients.set(r.nextInt(ingredients.size()), null);
				Collections.shuffle(lessIngredients, r);
				Assert(!WBRecipeComparer.ingredientsMatch(ingredients.stream().toArray(ItemStack[]::new), lessIngredients.stream().toArray(ItemStack[]::new), ItemMatchers::matchMeta));
				List<ItemStack> moreIngredients = new ArrayList<>(ingredients);
				Arrays.asList(new int[1 + r.nextInt(5)]).forEach(x -> moreIngredients.add(items[r.nextInt(items.length)]));
				Collections.shuffle(moreIngredients);
				Assert(!WBRecipeComparer.ingredientsMatch(ingredients.stream().toArray(ItemStack[]::new), moreIngredients.stream().toArray(ItemStack[]::new), ItemMatchers::matchMeta));
			}
		}
		p.sendMessage("test successfully completed!");
	}

	@SneakyThrows
	private void Assert(boolean x) {
		if (!x)
			throw new Exception("assertion failed");
	}
}



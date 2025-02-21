package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.FurnaceRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.files.CategoryData;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewer;
import com.dutchjelly.craftenhance.gui.guis.RecipesViewerCategorys;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.guis.viewers.RecipeViewRecipe;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.util.PermissionTypes;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.gui.util.FormatListContents.canSeeRecipes;

@CommandRoute(cmdPath = {"recipes", "ceh.viewer"}, perms = "perms.recipe-viewer")
public class RecipesCmd implements ICommand {

	private final CustomCmdHandler handler;

	public RecipesCmd(final CustomCmdHandler handler) {
		this.handler = handler;
	}

	@Override
	public String getDescription() {
		return "The view command opens an inventory that contains all available recipes for the sender of the command, unless it's configured to show all. The usage is /ceh view or /recipes";
	}

	@Override
	public void handlePlayerCommand(final Player p, final String[] args) {
		final RecipesViewerCategorys menu = new RecipesViewerCategorys("");
		if (args.length > 1) {
			if (args[0].equals("page"))
				try {
					final int pageIndex = Integer.parseInt(args[1]);
					final boolean b = menu.setPage(pageIndex);
					if (!b)
						p.sendMessage("Could not open this page " + args[1] + " , will open first page.");
					//gui.setPage(pageIndex); //setpage will handle invalid indexes and will jump to the nearest valid page
				} catch (final NumberFormatException e) {
					p.sendMessage("that's not a number " + args[1]);
				}
			if (args[0].equals("category")) {
				final CategoryData categoryData = self().getCategoryDataCache().get(args[1]);
				if (categoryData != null) {
					if (args.length > 2) {
						final EnhancedRecipe recipe = getRecipe(categoryData.getEnhancedRecipes(), args[2]);
						if (recipe instanceof WBRecipe) {
							if ( p.hasPermission(PermissionTypes.Edit.getPerm()))
								new RecipeEditor<>((WBRecipe) recipe,0, categoryData, null, ButtonType.ChooseWorkbenchType).menuOpen(p);
							else
								new RecipeViewRecipe<>(categoryData, 0,(WBRecipe) recipe, "WBRecipeViewer").menuOpen(p);
							return;
						}
						if (recipe  instanceof FurnaceRecipe) {
							if (p.hasPermission(PermissionTypes.Edit.getPerm()))
								new RecipeEditor<>((FurnaceRecipe) recipe,0, categoryData,null, ButtonType.ChooseFurnaceType).menuOpen(p);
							else
								new RecipeViewRecipe<>(categoryData, 0,(FurnaceRecipe) recipe, "FurnaceRecipeViewer").menuOpen(p);
							return;
						}
					}
					new RecipesViewer(categoryData, "", p)
							.menuOpen(p);

				}

				else {
					p.sendMessage("that's not a valid category " + args[1]);
				}
				return;
			}
		}
		menu.menuOpen(p);
	}

	@Override
	public List<String> handleTabCompletion(final CommandSender sender, final String[] args) {
		final List<String> list = new ArrayList<>();
		if (args.length == 2) {
			list.add("page");
			list.add("category");
		}
		if (args.length >= 3 && args[1].contains("category")) {
			if (args.length == 3 ) {
				list.addAll(self().getCategoryDataCache().getCategoryNames());
			}
			if (args.length == 4 ){
				self().getCategoryDataCache().values().forEach(categoryData ->
				list.addAll( recipeStringList(categoryData, (Player) sender)));
			}
		}
		return list;
	}

	@Nullable
	public EnhancedRecipe getRecipe(final List<EnhancedRecipe> recipes, final String recipe) {
		for (final EnhancedRecipe enhancedRecipe :recipes) {
			if (enhancedRecipe.getKey().equals(recipe))
				return enhancedRecipe;
		}
		return null;
	}

	public List<String> recipeStringList(final CategoryData categoryData, final Player player) {
		return canSeeRecipes(categoryData.getEnhancedRecipes(""), player).stream().map(EnhancedRecipe::getKey).collect(Collectors.toList());
	}

	@Override
	public void handleConsoleCommand(final CommandSender sender, final String[] args) {
		Messenger.MessageFromConfig("messages.commands.only-for-players", sender);
	}


}
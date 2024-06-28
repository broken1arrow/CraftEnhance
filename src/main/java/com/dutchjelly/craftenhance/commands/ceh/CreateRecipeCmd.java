package com.dutchjelly.craftenhance.commands.ceh;

import com.dutchjelly.craftenhance.commandhandling.CommandRoute;
import com.dutchjelly.craftenhance.commandhandling.CustomCmdHandler;
import com.dutchjelly.craftenhance.commandhandling.ICommand;
import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedRecipe;
import com.dutchjelly.craftenhance.crafthandling.recipes.utility.RecipeType;
import com.dutchjelly.craftenhance.crafthandling.recipes.WBRecipe;
import com.dutchjelly.craftenhance.gui.guis.EditorTypeSelector;
import com.dutchjelly.craftenhance.gui.guis.editors.RecipeEditor;
import com.dutchjelly.craftenhance.gui.util.ButtonType;
import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandRoute(cmdPath="ceh.createrecipe", perms="perms.recipe-editor")
public class CreateRecipeCmd implements ICommand {

	private CustomCmdHandler handler;
	
	public CreateRecipeCmd(CustomCmdHandler handler){
		this.handler = handler;
	}
	
	@Override
	public String getDescription() {
		return "The create recipe command allows users to create a recipe and open the editor of it. The usage is /ceh createrecipe [key] [permission]. You can leave both parameters empty. However, if you do want to customise recipe keys and permissions: the key has to be unique, and the permission can be empty to not have any permission. An example: /ceh createrecipe army_chest ceh.army-chest. The now created recipe has a key of army_chest and a permission of ceh.army-chest.";
	}
	@Override
	public void handlePlayerCommand(Player p, String[] args) {

        //Fill in a unique key and empty permission for the user.
	    if(args.length == 0){
	        int uniqueKeyIndex = 1;
	        while(!handler.getMain().getFm().isUniqueRecipeKey("recipe" + uniqueKeyIndex))
                uniqueKeyIndex++;
            WBRecipe newRecipe = new WBRecipe(null, null, new ItemStack[9]);
            newRecipe.setKey("recipe" + uniqueKeyIndex);
            /*EditorTypeSelector gui = new EditorTypeSelector(handler.getMain().getGuiManager(), handler.getMain().getGuiTemplatesFile().getTemplate(EditorTypeSelector.class), null, p, "recipe" + uniqueKeyIndex, null);
            handler.getMain().getGuiManager().openGUI(p, gui);*/
		    EditorTypeSelector guis  = new EditorTypeSelector("recipe" + uniqueKeyIndex, null);
		    guis.menuOpen(p);
	        return;
        }
		System.out.println("args " + Arrays.toString(args));
        //Use the input that the user gave.
		if(args.length == 1){
			args = addEmptyString(args);
		}
		else if(args.length != 2){
			Messenger.MessageFromConfig("messages.commands.few-arguments", p, "2");
			return;
		}
		
		if(!handler.getMain().getFm().isUniqueRecipeKey(args[0])){
            Messenger.Message("The specified recipe key isn't unique.", p);
			return;
		}

        WBRecipe newRecipe = new WBRecipe(args[1], null, new ItemStack[9]);
		ButtonType buttonType = ButtonType.ChooseWorkbenchType;
		if (newRecipe.getType() == RecipeType.FURNACE) {
			buttonType = ButtonType.ChooseFurnaceType;
			newRecipe = new WBRecipe(args[1], null, new ItemStack[1]);
		}
		newRecipe.setKey(args[0]);
		RecipeEditor<EnhancedRecipe> menu = new RecipeEditor<EnhancedRecipe>(newRecipe, null,null, buttonType);
		menu.menuOpen(p);
/*		WBRecipeEditor gui = new WBRecipeEditor(handler.getMain().getGuiManager(), handler.getMain().getGuiTemplatesFile().getTemplate(WBRecipeEditor.class), null, p, newRecipe);
		handler.getMain().getGuiManager().openGUI(p, gui);*/
	}

	@Override
	public void handleConsoleCommand(CommandSender sender, String[] args) {
        Messenger.MessageFromConfig("messages.commands.only-for-players", sender);
	}

	@Override
	public List<String> handleTabCompletion(final CommandSender sender, final String[] args) {
		List<String> tab = new ArrayList<>();
		if (args.length == 2){
			tab.add(ButtonType.ChooseWorkbenchType.name());
			tab.add(ButtonType.ChooseFurnaceType.name());
			tab.add(ButtonType.ChooseBlastType.name());
			tab.add(ButtonType.ChooseSmokerType.name());
		}
		if (args.length == 3){
			tab.add("key");
		}
		return tab;
	}

	//Create a new array object so return that.
	private String[] addEmptyString(String[] args){
		String[] newArray = new String[args.length + 1];
		newArray[0] = args[0];
		newArray[1] = "";
		return newArray;
	}
}

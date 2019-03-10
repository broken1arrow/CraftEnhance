# CraftEnhance

This is the repository of CraftEnhance. 

The basic idea of the plugin is that it allows users to make custom recipes on minecraft servers that run on the bukkit/spigot API. One thing that this plugin is unique in is that it allows items in recipes to have metadata like enchantments or custom names. It also features full GUI support, which makes in-game recipe editing/creation possible.

The plugin is properly documented here for users: https://dev.bukkit.org/projects/craftenhance.

If you want to use this plugin as an API, you need to get the instance of the main JavaPlugin extended class. This contains all objects that get used. For now, as I don't have a proper documentation on the API part, you can just look at the source code to see what everything does.

Some examples of why I would use this plugin as an API in certain cases is to:
 - Access the GUI system.
 - Use the messaging system.
 - Use the command handler.
 - Create own recipes programatically.
 - Use the debugger/messenger.
 - Add own GUI's by extending the GUIElement interface class.
 
These are some of the TODO's including future features. Any help in doing those will be appreciated.
 - TODO: Remove functions for checking if the recipe is equal to some matrix of items from the Recipe class and add it to the RecipeInjector class.
 - TODO: Add a class extending Recipe that handles "lore-upgrading".
 - TODO: Add optional category property to the Recipe class.
 - TODO: Add "hide" property to Recipe class. Useful for recipes that don't need to be shown, maybe useful for the lore upgrade feature.
 - TODO: Make the glass panes in the release that's built for all spigot versions other color than white.
 
 
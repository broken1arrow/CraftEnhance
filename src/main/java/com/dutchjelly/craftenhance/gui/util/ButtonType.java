package com.dutchjelly.craftenhance.gui.util;

public enum ButtonType {
    NxtPage(""),
    PrvPage(""),
    Back(""),
    SaveRecipe(""),
    DeleteRecipe(""),
    ChangeCategory(""),
    SwitchShaped(""),
    SwitchMatchMeta(""),
    ResetRecipe(""),
    SetPosition(""),
    SwitchHidden(""),
    SetPermission(""),
    SwitchDisablerMode(""),
    SetCookTime(""),
    SetExp(""),
    ChooseWorkbenchType("WBRecipeEditor"),
    ChooseFurnaceType("FurnaceRecipeEditor"),
    Search("");

    private String type;

    ButtonType(String type) {

        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ButtonType valueOfType(String buttontype) {
        ButtonType[] buttonTypes = ButtonType.values();
        for (ButtonType buttonType : buttonTypes) {
            if (buttonType.name().equalsIgnoreCase(buttontype))
                return buttonType;
        }
        return null;
    }
    }

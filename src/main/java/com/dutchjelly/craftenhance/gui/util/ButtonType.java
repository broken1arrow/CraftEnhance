package com.dutchjelly.craftenhance.gui.util;

public enum ButtonType {
    NxtPage,
    PrvPage,
    Back,
    SaveRecipe,
    DeleteRecipe,
    ChangeCategory,
    SwitchShaped,
    SwitchMatchMeta,
    ResetRecipe,
    SetPosition,
    SwitchHidden,
    SetPermission,
    SwitchDisablerMode,
    ChooseWorkbenchType,
    SetCookTime,
    SetExp,
    ChooseFurnaceType,
    Search;

    public static ButtonType valueOfType(String buttontype) {
        ButtonType[] buttonTypes = ButtonType.values();
        for (ButtonType buttonType : buttonTypes) {
            if (buttonType.name().equalsIgnoreCase(buttontype))
                return buttonType;
        }
        return null;
    }
    }

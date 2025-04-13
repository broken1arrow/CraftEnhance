package com.dutchjelly.craftenhance.gui.util;

public enum InfoItemPlaceHolders {
    MatchMeta("[matcher]"),
    MatchDescription("[match_description]"),
    Shaped("[shaped]"),
    Hidden("[hidden]"),
    Permission("[permission]"),
    Config_permission("[config_permission]"),
    Key("[key]"),
    Slot("[slot]"),
    DisableMode("[mode]"),
    Exp("[exp]"),
    Duration("[duration]"),
    Page("[page]"),
    Category("[category]"),
    DisplayName("[display_name]"),
    Worlds("[worlds]"),
    Partial_match("[partial_match]"),
    Recipe_type("[Recipe_type]");
    private final String placeHolder;

    public String getPlaceHolder(){
        return placeHolder;
    }
    InfoItemPlaceHolders(final String placeHolder){
        this.placeHolder = placeHolder;
    }
}

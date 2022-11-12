package com.dutchjelly.craftenhance.gui.util;

public enum InfoItemPlaceHolders {
    MatchMeta("[match meta]"),
    MatchType("[match type]"),
    Shaped("[shaped]"),
    Hidden("[hidden]"),
    Permission("[permission]"),
    Key("[key]"),
    Slot("[slot]"),
    DisableMode("[mode]"),
    Exp("[exp]"),
    Duration("[duration]"),
    Page("[page]"),
    Category("[category]"),
    DisplayName("[display_name]");

    private final String placeHolder;

    public String getPlaceHolder(){
        return placeHolder;
    }
    InfoItemPlaceHolders(String placeHolder){
        this.placeHolder = placeHolder;
    }
}

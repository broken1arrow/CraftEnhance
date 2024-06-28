package com.dutchjelly.craftenhance.crafthandling.recipes.utility;

import lombok.Getter;

public enum MatchType {
    META("match meta"), MATERIAL("only match type"), NAME("only match name and type");

    @Getter
    private String description;

    MatchType(String s) {
        description = s;
    }
}
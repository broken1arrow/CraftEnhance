package com.dutchjelly.craftenhance.util;

public enum SortOrder {
	NON,
	NAME,
	ID,
	MATCH_TYPE,
	RECIPE_TYPE,
	GROUP;


	public SortOrder nextValue() {
		SortOrder[] sortOrder = values();
		if (this.ordinal() + 1 >= sortOrder.length) {
			return NON;
		}
		return sortOrder[this.ordinal() + 1];
	}

}

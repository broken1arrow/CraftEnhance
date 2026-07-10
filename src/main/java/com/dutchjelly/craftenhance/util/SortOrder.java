package com.dutchjelly.craftenhance.util;

public enum SortOrder {
	NON,
	NAME,
	RESULT_MATERIAL_NAME,
	ID,
	MATCH_TYPE,
	RECIPE_TYPE,
	GROUP, IS_HIDDEN;


	public SortOrder nextValue() {
		SortOrder[] sortOrder = values();
		if (this.ordinal() + 1 >= sortOrder.length) {
			return NON;
		}
		return sortOrder[this.ordinal() + 1];
	}

}

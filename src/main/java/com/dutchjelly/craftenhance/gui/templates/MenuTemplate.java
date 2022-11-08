package com.dutchjelly.craftenhance.gui.templates;

import java.util.List;
import java.util.Map;

public class MenuTemplate {

	private final String menuTitel;
	private final List<Integer> fillSlots;
	private final Map<List<Integer>, MenuButton> menuButtons;

	public MenuTemplate(String menuTitel, List<Integer> fillSlots, Map<List<Integer>, MenuButton> menuButtons) {
		this.menuTitel = menuTitel;
		this.fillSlots = fillSlots;
		this.menuButtons = menuButtons;
	}

	public String getMenuTitel() {
		return menuTitel;
	}

	public List<Integer> getFillSlots() {
		return fillSlots;
	}

	public Map<List<Integer>, MenuButton> getMenuButton() {
		return menuButtons;
	}
}

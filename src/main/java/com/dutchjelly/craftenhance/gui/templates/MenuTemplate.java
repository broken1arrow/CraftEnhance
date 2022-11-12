package com.dutchjelly.craftenhance.gui.templates;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MenuTemplate {

	private final String menuTitel;
	private final List<Integer> fillSlots;
	private final Map<List<Integer>, MenuButton> menuButtons;
	private final int amountOfButtons;
	public MenuTemplate(String menuTitel, List<Integer> fillSlots, Map<List<Integer>, MenuButton> menuButtons) {
		this.menuTitel = menuTitel;
		this.fillSlots = fillSlots;
		this.menuButtons = menuButtons;
		this.amountOfButtons = calculateAmountOfButtons( menuButtons);
	}

	public int getAmountOfButtons() {
		return amountOfButtons;
	}

	public String getMenuTitel() {
		return menuTitel;
	}

	public List<Integer> getFillSlots() {
		return fillSlots;
	}

	public Map<List<Integer>, MenuButton> getMenuButtons() {
		return menuButtons;
	}

	public MenuButton getMenuButton(int slot) {
		for (Entry<List<Integer>, MenuButton> slots : menuButtons.entrySet()) {
			for (int menuSlot : slots.getKey())
				if (menuSlot == slot)
					return slots.getValue();
		}
		return null;
	}
	public int calculateAmountOfButtons(Map<List<Integer>, MenuButton> menuButtons) {
		int amountOfButtons = 0;
		for (List<Integer> slots : menuButtons.keySet()){
			amountOfButtons += slots.size();
		}
		return amountOfButtons;
	}

}

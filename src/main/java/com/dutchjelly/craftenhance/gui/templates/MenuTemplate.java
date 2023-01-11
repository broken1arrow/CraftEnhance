package com.dutchjelly.craftenhance.gui.templates;

import com.dutchjelly.craftenhance.util.SoundUtillity;
import org.bukkit.Sound;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MenuTemplate {

	private final String menuTitel;
	private final List<Integer> fillSlots;
	private final Map<List<Integer>, MenuButton> menuButtons;
	private final int amountOfButtons;
	private final Sound sound;

	public MenuTemplate(final String menuTitel, final List<Integer> fillSlots, final Map<List<Integer>, MenuButton> menuButtons, final String sound) {
		this.menuTitel = menuTitel;
		this.fillSlots = fillSlots;
		this.menuButtons = menuButtons;
		this.amountOfButtons = calculateAmountOfButtons( menuButtons);
		this.sound = SoundUtillity.getSound( sound);
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

	public Sound getSound() {
		return sound;
	}

	public MenuButton getMenuButton(final int slot) {
		for (final Entry<List<Integer>, MenuButton> slots : menuButtons.entrySet()) {
			for (final int menuSlot : slots.getKey())
				if (menuSlot == slot)
					return slots.getValue();
		}
		return null;
	}
	public int calculateAmountOfButtons(final Map<List<Integer>, MenuButton> menuButtons) {
		int amountOfButtons = 0;
		for (final List<Integer> slots : menuButtons.keySet()){
			amountOfButtons += slots.size();
		}
		return amountOfButtons;
	}

}

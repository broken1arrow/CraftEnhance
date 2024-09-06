package com.dutchjelly.craftenhance.gui;

import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.gui.guis.GUIElement;
import com.dutchjelly.craftenhance.gui.interfaces.IChatInputHandler;
import com.dutchjelly.craftenhance.messaging.Debug;
import com.dutchjelly.craftenhance.messaging.Messenger;
import com.dutchjelly.craftenhance.util.Pair;
import org.broken.arrow.menu.library.holder.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager implements Listener {

	//TODO: make gui's check which user clicks on button click instead of using the stored user.
	private static final int MaxPreviousPageBuffer = 20;

	private final Map<UUID, Pair<GUIElement, IChatInputHandler>> chatWaiting = new HashMap<>();
	private final Map<UUID, Pair<MenuHolder, IChatInputHandler>> chatWaitingCopy = new HashMap<>();
	private final CraftEnhance main;

	public GuiManager(final CraftEnhance main) {
		this.main = main;
	}

	public CraftEnhance getMain() {
		return main;
	}


	@EventHandler
	public void onDrag(final InventoryDragEvent e) {
/*		if (!(e.getView().getTopInventory().getHolder() instanceof GUIElement)) return;
		final GUIElement openGUI = (GUIElement) e.getView().getTopInventory().getHolder();
		if (openGUI == null || e.getInventory() == null) return;

		try {
			openGUI.handleDragging(e);
			if (!openGUI.isCancelResponsible() && !e.isCancelled())
				e.setCancelled(true);
		} catch (final Exception exception) {
			exception.printStackTrace();
			if (!e.isCancelled())
				e.setCancelled(true);
		}*/
	}

	@EventHandler
	public void onClick(final InventoryClickEvent clickEvent) {

/*		if (!(clickEvent.getView().getTopInventory().getHolder() instanceof GUIElement)) return;
		final GUIElement openGUI = (GUIElement) clickEvent.getView().getTopInventory().getHolder();

		if (openGUI == null) return;

		try {
			if (clickEvent.getClickedInventory() != null && clickEvent.getClickedInventory().equals(openGUI.getInventory()))
				openGUI.handleEvent(clickEvent);
			else openGUI.handleOutsideClick(clickEvent);

			if (!openGUI.isCancelResponsible() && !clickEvent.isCancelled())
				clickEvent.setCancelled(true);

		} catch (final Exception exception) {
			exception.printStackTrace();
			if (!clickEvent.isCancelled())
				clickEvent.setCancelled(true);
		}*/
	}

	@EventHandler
	public void onChat(final AsyncPlayerChatEvent e) {
		if (e.getPlayer() == null) return;

		/*if (chatWaiting(e))
			e.setCancelled(true);*/
	}
	@EventHandler
	public void onChatold(final AsyncPlayerChatEvent e) {
		if (e.getPlayer() == null) return;

/*		if (chatWaitingOld(e))
			e.setCancelled(true);*/
	}
	public boolean chatWaiting(final AsyncPlayerChatEvent e){
		final UUID id = e.getPlayer().getUniqueId();
		if (!chatWaitingCopy.containsKey(id)) return false;

		Bukkit.getScheduler().runTask(getMain(), () -> {
			final IChatInputHandler callback = chatWaitingCopy.get(id).getSecond();
			if (callback.handle(e.getMessage())) return;
			final MenuHolder gui = chatWaitingCopy.get(id).getFirst();
		/*	if (gui != null)
				gui.menuOpen(e.getPlayer());*/
			chatWaitingCopy.remove(id);
		});
		return true;
	}
	public boolean chatWaitingOld(final AsyncPlayerChatEvent e){
		final UUID id = e.getPlayer().getUniqueId();
		if (!chatWaiting.containsKey(id)) return false;

		Bukkit.getScheduler().runTask(getMain(), () -> {
			final IChatInputHandler callback = chatWaiting.get(id).getSecond();
			if (callback.handle(e.getMessage())) return;
			final GUIElement gui = chatWaiting.get(id).getFirst();
			if (gui != null)
				openGUI(e.getPlayer(), gui);
			chatWaiting.remove(id);
		});
		return true;
	}
	public void openGUI(final Player p, final GUIElement gui) {
		if (countPreviousPages(gui) >= MaxPreviousPageBuffer) {
			Messenger.Message("For performance reasons you cannot open more gui's in that chain (the server keeps track of the previous gui's so you can go back).", p);
			return;
		}
		if (gui == null) {
			Debug.Send("trying to open null gui...");
			return;
		}
		Debug.Send("Opening a gui element: " + gui.getClass().getName());
		p.openInventory(gui.getInventory());
	}

	private int countPreviousPages(GUIElement gui) {
		if (gui == null) return 0;
		int counter = 0;
		while (gui != null) {
			gui = gui.getPreviousGui();
			counter++;
		}

		return counter;
	}

	public void waitForChatInput(final GUIElement gui, final Player p, final IChatInputHandler callback) {
		final UUID playerId = p.getUniqueId();
		p.closeInventory();
		chatWaiting.put(playerId, new Pair(gui, callback));
	}
	public void waitForChatInput(final MenuHolder gui, final Player p, final IChatInputHandler callback) {
		final UUID playerId = p.getUniqueId();
		p.closeInventory();
		chatWaitingCopy.put(playerId, new Pair(gui, callback));
	}
}

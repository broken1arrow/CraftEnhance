package com.dutchjelly.craftenhance.api.event;

import com.dutchjelly.craftenhance.messaging.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 *
 * All events require a static method named getHandlerList() which returns the same {@link HandlerList} as {@link #getHandlers()}.
 * You need create this static method in every class some extend this event and you need also {@link this#registerEvent()}
 */

public abstract class EventUtility extends Event implements Cancellable {


	private final HandlerList handler;

	/**
	 * The default constructor is defined for cleaner code. This constructor
	 * assumes the event is synchronous.
	 */
	public EventUtility(final HandlerList handler) {
		this(handler,false);
	}

	/**
	 * This constructor is used to explicitly declare an event as synchronous
	 * or asynchronous.
	 *
	 * @param isAsync true indicates the event will fire asynchronously, false
	 *                by default from default constructor
	 */
	public EventUtility(final HandlerList handler, final boolean isAsync) {
		super(isAsync);
		this.handler = handler;
	}

	/**
	 * After you set the data you also need register the event in the constructor or were you use it.
	 */
	public void registerEvent() {
		Bukkit.getPluginManager().callEvent(this);
	}

	/**
	 * Gets the cancellation state of this event. A cancelled event will not
	 * be executed in the server, but will still pass to other plugins
	 *
	 * @return true if this event is cancelled
	 */
	@Override
	public boolean isCancelled() {
		return false;
	}

	/**
	 * Sets the cancellation state of this event. A cancelled event will not
	 * be executed in the server, but will still pass to other plugins.
	 *
	 * @param cancel true if you wish to cancel this event
	 */
	@Override
	public void setCancelled(final boolean cancel) {
		Messenger.Error("You can't cancel this event.");
	}

	@Override
	@Nonnull
	public HandlerList getHandlers() {
		return handler;
	}

}
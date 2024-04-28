package com.dutchjelly.craftenhance.prompt;

import com.dutchjelly.craftenhance.gui.interfaces.IChatInputHandler;
import org.broken.arrow.menu.library.holder.HolderUtility;
import org.broken.arrow.prompt.library.SimpleConversation;
import org.broken.arrow.prompt.library.SimplePrompt;
import org.broken.arrow.prompt.library.utility.SimpleCanceller;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dutchjelly.craftenhance.CraftEnhance.self;
import static com.dutchjelly.craftenhance.messaging.Messenger.Message;
import static com.dutchjelly.craftenhance.messaging.Messenger.getMessage;

public class HandleChatInput extends SimpleConversation {


	private final HolderUtility<?> menuHolder;
	private final List<String> messages;
	private final IChatInputHandler chatInputHandler;

	public HandleChatInput(final HolderUtility<?> menuHolder, final IChatInputHandler callback) {
		super(self());
		this.menuHolder = menuHolder;
		this.messages = new ArrayList<>();
		this.chatInputHandler = callback;
	}

	public HandleChatInput setMessages(final String... messages) {
		this.messages.addAll(Arrays.asList(messages));
		return this;
	}

	@Override
	public int getTimeout() {
		return 120;
	}

	@Override
	protected void onConversationEnd(final SimpleConversation conversation, final ConversationAbandonedEvent event) {
		Message("Quit the prompt ", (CommandSender) event.getContext().getForWhom());
	}

	@Override
	protected ConversationCanceller getCanceller()  {
		return new SimpleCanceller("non");
	}

	@Override
	public Prompt getFirstPrompt() {
		return new Action(menuHolder, messages, chatInputHandler);
	}

	public static class Action extends SimplePrompt {
		private final HolderUtility<?> menuHolder;
		private final List<String> messages;
		private final IChatInputHandler chatInputHandler;

		public Action(final HolderUtility<?> menuHolder, final List<String> message, final IChatInputHandler callback) {
			this.menuHolder = menuHolder;
			this.messages = message;
			this.chatInputHandler = callback;
		}

		@Override
		protected String getPrompt(final ConversationContext conversationContext) {
			Player player = getPlayer();
			if (conversationContext.getForWhom() instanceof Player)
				player = getPlayer(conversationContext);
			final String lastMessage = this.messages.get(this.messages.size() - 1);
			for (final String message : this.messages) {
				if (message.equals(lastMessage)) break;
				Message(message, player);
			}
			return getMessage(lastMessage, player);
		}

		@Nullable
		@Override
		protected Prompt acceptValidatedInput(@Nonnull final ConversationContext context, @Nonnull final String input) {

			final IChatInputHandler callback = this.chatInputHandler;
			if (callback.handle(input)) {
				return new Action(this.menuHolder, this.messages, callback);
			}
			final HolderUtility<?> gui = menuHolder;
		/*	if (gui != null)
				gui.menuOpen(e.getPlayer());*/

			return null;
		}


	}
}

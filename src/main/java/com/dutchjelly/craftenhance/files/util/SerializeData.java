package com.dutchjelly.craftenhance.files.util;

import com.google.gson.Gson;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.broken.arrow.library.menu.utility.ServerVersion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dutchjelly.craftenhance.files.util.SerializeingLocation.serializeLoc;

public class SerializeData {

	public static Object serialize(final Object obj) {
		if (obj == null)
			return null;

		else if (obj instanceof ChatColor)
			return ((ChatColor) obj).name();

		else if (obj instanceof net.md_5.bungee.api.ChatColor) {
			final net.md_5.bungee.api.ChatColor color = ((net.md_5.bungee.api.ChatColor) obj);

			return ServerVersion.atLeast(ServerVersion.V1_16) ? color.toString() : color.name();
		} else if (obj instanceof Location)
			return serializeLoc((Location) obj);

		else if (obj instanceof UUID)
			return obj.toString();

		else if (obj instanceof Enum<?>)
			return obj.toString();

		else if (obj instanceof CommandSender)
			return ((CommandSender) obj).getName();

		else if (obj instanceof World)
			return ((World) obj).getName();

		else if (obj instanceof PotionEffect)
			return ((PotionEffect) obj).serialize();

		else if (obj instanceof java.awt.Color)
			return "#" + ((java.awt.Color) obj).getRGB();

		else if (obj instanceof BaseComponent)
			return toJson((BaseComponent) obj);

		else if (obj instanceof BaseComponent[])
			return toJson((BaseComponent[]) obj);

		else if (obj instanceof HoverEvent) {
			final HoverEvent event = (HoverEvent) obj;
			final Map<String, Object> serialize = new HashMap<>();
			serialize.put("Action", event.getAction().name());
			serialize.put("Value", Arrays.stream(event.getValue().clone()).map(BaseComponent::toString).collect(Collectors.toList()));
			return serialize;
		} else if (obj instanceof ClickEvent) {
			final ClickEvent event = (ClickEvent) obj;
			final Map<String, Object> serialize = new HashMap<>();
			serialize.put("Action", event.getAction().name());
			serialize.put("Value", event.getValue());
			return serialize;
		} else if (obj instanceof Iterable || obj.getClass().isArray()) {
			final List<Object> serialized = new ArrayList<>();

			if (obj instanceof Iterable)
				for (final Object element : (Iterable<?>) obj)
					serialized.add(serialize(element));
			else
				for (final Object element : (Object[]) obj)
					serialized.add(serialize(element));

			return serialized;
		} else if (obj instanceof Map) {
			final Map<?, ?> oldMap = (Map<?, ?>) obj;
			final Map<Object, Object> newMap = new LinkedHashMap<>();

			for (final Map.Entry<?, ?> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;
		} else if (obj instanceof Integer || obj instanceof Double || obj instanceof Float || obj instanceof Long || obj instanceof Short
				|| obj instanceof String || obj instanceof Boolean || obj instanceof ItemStack || obj instanceof MemorySection
				|| obj instanceof Pattern)
			return obj;

		else if (obj instanceof ConfigurationSerializable)
			return ((ConfigurationSerializable) obj).serialize();

		throw new SerializeFailedException("Does not know how to serialize " + obj.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + obj);
	}

	public static String toJson(final BaseComponent... comps) {
		if (ServerVersion.olderThan(ServerVersion.V1_7))
			return "{}";
		String json;
		try {
			json = ComponentSerializer.toString(comps);
		} catch (final Throwable var3) {
			json = (new Gson()).toJson((new TextComponent(comps)).toLegacyText());
		}

		return json;
	}

	public static class SerializeFailedException extends RuntimeException {
		private static final long serialVersionUID = 2L;

		public SerializeFailedException(final String reason) {
			super(reason);
		}

	}
}

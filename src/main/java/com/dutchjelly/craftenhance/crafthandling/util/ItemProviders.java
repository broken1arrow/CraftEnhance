package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.craftenhance.messaging.Debug;
import com.saicone.rtag.RtagItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemProviders {

    private static final Map<String, List<Object[]>> PROVIDERS = new HashMap<>();

    public static void init(final ConfigurationSection providers) {
        PROVIDERS.clear();
        if (providers == null) {
            return;
        }
        for (String key : providers.getKeys(false)) {
            readProvider(key, providers.get(key));
        }
    }

    private static void readProvider(final String key, final Object obj) {
        final List<Object[]> paths;
        if (obj instanceof String) {
            if (((String) obj).trim().isEmpty()) {
                Debug.Send("The id provider '" + key + "' is empty");
                return;
            }

            paths = new ArrayList<>();
            paths.add(readPath((String) obj));
        } else if (obj instanceof Iterable) {
            paths = new ArrayList<>();

            for (Object o : (Iterable<?>) obj) {
                if (o == null) {
                    continue;
                }
                final String s = String.valueOf(o);
                if (s.trim().isEmpty()) {
                    continue;
                }
                paths.add(readPath(s));
            }

            if (paths.isEmpty()) {
                Debug.Send("The id provider '" + key + "' is empty");
                return;
            }
        } else {
            Debug.Send("The id provider '" + key + "' use an invalid data type");
            return;
        }
        Debug.Send("The NBT matcher '" + key + "' has been loaded with paths = " + paths);
        PROVIDERS.put(key, paths);
    }

    private static Object[] readPath(final String s) {
        final String[] split = s.replace("\\.", "<dot>").split("\\.");
        final Object[] path = new Object[split.length];
        for (int i = 0; i < split.length; i++) {
            String pathKey = split[i].replace("<dot>", ".");
            if (pathKey.startsWith("[") && pathKey.endsWith("]") && pathKey.length() >= 3) {
                final String num = pathKey.substring(1, pathKey.length() - 1);
                try {
                    final int index = Integer.parseInt(num);
                    path[i] = index;
                    continue;
                } catch (NumberFormatException ignored) { }
            }
            path[i] = pathKey;
        }
        return path;
    }

    public static String getProvider(final ItemStack item) {
        if (item == null) {
            return null;
        }
        for (Map.Entry<String, List<Object[]>> entry : PROVIDERS.entrySet()) {
            final RtagItem tag = new RtagItem(item);
            boolean contains = true;
            for (Object[] path : entry.getValue()) {
                if (tag.notHasTag(path)) {
                    contains = false;
                    break;
                }
            }
            if (contains) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static List<Object> getComparison(final ItemStack item, final String key) {
        final List<Object[]> paths = PROVIDERS.get(key);
        if (paths == null || paths.isEmpty()) {
            return new ArrayList<>();
        }
        final List<Object> comparison = new ArrayList<>();
        final RtagItem tag = new RtagItem(item);
        for (Object[] path : paths) {
            comparison.add(tag.get(path));
        }
        return comparison;
    }

    public static boolean match(final ItemStack a, final ItemStack b) {
        for (Map.Entry<String, List<Object[]>> entry : PROVIDERS.entrySet()) {
            if (match(a, b, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static boolean match(final ItemStack a, final ItemStack b, final String key) {
        final List<Object[]> paths = PROVIDERS.get(key);
        return paths != null && match(a, b, paths);
    }

    public static boolean match(final ItemStack a, final String key, final List<Object> comparison) {
        final List<Object[]> paths = PROVIDERS.get(key);
        if (paths == null || paths.size() != comparison.size()) {
            return false;
        }
        final RtagItem tag = new RtagItem(a);
        for (int i = 0; i < paths.size(); i++) {
            final Object objectA = tag.get(paths.get(i));
            final Object objectB;
            if (objectA == null || (objectB = comparison.get(i)) == null || !Objects.deepEquals(objectA, objectB)) {
                return false;
            }
        }
        return true;
    }

    public static boolean match(final ItemStack a, final ItemStack b, final List<Object[]> paths) {
        final RtagItem tagA = new RtagItem(a);
        final RtagItem tagB = new RtagItem(b);

        for (Object[] path : paths) {
            final Object objectA = tagA.get(path);
            final Object objectB;
            if (objectA == null || (objectB = tagB.get(path)) == null || !Objects.deepEquals(objectA, objectB)) {
                return false;
            }
        }
        return true;
    }
}

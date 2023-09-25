package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedItem;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface IMatcher<T extends ItemStack> {

    boolean match(T a, T b);

    @SuppressWarnings("unchecked")
    default boolean match(EnhancedItem a, T b) {
        if (a.getProvider() != null) {
            return a.match(b);
        }
        return match((T) a.getItem(), b);
    }

    @SuppressWarnings("unchecked")
    default boolean match(T a, EnhancedItem b) {
        if (b.getProvider() != null) {
            return b.match(a);
        }
        return match(a, (T) b.getItem());
    }

    @SuppressWarnings("unchecked")
    default boolean match(Object a, Object b) {
        if (a instanceof EnhancedItem) {
            return match((EnhancedItem) a, (T) b);
        } else if (b instanceof EnhancedItem) {
            return match((T) a, (EnhancedItem) b);
        }
        return match((T) a, (T) b);
    }
}

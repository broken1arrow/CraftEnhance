package com.dutchjelly.craftenhance.crafthandling.recipes;

import com.dutchjelly.craftenhance.crafthandling.util.ItemProviders;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EnhancedItem {

    @Getter
    private final ItemStack item;
    @Getter
    private final String provider;
    private final List<Object> comparison;

    public static EnhancedItem[] of(ItemStack[] array) {
        final EnhancedItem[] items = new EnhancedItem[array.length];
        for (int i = 0; i < array.length; i++) {
            items[i] = new EnhancedItem(array[i]);
        }
        return items;
    }

    public EnhancedItem() {
        this(null, null);
    }

    public EnhancedItem(final ItemStack item) {
        this(item, ItemProviders.getProvider(item));
    }

    public EnhancedItem(final ItemStack item, final String provider) {
        this.item = item;
        this.provider = provider;
        this.comparison = provider == null ? null : ItemProviders.getComparison(item, provider);
    }

    public boolean match(final ItemStack item) {
        return item != null && ItemProviders.match(item, provider, comparison);
    }

    public boolean equals(ItemStack item) {
        if (item == null) {
            return this.item == null;
        }
        if (this.item == null) {
            return false;
        }
        if (provider != null) {
            return ItemProviders.match(item, this.item, provider);
        }
        return this.item.equals(item);
    }

    @Override
    public String toString() {
        return item.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ItemStack) {
            return equals((ItemStack) o);
        }
        if (o == null || getClass() != o.getClass()) return false;

        EnhancedItem that = (EnhancedItem) o;

        return equals(that.item);
    }

    @Override
    public int hashCode() {
        return item != null ? item.hashCode() : 0;
    }
}

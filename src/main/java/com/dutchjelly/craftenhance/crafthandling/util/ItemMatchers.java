package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.util.StripColors;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class ItemMatchers {

    public enum MatchType {

        MATCH_TYPE(constructIMatcher(ItemMatchers::matchType), "match type"),
        MATCH_META(constructIMatcher(ItemMatchers::matchMeta), "match meta"),
        MATCH_NAME(constructIMatcher(ItemMatchers::matchName), "match name"),
        MATCH_MODELDATA_AND_TYPE(constructIMatcher(ItemMatchers::matchType, ItemMatchers::matchModelData), "match modeldata and type"),
        MATCH_NAME_LORE(constructIMatcher(ItemMatchers::matchNameLore), "match name and lore");
        //        MATCH_ITEMSADDER()
//        MATCH_NAME_AND_TYPE(constructIMatcher(ItemMatchers::matchName, ItemMatchers::matchType), "match name and type");

        @Getter
        private final IMatcher matcher;

        @Getter
        private final String description;


        MatchType(IMatcher matcher, String description) {
            this.matcher = matcher;
            this.description = description;
        }
    }

    private static boolean backwardsCompatibleMatching = false;

    public static void init(boolean backwardsCompatibleMatching) {
        ItemMatchers.backwardsCompatibleMatching = backwardsCompatibleMatching;
    }

    public static boolean matchItems(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == null && b == null;
        return a.equals(b);
    }

    public static boolean matchModelData(ItemStack a, ItemStack b) {
        ItemMeta am = a.getItemMeta(), bm = b.getItemMeta();
        if (am == null) return bm == null || !bm.hasCustomModelData();
        if (bm == null) return am == null || !am.hasCustomModelData();
        return am.hasCustomModelData() == bm.hasCustomModelData() && (!am.hasCustomModelData() || am.getCustomModelData() == bm.getCustomModelData());
    }

    public static boolean matchMeta(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == null && b == null;
        boolean canUseModeldata = Adapter.canUseModeldata();

        if (backwardsCompatibleMatching) {
            return a.getType().equals(b.getType()) && a.getDurability() == b.getDurability() && a.hasItemMeta() == b.hasItemMeta() && (!a.hasItemMeta() || (
                    a.getItemMeta().toString().equals(b.getItemMeta().toString()))
                    && (canUseModeldata && matchModelData(a, b) || !canUseModeldata)
            );
        }

        return a.isSimilar(b) && (!canUseModeldata || matchModelData(a, b));
    }

    public static boolean matchType(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == null && b == null;
        return a.getType().equals(b.getType());
    }

    public static <T> IMatcher<T> constructIMatcher(IMatcher<T>... matchers) {
        return (a, b) -> Arrays.stream(matchers).allMatch(x -> x.match(a, b));
    }

    public static boolean matchCustomModelData(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == null && b == null;

        if (a.hasItemMeta() && b.hasItemMeta()) {
            ItemMeta itemMetaA = a.getItemMeta();
            ItemMeta itemMetaB = b.getItemMeta();
            if (itemMetaA != null && itemMetaB != null && itemMetaA.hasCustomModelData() && itemMetaB.hasCustomModelData())
                return itemMetaA.getCustomModelData() == itemMetaB.getCustomModelData();
        }
        return false;
    }

    public static boolean matchTypeData(ItemStack a, ItemStack b) {

        if (a == null || b == null) return a == null && b == null;

        if (self().getVersionChecker().olderThan(VersionChecker.ServerVersion.v1_14)) {
            if (a.getData() == null && b.getData() == null)
                return matchType(a, b);
            return a.getData().equals(b.getData());
        } else {
            if (a.hasItemMeta() && b.hasItemMeta()) {
                return matchCustomModelData(a, b) || matchType(a, b);
            }
            return matchType(a, b);
        }
    }

    public static boolean matchName(ItemStack a, ItemStack b) {
        if (a.hasItemMeta() && b.hasItemMeta()) {
            return a.getItemMeta().getDisplayName().equals(b.getItemMeta().getDisplayName());
        }
        //neither has item meta, and type has to match
        return a.hasItemMeta() == b.hasItemMeta() && a.getType() == b.getType();
    }

    public static boolean matchNameLore(ItemStack a, ItemStack b) {
        if (a.hasItemMeta() && b.hasItemMeta()) {
            ItemMeta itemMetaA = a.getItemMeta();
            ItemMeta itemMetaB = b.getItemMeta();

            if (itemMetaA != null && itemMetaB != null) {
                boolean hasSameLore = itemMetaA.getLore() == null || itemMetaA.getLore().equals(itemMetaB.getLore());
                if (!hasSameLore)
                    hasSameLore = StripColors.stripLore(itemMetaA.getLore()).equals(StripColors.stripLore(itemMetaB.getLore()));

                return itemMetaA.getDisplayName().equals(itemMetaB.getDisplayName()) && hasSameLore;
            }
        }
        //neither has item meta, and type has to match
        return a.hasItemMeta() == b.hasItemMeta() && a.getType() == b.getType();
    }
//    public static boolean matchItemsadderItems(ItemStack a, ItemStack b) {
//        CustomStack stack = CustomStack.byItemStack(myItemStack);
//        CustomStack
//    }
}

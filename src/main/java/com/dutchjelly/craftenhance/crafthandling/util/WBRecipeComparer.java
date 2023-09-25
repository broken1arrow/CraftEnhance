package com.dutchjelly.craftenhance.crafthandling.util;

import com.dutchjelly.craftenhance.crafthandling.recipes.EnhancedItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.logging.Level;

public class WBRecipeComparer {

    private static ItemStack item(Object o) {
        if (o instanceof EnhancedItem) {
            return ((EnhancedItem) o).getItem();
        }
        return (ItemStack) o;
    }

    private static boolean isItem(Object o) {
        if (o instanceof EnhancedItem) {
            return ((EnhancedItem) o).getItem() != null && ((EnhancedItem) o).getItem().getType() != Material.AIR;
        }
        return o instanceof ItemStack && ((ItemStack) o).getType() != Material.AIR;
    }

    private static Object[] mirror(final Object[] content, final int size){
        if(content == null) return null;
        if(content.length == 0) return content;
        final Object[] mirrored = new Object[content.length];


        for(int i = 0; i < size; i++){

            //Walk through right and left elements of this row and swab them.
            for(int j = 0; j < size/2; j++){
                final int i1 = i * size + (size - j - 1);
                mirrored[i*size+j] = content[i1];
                mirrored[i1] = content[i*size+j];
            }

            //Copy middle item to mirrored.
            if(size%2 != 0)
                mirrored[i*size+(size/2)] = content[i*size+(size/2)];
        }
        return mirrored;
    }

    //This compares shapes and doesn't take mirrored recipes into account.
    //public for testing purposes. Not very professional I know, but it gets the job done.
    public static boolean shapeIterationMatches(final Object[] itemsOne, final Object[] itemsTwo, final IMatcher<ItemStack> matcher, final int rowSize){
        //Find the first element of r and content.
        int indexTwo = -1, indexOne = -1;
        while(++indexTwo < itemsTwo.length && !isItem(itemsTwo[indexTwo]));
        while(++indexOne < itemsOne.length && !isItem(itemsOne[indexOne]));

        //Look if one or both recipes are empty. Return true if both are empty.
        if(indexTwo == itemsTwo.length || indexOne == itemsOne.length) return indexTwo == itemsTwo.length && indexOne == itemsOne.length;

        if(!matcher.match(itemsTwo[indexTwo],itemsOne[indexOne])){
            return false;
        }

        //Offsets relative to the first item of the recipe.
        int iIndex, twoRowOffset, jIndex, oneRowOffset;
        for(;;) {
            iIndex = twoRowOffset = 0;
            jIndex = oneRowOffset = 0;
            while (++indexTwo < itemsTwo.length) {
                iIndex++;
                if (indexTwo % rowSize == 0) twoRowOffset++;

                if(isItem(itemsTwo[indexTwo])) break;

            }

            while (++indexOne < itemsOne.length) {
                jIndex++;
                if (indexOne % rowSize == 0) oneRowOffset++;

                if(isItem(itemsOne[indexOne])) break;
            }

            if (indexTwo == itemsTwo.length || indexOne == itemsOne.length) {
                return indexTwo == itemsTwo.length && indexOne == itemsOne.length;
            }
            if (!matcher.match(itemsTwo[indexTwo], itemsOne[indexOne]))
                return false;

            //The offsets have to be the same, otherwise the shape isn't equal.
            if (iIndex != jIndex || twoRowOffset != oneRowOffset) return false;
        }
    }

    public static boolean shapeMatches(final Object[] content, final Object[] stacks, final IMatcher<ItemStack> matcher){
        final int rowSize = content == null ? 0 : (int)Math.sqrt(content.length);

        return shapeIterationMatches(content, stacks, matcher, rowSize) || shapeIterationMatches(mirror(content, rowSize), stacks, matcher, rowSize);
    }

    private static ItemStack[] ensureNoGaps(final ItemStack[] items){
        return Arrays.asList(items).stream().filter(x -> x != null && x.getType() != Material.AIR).toArray(ItemStack[]::new);
    }

    private static EnhancedItem[] ensureNoGaps(final EnhancedItem[] items) {
        return Arrays.stream(items).filter(x -> x != null && x.getItem() != null && x.getItem().getType() != Material.AIR).toArray(EnhancedItem[]::new);
    }

    private static Object[] ensureNoGaps(final Object[] items) {
        if (items instanceof EnhancedItem[]) {
            return ensureNoGaps((EnhancedItem[]) items);
        }
        return ensureNoGaps((ItemStack[]) items);
    }

    public static boolean ingredientsMatch(Object[] a, Object[] b, final IMatcher<ItemStack> matcher) {
        //array with all values to false.
        a = ensureNoGaps(a);
        b = ensureNoGaps(b);

        if(a.length == 0 || b.length == 0) return false;
        if(a.length != b.length) return false;

        //use no primitive type to allow Boolean stream of objects instead of arrays.
        final Boolean[] used = new Boolean[a.length];
        Arrays.fill(used, false);

        for(final Object inRecipe : a){
            if(inRecipe == null) continue;
            //Look if inRecipe matches with an ingredient.
            for(int i = 0; i < used.length; i++) {
                if (used[i]) continue;
                if(b[i] == null){
                    Bukkit.getLogger().log(Level.SEVERE, "Error, found null ingredient.");
                    return false;
                }
                if(matcher.match(b[i], inRecipe)){
                    used[i] = true;
                    break;
                }
            }
        }
        return !Arrays.stream(used).anyMatch(x -> x == false);
    }

}

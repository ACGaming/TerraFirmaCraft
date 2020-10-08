/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.api.capability.food;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;

public interface IFoodStatsTFC
{
    float MAX_PLAYER_THIRST = 100f;

    /**
     * A copy of {@link FoodStatsTFC#addStats(ItemFood, ItemStack)} but without an ItemFood argument.
     * This is for when an item isn't an ItemFood or an extension of it.
     *
     * @param stack an ItemStack that must have an IFood capability attached
     */
    void addStats(ItemStack stack);

    float getHealthModifier();

    float getThirst();

    void setThirst(float value);

    @Nonnull
    NutritionStats getNutrition();

    /**
     * Used to drink from a water source. Has an internal cooldown
     * Attention: Simulation updates the cooldown, if you need to update the value after a simulation, use #addThirst
     *
     * @param value    the amount to drink = the value to increase thirst by
     * @param simulate determines if this is a simulated drink (eg: the thirst value is not updated, but still checks if this attempt would happen)
     * @return true if the player was able to drink (cooldown + not already full)
     */
    boolean attemptDrink(float value, boolean simulate);

    /**
     * Used to directly add thirst, i.e. from an external source like a water bottle
     */
    default void addThirst(float value)
    {
        setThirst(getThirst() + value);
    }

    /**
     * Resets cooldown to prevent arm swinging in client when it attempts to drink water
     * Client also needs to update cooldown after a sucessful drink attempt
     */
    void resetCooldown();
}

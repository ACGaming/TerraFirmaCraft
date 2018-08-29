/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.recipes;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.IForgeRegistryEntry;

import net.dries007.tfc.api.capability.IMoldHandler;
import net.dries007.tfc.api.capability.heat.IItemHeat;
import net.dries007.tfc.objects.items.ceramics.ItemMold;
import net.dries007.tfc.objects.items.metal.ItemMetal;

import static net.dries007.tfc.api.capability.heat.CapabilityItemHeat.ITEM_HEAT_CAPABILITY;
import static net.dries007.tfc.api.util.TFCConstants.MOD_ID;
import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;

public class UnmoldRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe
{
    private final ItemMold mold;

    public UnmoldRecipe(ItemMold mold)
    {
        this.mold = mold;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn)
    {
        boolean mold = false;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++)
        {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ItemMold)
            {
                ItemMold metal = ((ItemMold) stack.getItem());

                IFluidHandler cap = stack.getCapability(FLUID_HANDLER_CAPABILITY, null);
                if (!(cap instanceof IMoldHandler)) return false;
                if (((IMoldHandler) cap).isMolten()) return false;

                if (metal != this.mold) return false;
                if (mold) return false;
                mold = true;
            }
            else
            {
                return false;
            }
        }
        return mold;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv)
    {
        ItemStack moldStack = null;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++)
        {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ItemMold)
            {
                ItemMold tmp = ((ItemMold) stack.getItem());
                if (tmp != this.mold) return null;
                if (moldStack != null) return null;
                moldStack = stack;
            }
            else
            {
                return null;
            }
        }
        if (moldStack == null) return null;

        IFluidHandler moldCap = moldStack.getCapability(FLUID_HANDLER_CAPABILITY, null);
        if (!(moldCap instanceof IMoldHandler)) return null;
        IMoldHandler moldHandler = ((IMoldHandler) moldCap);
        if (moldHandler.isMolten()) return null;

        ItemStack output = new ItemStack(ItemMetal.get(moldHandler.getMetal(), ((ItemMold) moldStack.getItem()).type));
        IItemHeat heat = output.getCapability(ITEM_HEAT_CAPABILITY, null);
        if (heat != null)
        {
            heat.setTemperature(moldHandler.getTemperature());
        }
        return output;
    }

    @Override
    public boolean canFit(int width, int height)
    {
        return true; // width * height > 1;
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    @Override
    public String getGroup()
    {
        return MOD_ID + ":unmold_" + mold.type.name().toLowerCase();
    }
}

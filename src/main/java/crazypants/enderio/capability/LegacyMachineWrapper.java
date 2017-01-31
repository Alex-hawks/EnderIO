package crazypants.enderio.capability;

import com.enderio.core.common.util.ItemUtil;

import crazypants.enderio.machine.AbstractInventoryMachineEntity;
import crazypants.enderio.machine.IoMode;
import crazypants.util.Prep;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;

public class LegacyMachineWrapper implements IItemHandler {
  protected final AbstractInventoryMachineEntity machine;
  protected final EnumFacing side;

  public LegacyMachineWrapper(AbstractInventoryMachineEntity machine, EnumFacing side) {
    this.machine = machine;
    this.side = side;
  }

  @Override
  public int getSlots() {
    int result = 0;
    final IoMode ioMode = machine.getIoMode(side);
    if (ioMode.canRecieveInput()) {
      result += machine.getSlotDefinition().getNumInputSlots();
    }
    if (ioMode.canOutput()) {
      result += machine.getSlotDefinition().getNumOutputSlots();
    }
    return result;
  }

  private int smin(int a, int b) {
    return a > -1 && a < b ? a : b;
  }

  private int extSlot2intSlot(int external) {
    int min = -1, max = -1;
    final IoMode ioMode = machine.getIoMode(side);
    if (ioMode.canRecieveInput()) {
      min = machine.getSlotDefinition().getMinInputSlot();
      max = machine.getSlotDefinition().getMaxInputSlot();
    }
    if (ioMode.canOutput()) {
      min = smin(min, machine.getSlotDefinition().getMinOutputSlot());
      max = Math.max(max, machine.getSlotDefinition().getMaxOutputSlot());
    }
    if (min < 0) {
      return -1;
    }
    int internal = external - min;
    if (internal > max) {
      return -1;
    }
    return internal;
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    return machine.getStackInSlot(extSlot2intSlot(slot));
  }

  @Override
  public ItemStack insertItem(int external, ItemStack stack, boolean simulate) {
    if (Prep.isInvalid(stack) || !machine.getIoMode(side).canRecieveInput()) {
      return stack;
    }

    int slot = extSlot2intSlot(external);
    if (!machine.getSlotDefinition().isInputSlot(slot)) {
      return stack;
    }

    ItemStack existing = machine.getStackInSlot(slot);
    if (Prep.isValid(existing)) {
      int max = Math.min(existing.getMaxStackSize(), machine.getInventoryStackLimit(slot));
      if (existing.stackSize >= max || !ItemUtil.areStackMergable(existing, stack)) {
        return stack;
      }
      int movable = Math.min(max - existing.stackSize, stack.stackSize);
      if (!simulate) {
        existing.stackSize += movable;
        machine.markDirty();
      }
      if (movable >= stack.stackSize) {
        return Prep.getEmpty();
      } else {
        ItemStack copy = stack.copy();
        copy.stackSize -= movable;
        return copy;
      }
    } else {
      if (!machine.isMachineItemValidForSlot(slot, stack)) {
        return stack;
      }
      int max = Math.min(stack.getMaxStackSize(), machine.getInventoryStackLimit(slot));
      if (max >= stack.stackSize) {
        if (!simulate) {
          machine.setInventorySlotContents(slot, stack.copy());
        }
        return Prep.getEmpty();
      } else {
        ItemStack copy = stack.copy();
        copy.stackSize = max;
        if (!simulate) {
          machine.setInventorySlotContents(slot, copy);
        }
        copy = stack.copy();
        copy.stackSize -= max;
        return copy;
      }
    }
  }

  @Override
  public ItemStack extractItem(int external, int amount, boolean simulate) {
    if (amount <= 0 || !machine.getIoMode(side).canOutput())
      return null;

    int slot = extSlot2intSlot(external);
    if (!machine.getSlotDefinition().isInputSlot(slot)) {
      return Prep.getEmpty();
    }

    ItemStack existing = machine.getStackInSlot(slot);

    if (Prep.isInvalid(existing)) {
      return Prep.getEmpty();
    }

    int max = Math.min(amount, existing.stackSize);

    ItemStack copy = existing.copy();
    copy.stackSize = max;

    if (!simulate) {
      existing.stackSize -= max;
      if (existing.stackSize <= 0) {
        machine.setInventorySlotContents(slot, Prep.getEmpty());
      } else {
        machine.markDirty();
      }
    }
    return copy;
  }

}
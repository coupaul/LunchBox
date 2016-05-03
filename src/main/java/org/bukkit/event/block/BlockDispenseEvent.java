package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BlockDispenseEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private ItemStack item;
    private Vector velocity;

    public BlockDispenseEvent(Block block, ItemStack dispensed, Vector velocity) {
        super(block);
        this.item = dispensed;
        this.velocity = velocity;
    }

    public ItemStack getItem() {
        return this.item.clone();
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public Vector getVelocity() {
        return this.velocity.clone();
    }

    public void setVelocity(Vector vel) {
        this.velocity = vel;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return BlockDispenseEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return BlockDispenseEvent.handlers;
    }
}

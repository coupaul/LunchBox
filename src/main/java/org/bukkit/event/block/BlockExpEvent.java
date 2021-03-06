package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;

public class BlockExpEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();
    private int exp;

    public BlockExpEvent(Block block, int exp) {
        super(block);
        this.exp = exp;
    }

    public int getExpToDrop() {
        return this.exp;
    }

    public void setExpToDrop(int exp) {
        this.exp = exp;
    }

    public HandlerList getHandlers() {
        return BlockExpEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return BlockExpEvent.handlers;
    }
}

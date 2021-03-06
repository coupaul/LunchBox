package org.bukkit.event.entity;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class EntityCombustByBlockEvent extends EntityCombustEvent {

    private final Block combuster;

    public EntityCombustByBlockEvent(Block combuster, Entity combustee, int duration) {
        super(combustee, duration);
        this.combuster = combuster;
    }

    public Block getCombuster() {
        return this.combuster;
    }
}

package org.bukkit;

public enum Statistic {

    DAMAGE_DEALT, DAMAGE_TAKEN, DEATHS, MOB_KILLS, PLAYER_KILLS, FISH_CAUGHT, ANIMALS_BRED, TREASURE_FISHED, JUNK_FISHED, LEAVE_GAME, JUMP, DROP, PLAY_ONE_TICK, WALK_ONE_CM, SWIM_ONE_CM, FALL_ONE_CM, CLIMB_ONE_CM, FLY_ONE_CM, DIVE_ONE_CM, MINECART_ONE_CM, BOAT_ONE_CM, PIG_ONE_CM, HORSE_ONE_CM, SPRINT_ONE_CM, CROUCH_ONE_CM, MINE_BLOCK(Statistic.Type.BLOCK), USE_ITEM(Statistic.Type.ITEM), BREAK_ITEM(Statistic.Type.ITEM), CRAFT_ITEM(Statistic.Type.ITEM), KILL_ENTITY(Statistic.Type.ENTITY), ENTITY_KILLED_BY(Statistic.Type.ENTITY), TIME_SINCE_DEATH, TALKED_TO_VILLAGER, TRADED_WITH_VILLAGER, CAKE_SLICES_EATEN, CAULDRON_FILLED, CAULDRON_USED, ARMOR_CLEANED, BANNER_CLEANED, BREWINGSTAND_INTERACTION, BEACON_INTERACTION, DROPPER_INSPECTED, HOPPER_INSPECTED, DISPENSER_INSPECTED, NOTEBLOCK_PLAYED, NOTEBLOCK_TUNED, FLOWER_POTTED, TRAPPED_CHEST_TRIGGERED, ENDERCHEST_OPENED, ITEM_ENCHANTED, RECORD_PLAYED, FURNACE_INTERACTION, CRAFTING_TABLE_INTERACTION, CHEST_OPENED;

    private final Statistic.Type type;

    private Statistic() {
        this(Statistic.Type.UNTYPED);
    }

    private Statistic(Statistic.Type type) {
        this.type = type;
    }

    public Statistic.Type getType() {
        return this.type;
    }

    public boolean isSubstatistic() {
        return this.type != Statistic.Type.UNTYPED;
    }

    public boolean isBlock() {
        return this.type == Statistic.Type.BLOCK;
    }

    public static enum Type {

        UNTYPED, ITEM, BLOCK, ENTITY;
    }
}

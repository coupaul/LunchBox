package org.bukkit.material;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class Ladder extends SimpleAttachableMaterialData {

    private static int[] $SWITCH_TABLE$org$bukkit$block$BlockFace;

    public Ladder() {
        super(Material.LADDER);
    }

    /** @deprecated */
    @Deprecated
    public Ladder(int type) {
        super(type);
    }

    public Ladder(Material type) {
        super(type);
    }

    /** @deprecated */
    @Deprecated
    public Ladder(int type, byte data) {
        super(type, data);
    }

    /** @deprecated */
    @Deprecated
    public Ladder(Material type, byte data) {
        super(type, data);
    }

    public BlockFace getAttachedFace() {
        byte data = this.getData();

        switch (data) {
        case 2:
            return BlockFace.SOUTH;

        case 3:
            return BlockFace.NORTH;

        case 4:
            return BlockFace.EAST;

        case 5:
            return BlockFace.WEST;

        default:
            return null;
        }
    }

    public void setFacingDirection(BlockFace face) {
        byte data = 0;

        switch ($SWITCH_TABLE$org$bukkit$block$BlockFace()[face.ordinal()]) {
        case 1:
            data = 3;
            break;

        case 2:
            data = 4;
            break;

        case 3:
            data = 2;
            break;

        case 4:
            data = 5;
        }

        this.setData(data);
    }

    public Ladder clone() {
        return (Ladder) super.clone();
    }

    static int[] $SWITCH_TABLE$org$bukkit$block$BlockFace() {
        int[] aint = Ladder.$SWITCH_TABLE$org$bukkit$block$BlockFace;

        if (Ladder.$SWITCH_TABLE$org$bukkit$block$BlockFace != null) {
            return aint;
        } else {
            int[] aint1 = new int[BlockFace.values().length];

            try {
                aint1[BlockFace.DOWN.ordinal()] = 6;
            } catch (NoSuchFieldError nosuchfielderror) {
                ;
            }

            try {
                aint1[BlockFace.EAST.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror1) {
                ;
            }

            try {
                aint1[BlockFace.EAST_NORTH_EAST.ordinal()] = 14;
            } catch (NoSuchFieldError nosuchfielderror2) {
                ;
            }

            try {
                aint1[BlockFace.EAST_SOUTH_EAST.ordinal()] = 15;
            } catch (NoSuchFieldError nosuchfielderror3) {
                ;
            }

            try {
                aint1[BlockFace.NORTH.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror4) {
                ;
            }

            try {
                aint1[BlockFace.NORTH_EAST.ordinal()] = 7;
            } catch (NoSuchFieldError nosuchfielderror5) {
                ;
            }

            try {
                aint1[BlockFace.NORTH_NORTH_EAST.ordinal()] = 13;
            } catch (NoSuchFieldError nosuchfielderror6) {
                ;
            }

            try {
                aint1[BlockFace.NORTH_NORTH_WEST.ordinal()] = 12;
            } catch (NoSuchFieldError nosuchfielderror7) {
                ;
            }

            try {
                aint1[BlockFace.NORTH_WEST.ordinal()] = 8;
            } catch (NoSuchFieldError nosuchfielderror8) {
                ;
            }

            try {
                aint1[BlockFace.SELF.ordinal()] = 19;
            } catch (NoSuchFieldError nosuchfielderror9) {
                ;
            }

            try {
                aint1[BlockFace.SOUTH.ordinal()] = 3;
            } catch (NoSuchFieldError nosuchfielderror10) {
                ;
            }

            try {
                aint1[BlockFace.SOUTH_EAST.ordinal()] = 9;
            } catch (NoSuchFieldError nosuchfielderror11) {
                ;
            }

            try {
                aint1[BlockFace.SOUTH_SOUTH_EAST.ordinal()] = 16;
            } catch (NoSuchFieldError nosuchfielderror12) {
                ;
            }

            try {
                aint1[BlockFace.SOUTH_SOUTH_WEST.ordinal()] = 17;
            } catch (NoSuchFieldError nosuchfielderror13) {
                ;
            }

            try {
                aint1[BlockFace.SOUTH_WEST.ordinal()] = 10;
            } catch (NoSuchFieldError nosuchfielderror14) {
                ;
            }

            try {
                aint1[BlockFace.UP.ordinal()] = 5;
            } catch (NoSuchFieldError nosuchfielderror15) {
                ;
            }

            try {
                aint1[BlockFace.WEST.ordinal()] = 4;
            } catch (NoSuchFieldError nosuchfielderror16) {
                ;
            }

            try {
                aint1[BlockFace.WEST_NORTH_WEST.ordinal()] = 11;
            } catch (NoSuchFieldError nosuchfielderror17) {
                ;
            }

            try {
                aint1[BlockFace.WEST_SOUTH_WEST.ordinal()] = 18;
            } catch (NoSuchFieldError nosuchfielderror18) {
                ;
            }

            Ladder.$SWITCH_TABLE$org$bukkit$block$BlockFace = aint1;
            return aint1;
        }
    }
}

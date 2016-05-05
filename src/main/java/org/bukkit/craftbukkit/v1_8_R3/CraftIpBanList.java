package org.bukkit.craftbukkit.v1_8_R3;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import net.minecraft.server.v1_8_R3.IpBanEntry;
import net.minecraft.server.v1_8_R3.IpBanList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

public class CraftIpBanList implements BanList {

    private final IpBanList list;

    public CraftIpBanList(IpBanList list) {
        this.list = list;
    }

    public BanEntry getBanEntry(String target) {
        Validate.notNull(target, "Target cannot be null");
        IpBanEntry entry = (IpBanEntry) this.list.get((Object) target);

        return entry == null ? null : new CraftIpBanEntry(target, entry, this.list);
    }

    public BanEntry addBan(String target, String reason, Date expires, String source) {
        Validate.notNull(target, "Ban target cannot be null");
        IpBanEntry entry = new IpBanEntry(target, new Date(), StringUtils.isBlank(source) ? null : source, expires, StringUtils.isBlank(reason) ? null : reason);

        this.list.add(entry);

        try {
            this.list.save();
        } catch (IOException ioexception) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save banned-ips.json, {0}", ioexception.getMessage());
        }

        return new CraftIpBanEntry(target, entry, this.list);
    }

    public Set getBanEntries() {
        ImmutableSet.Builder builder = ImmutableSet.builder();
        String[] astring;
        int i = (astring = this.list.getEntries()).length;

        for (int j = 0; j < i; ++j) {
            String target = astring[j];

            builder.add((Object) (new CraftIpBanEntry(target, (IpBanEntry) this.list.get((Object) target), this.list)));
        }

        return builder.build();
    }

    public boolean isBanned(String target) {
        Validate.notNull(target, "Target cannot be null");
        return this.list.isBanned(InetSocketAddress.createUnresolved(target, 0));
    }

    public void pardon(String target) {
        Validate.notNull(target, "Target cannot be null");
        this.list.remove(target);
    }
}
package org.bukkit.craftbukkit.v1_8_R3.scoreboard;

import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
final class CraftCriteria {

    static final Map DEFAULTS;
    static final CraftCriteria DUMMY;
    final IScoreObjectiveCriteria criteria;
    final String bukkitName;

    static {
        ImmutableMap.Builder defaults = ImmutableMap.builder();
        Iterator iterator = IScoreObjectiveCriteria.INSTANCES.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            String name = entry.getKey().toString();
            IScoreObjectiveCriteria criteria = (IScoreObjectiveCriteria) entry.getValue();

            defaults.put(name, new CraftCriteria(criteria));
        }

        DEFAULTS = defaults.build();
        DUMMY = (CraftCriteria) CraftCriteria.DEFAULTS.get("dummy");
    }

    private CraftCriteria(String bukkitName) {
        this.bukkitName = bukkitName;
        this.criteria = CraftCriteria.DUMMY.criteria;
    }

    private CraftCriteria(IScoreObjectiveCriteria criteria) {
        this.criteria = criteria;
        this.bukkitName = criteria.getName();
    }

    static CraftCriteria getFromNMS(ScoreObjective objective) {
        return (CraftCriteria) CraftCriteria.DEFAULTS.get(objective.getCriteria().getName());
    }

    static CraftCriteria getFromBukkit(String name) {
        CraftCriteria criteria = (CraftCriteria) CraftCriteria.DEFAULTS.get(name);

        return criteria != null ? criteria : new CraftCriteria(name);
    }

    public boolean equals(Object that) {
        return !(that instanceof CraftCriteria) ? false : ((CraftCriteria) that).bukkitName.equals(this.bukkitName);
    }

    public int hashCode() {
        return this.bukkitName.hashCode() ^ CraftCriteria.class.hashCode();
    }
}

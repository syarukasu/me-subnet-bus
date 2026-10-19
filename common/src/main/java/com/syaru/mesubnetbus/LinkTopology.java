package com.syaru.mesubnetbus;

import java.util.*;
import appeng.api.networking.IGrid;
import appeng.api.storage.IStorageProvider;
import com.syaru.mesubnetbus.core.ExposureGraph;

public final class LinkTopology {
    private static final Set<BaseSubnetPart> PARTS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean dirty;
    private LinkTopology() {}
    static void add(BaseSubnetPart part) { PARTS.add(part); changed(part); }
    static void remove(BaseSubnetPart part) { changed(part); PARTS.remove(part); }
    static void changed(BaseSubnetPart changed) {
        changed.disconnectStorage();
        dirty = true;
        // Only the connected exposure component is invalidated. Unrelated networks keep working.
        Set<IGrid> affected=Collections.newSetFromMap(new IdentityHashMap<>());
        collect(affected,changed);
        boolean expanded;
        do {
            expanded=false;
            for(var part:PARTS) {
                if(affected.contains(part.lastSource) || affected.contains(part.lastTarget)
                        || (part.inner!=null && affected.contains(part.inner.getGrid()))
                        || (part.outer!=null && affected.contains(part.outer.getGrid()))) {
                    part.disconnectStorage();
                    int before=affected.size(); collect(affected,part);
                    expanded|=before!=affected.size();
                }
            }
        } while(expanded);
    }
    private static void collect(Set<IGrid> grids,BaseSubnetPart part) {
        if(part.inner!=null && part.inner.getGrid()!=null) grids.add(part.inner.getGrid());
        if(part.outer!=null && part.outer.getGrid()!=null) grids.add(part.outer.getGrid());
        if(part.lastSource!=null) grids.add(part.lastSource);
        if(part.lastTarget!=null) grids.add(part.lastTarget);
    }
    public static void clear() {
        PARTS.forEach(BaseSubnetPart::disconnectStorage);
        PARTS.clear(); dirty = false;
    }
    public static void tick() {
        if (!dirty) return;
        dirty = false;
        var sorted = new ArrayList<>(PARTS);
        sorted.sort(Comparator.comparing(BaseSubnetPart::locationKey));
        var graph = new ExposureGraph<IGrid>();
        for (var part : sorted) {
            String status = part.connectionStatus();
            if (status.equals("ready")) {
                status = switch (graph.add(part.source().getGrid(), part.target().getGrid())) {
                    case ACCEPTED -> "ready";
                    case SAME_NETWORK -> "same_grid";
                    case CYCLE -> "cycle";
                    case DUPLICATE_PATH -> "duplicate_path";
                };
            }
            part.valid = status.equals("ready");
            part.status = status;
            part.lastSource=part.source().getGrid(); part.lastTarget=part.target().getGrid();
        }
        // Decide every edge before mounting any storage; callbacks cannot see a half-built graph.
        for (var part : sorted) {
            part.refreshStorage();
            IStorageProvider.requestUpdate(part.inner);
            IStorageProvider.requestUpdate(part.outer);
            part.host.markForUpdate();
        }
    }
}

package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.Path;

import com.google.common.util.concurrent.JdkFutureAdapters;

public final class DataModificationTracker<P extends Path<P>,D> {

    ConcurrentMap<Object, DataModification<P,D>> trackedTransactions = new ConcurrentHashMap<>();
    
    
    public void startTrackingModification(DataModification<P,D> modification) {
        trackedTransactions.putIfAbsent(modification.getIdentifier(), modification);
        
        
    }
    
    public boolean containsIdentifier(Object identifier) {
        return trackedTransactions.containsKey(identifier);
    }
}

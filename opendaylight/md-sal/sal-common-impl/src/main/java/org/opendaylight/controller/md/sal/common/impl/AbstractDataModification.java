package org.opendaylight.controller.md.sal.common.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.Path;

import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.NEW;

public abstract class AbstractDataModification<P extends Path<P>, D> implements DataModification<P, D>  {

    private final Map<P,D> configurationUpdate;
    private final Map<P,D> operationalUpdate;

    private final Set<P> configurationRemove;
    private final Set<P> operationalRemove;
   
    private final Map<P, D> unmodifiable_configurationUpdate;
    private final Map<P, D> unmodifiable_operationalUpdate;
    private final Set<P> unmodifiable_configurationRemove;
    private final Set<P> unmodifiable_OperationalRemove;

    

    public AbstractDataModification(Map<P, D> configurationUpdate, Map<P, D> operationalUpdate,
            Set<P> configurationRemove, Set<P> operationalRemove) {
        this.configurationUpdate = configurationUpdate;
        this.operationalUpdate = operationalUpdate;
        this.configurationRemove = configurationRemove;
        this.operationalRemove = operationalRemove;
        
        unmodifiable_configurationUpdate = Collections.unmodifiableMap(configurationUpdate);
        unmodifiable_operationalUpdate = Collections.unmodifiableMap(operationalUpdate);
        unmodifiable_configurationRemove = Collections.unmodifiableSet(configurationRemove);
        unmodifiable_OperationalRemove = Collections.unmodifiableSet(operationalRemove);
    }
    
    public AbstractDataModification() {
        this(new HashMap<P,D>(), new HashMap<P,D>(), new HashSet<P>(), new HashSet<P>());
    }

    @Override
    public final void putConfigurationData(P path, D data) {
        checkMutable();
        configurationUpdate.put(path, data);
        configurationRemove.remove(path);
    }
    
    @Override
    public final void putRuntimeData(P path, D data) {
        checkMutable();
        operationalUpdate.put(path, data);
        operationalRemove.remove(path);
    }
    
    @Override
    public final void removeRuntimeData(P path) {
        checkMutable();
        operationalUpdate.remove(path);
        operationalRemove.add(path);
    }
    
    @Override
    public final void removeConfigurationData(P path) {
        checkMutable();
        configurationUpdate.remove(path);
        configurationRemove.add(path);
    }

    private final void checkMutable() {
        if(!NEW.equals(this.getStatus())) throw new IllegalStateException("Transaction was already submitted");
    }

    @Override
    public Map<P, D> getUpdatedConfigurationData() {
        
        return unmodifiable_configurationUpdate;
    }

    @Override
    public Map<P, D> getUpdatedOperationalData() {
        return unmodifiable_operationalUpdate;
    }

    @Override
    public Set<P> getRemovedConfigurationData() {
        return unmodifiable_configurationRemove;
    }
    
    @Override
    public Set<P> getRemovedOperationalData() {
        return unmodifiable_OperationalRemove;
    }

}

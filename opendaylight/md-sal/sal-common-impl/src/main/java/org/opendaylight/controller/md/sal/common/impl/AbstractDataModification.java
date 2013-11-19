package org.opendaylight.controller.md.sal.common.impl;

import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.NEW;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;

public abstract class AbstractDataModification<P /* extends Path<P> */, D> implements DataModification<P, D> {

    private final ConcurrentMap<P, D> operationalOriginal;
    private final ConcurrentMap<P, D> configurationOriginal;

    private final ConcurrentMap<P, D> operationalCreated;
    private final ConcurrentMap<P, D> configurationCreated;

    private final ConcurrentMap<P, D> configurationUpdate;
    private final ConcurrentMap<P, D> operationalUpdate;

    private final ConcurrentMap<P, P> configurationRemove;
    private final ConcurrentMap<P, P> operationalRemove;

    private final Map<P, D> unmodifiable_configurationOriginal;
    private final Map<P, D> unmodifiable_operationalOriginal;
    private final Map<P, D> unmodifiable_configurationCreated;
    private final Map<P, D> unmodifiable_operationalCreated;
    private final Map<P, D> unmodifiable_configurationUpdate;
    private final Map<P, D> unmodifiable_operationalUpdate;
    private final Set<P> unmodifiable_configurationRemove;
    private final Set<P> unmodifiable_OperationalRemove;
    private DataReader<P, D> reader;

    public AbstractDataModification(DataReader<P, D> reader) {
        this.reader = reader;
        this.configurationUpdate = new ConcurrentHashMap<>();
        this.operationalUpdate = new ConcurrentHashMap<>();
        this.configurationRemove = new ConcurrentHashMap<>();
        this.operationalRemove = new ConcurrentHashMap<>();

        this.configurationOriginal = new ConcurrentHashMap<>();
        this.operationalOriginal = new ConcurrentHashMap<>();

        this.configurationCreated = new ConcurrentHashMap<>();
        this.operationalCreated = new ConcurrentHashMap<>();

        unmodifiable_configurationOriginal = Collections.unmodifiableMap(configurationOriginal);
        unmodifiable_operationalOriginal = Collections.unmodifiableMap(operationalOriginal);
        unmodifiable_configurationCreated = Collections.unmodifiableMap(configurationCreated);
        unmodifiable_operationalCreated = Collections.unmodifiableMap(operationalCreated);
        unmodifiable_configurationUpdate = Collections.unmodifiableMap(configurationUpdate);
        unmodifiable_operationalUpdate = Collections.unmodifiableMap(operationalUpdate);
        unmodifiable_configurationRemove = Collections.unmodifiableSet(configurationRemove.keySet());
        unmodifiable_OperationalRemove = Collections.unmodifiableSet(operationalRemove.keySet());

    }

    @Override
    public final void putConfigurationData(P path, D data) {
        checkMutable();

        if (!hasConfigurationOriginal(path)) {
            configurationCreated.put(path, data);
        }

        configurationUpdate.put(path, data);
        configurationRemove.remove(path);
    }

    @Override
    public final void putOperationalData(P path, D data) {
        checkMutable();
        if (!hasOperationalOriginal(path)) {
            operationalCreated.put(path, data);
        }
        operationalUpdate.put(path, data);
        operationalRemove.remove(path);
    }

    @Override
    public final void putRuntimeData(P path, D data) {
        putOperationalData(path, data);
    }

    @Override
    public final void removeOperationalData(P path) {
        checkMutable();
        hasOperationalOriginal(path);
        operationalUpdate.remove(path);
        operationalRemove.put(path, path);
    }

    @Override
    public final void removeRuntimeData(P path) {
        removeOperationalData(path);
    }

    @Override
    public final void removeConfigurationData(P path) {
        checkMutable();
        hasConfigurationOriginal(path);
        configurationUpdate.remove(path);
        configurationRemove.put(path, path);
    }

    private final void checkMutable() {
        if (!NEW.equals(this.getStatus()))
            throw new IllegalStateException("Transaction was already submitted");
    }

    @Override
    public final Map<P, D> getUpdatedConfigurationData() {

        return unmodifiable_configurationUpdate;
    }

    @Override
    public final Map<P, D> getUpdatedOperationalData() {
        return unmodifiable_operationalUpdate;
    }

    @Override
    public final Set<P> getRemovedConfigurationData() {
        return unmodifiable_configurationRemove;
    }

    @Override
    public final Set<P> getRemovedOperationalData() {
        return unmodifiable_OperationalRemove;
    }

    @Override
    public Map<P, D> getCreatedConfigurationData() {
        return unmodifiable_configurationCreated;
    }

    @Override
    public Map<P, D> getCreatedOperationalData() {
        return unmodifiable_operationalCreated;
    }

    @Override
    public Map<P, D> getOriginalConfigurationData() {
        return unmodifiable_configurationOriginal;
    }

    @Override
    public Map<P, D> getOriginalOperationalData() {
        return unmodifiable_operationalOriginal;
    }

    @Override
    public D readOperationalData(P path) {
        return reader.readOperationalData(path);
    }

    @Override
    public D readConfigurationData(P path) {
        return reader.readConfigurationData(path);
    }

    private boolean hasConfigurationOriginal(P path) {
        if (configurationOriginal.containsKey(path)) {
            return true;
        }
        D data = reader.readConfigurationData(path);
        if (data != null) {
            configurationOriginal.putIfAbsent(path, data);
            return true;
        }
        return false;
    }

    private boolean hasOperationalOriginal(P path) {
        if (operationalOriginal.containsKey(path)) {
            return true;
        }
        D data = reader.readOperationalData(path);
        if (data != null) {
            operationalOriginal.putIfAbsent(path, data);
            return true;
        }
        return false;
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl;

import static org.opendaylight.controller.md.sal.common.api.TransactionStatus.NEW;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.concepts.Path;

public abstract class AbstractDataModification<P extends Path<P>, D> implements DataModification<P, D> {

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
    private final DataReader<P, D> reader;

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
        D original = null;
        if ((original = getConfigurationOriginal(path)) == null) {
            configurationCreated.put(path, data);
        }

        configurationUpdate.put(path, mergeConfigurationData(path,original, data));
    }

    @Override
    public final void putOperationalData(P path, D data) {
        checkMutable();
        D original = null;
        if ((original = getOperationalOriginal(path)) == null) {
            operationalCreated.put(path, data);
        }
        operationalUpdate.put(path, mergeOperationalData(path,original,data));
    }

    @Override
    public final void removeOperationalData(P path) {
        checkMutable();
        getOperationalOriginal(path);
        operationalUpdate.remove(path);
        operationalRemove.put(path, path);
    }

    @Override
    public final void removeConfigurationData(P path) {
        checkMutable();
        getConfigurationOriginal(path);
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

    private D getConfigurationOriginal(P path) {
        D data = configurationOriginal.get(path);
        if (data != null) {
            return data;
        }
        data = reader.readConfigurationData(path);
        if (data != null) {
            configurationOriginal.putIfAbsent(path, data);
            return data;
        }
        return null;
    }

    private D getOperationalOriginal(P path) {
        D data = operationalOriginal.get(path);
        if (data != null) {
            return data;
        }
        data = reader.readOperationalData(path);
        if (data != null) {
            operationalOriginal.putIfAbsent(path, data);
            return data;
        }
        return null;
    }

    protected D mergeOperationalData(P path,D stored, D modified) {
        return modified;
    }

    protected D mergeConfigurationData(P path,D stored, D modified) {
        return modified;
    }
}

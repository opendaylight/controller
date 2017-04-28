/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.schema.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.YangTextSourceProvider;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.repo.api.MissingSchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

@Deprecated
public final class GlobalBundleScanningSchemaServiceImpl implements SchemaContextProvider, SchemaService,
        YangTextSourceProvider, AutoCloseable {

    @GuardedBy("lock")
    private final Set<ListenerRegistration<?>> listeners = new HashSet<>();
    private final Object lock = new Object();
    private final DOMSchemaService schemaService;
    private final DOMYangTextSourceProvider yangProvider;

    private GlobalBundleScanningSchemaServiceImpl(final DOMSchemaService schemaService) {
        this.schemaService = Preconditions.checkNotNull(schemaService);
        this.yangProvider = (DOMYangTextSourceProvider) schemaService.getSupportedExtensions()
                .get(DOMYangTextSourceProvider.class);
    }

    public static GlobalBundleScanningSchemaServiceImpl createInstance(final DOMSchemaService schemaService) {
        return new GlobalBundleScanningSchemaServiceImpl(schemaService);
    }

    @Override
    public SchemaContext getSchemaContext() {
        return schemaService.getGlobalContext();
    }

    @Override
    public SchemaContext getGlobalContext() {
        return schemaService.getGlobalContext();
    }

    @Override
    public void addModule(final Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SchemaContext getSessionContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeModule(final Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
            final SchemaContextListener listener) {
        synchronized (lock) {
            final ListenerRegistration<SchemaContextListener> reg = schemaService.registerSchemaContextListener(
                listener);

            final ListenerRegistration<SchemaContextListener> ret =
                    new AbstractListenerRegistration<SchemaContextListener>(listener) {
                @Override
                protected void removeRegistration() {
                    synchronized (lock) {
                        listeners.remove(this);
                    }
                    reg.close();
                }
            };

            listeners.add(ret);
            return ret;
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            for (ListenerRegistration<?> l : listeners) {
                l.close();
            }
            listeners.clear();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public CheckedFuture<YangTextSchemaSource, SchemaSourceException> getSource(
            final SourceIdentifier sourceIdentifier) {
        if (yangProvider == null) {
            return Futures.immediateFailedCheckedFuture(new MissingSchemaSourceException(
                "Source provider is not available", sourceIdentifier));
        }

        return (CheckedFuture<YangTextSchemaSource, SchemaSourceException>) yangProvider.getSource(sourceIdentifier);
    }
}

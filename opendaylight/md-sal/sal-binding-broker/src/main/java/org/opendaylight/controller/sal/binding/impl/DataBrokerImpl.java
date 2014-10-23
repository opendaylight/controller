/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataBroker;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.DataObjectReadingUtil;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;

/**
 *
 * @deprecated This class implements legacy deprecated APIs, thus implementation is deprecated.
 */
@Deprecated
public class DataBrokerImpl extends
        AbstractDataBroker<InstanceIdentifier<? extends DataObject>, DataObject, DataChangeListener> //
        implements DataProviderService, AutoCloseable {

    private final static class ContainsWildcarded implements Predicate<InstanceIdentifier<? extends DataObject>> {

        private final InstanceIdentifier<? extends DataObject> key;

        public ContainsWildcarded(final InstanceIdentifier<? extends DataObject> key) {
            this.key = key;
        }

        @Override
        public boolean apply(final InstanceIdentifier<? extends DataObject> input) {
            return key.containsWildcarded(input);
        }
    }

    private final static class IsContainedWildcarded implements Predicate<InstanceIdentifier<? extends DataObject>> {

        private final InstanceIdentifier<? extends DataObject> key;

        public IsContainedWildcarded(final InstanceIdentifier<? extends DataObject> key) {
            this.key = key;
        }

        @Override
        public boolean apply(final InstanceIdentifier<? extends DataObject> input) {
            return input.containsWildcarded(key);
        }
    }

    private final AtomicLong nextTransaction = new AtomicLong();
    private final AtomicLong createdTransactionsCount = new AtomicLong();
    private final DelegatingDataReadRouter router = new DelegatingDataReadRouter();
    private DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> rootCommitHandler;

    public DataBrokerImpl() {
        setDataReadRouter(router);
    }

    public void setDataReadDelegate(final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> delegate) {
        router.setDelegate(delegate);
    }

    public AtomicLong getCreatedTransactionsCount() {
        return createdTransactionsCount;
    }

    @Override
    public DataTransactionImpl beginTransaction() {
        final String transactionId = "BA-" + nextTransaction.getAndIncrement();
        createdTransactionsCount.getAndIncrement();
        return new DataTransactionImpl(transactionId, this);
    }

    @Override
    public void close() {

    }

    @Override
    protected Predicate<InstanceIdentifier<? extends DataObject>> createContainsPredicate(
            final InstanceIdentifier<? extends DataObject> key) {
        return new ContainsWildcarded(key);
    }

    @Override
    protected Predicate<InstanceIdentifier<? extends DataObject>> createIsContainedPredicate(
            final InstanceIdentifier<? extends DataObject> key) {
        return new IsContainedWildcarded(key);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Map<InstanceIdentifier<? extends DataObject>, DataObject> deepGetBySubpath(
            final Map<InstanceIdentifier<? extends DataObject>, DataObject> dataSet,
            final InstanceIdentifier<? extends DataObject> path) {
        final Builder<InstanceIdentifier<? extends DataObject>, DataObject> builder = ImmutableMap.builder();
        final Map<InstanceIdentifier<? extends DataObject>, DataObject> potential = Maps.filterKeys(dataSet,
                createIsContainedPredicate(path));
        for (final Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : potential.entrySet()) {
            try {
                builder.putAll(DataObjectReadingUtil.readData(entry.getValue(), (InstanceIdentifier) entry.getKey(),
                        path));
            } catch (final Exception e) {
                // FIXME : Log exception;
            }
        }
        return builder.build();

    }

    public class DelegatingDataReadRouter extends
            AbstractDataReadRouter<InstanceIdentifier<? extends DataObject>, DataObject> {

        private DataReader<InstanceIdentifier<? extends DataObject>, DataObject> delegate;

        @Override
        public DataObject readConfigurationData(final InstanceIdentifier<? extends DataObject> path) {
            return delegate.readConfigurationData(path);
        }

        public void setDelegate(final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> delegate) {
            this.delegate = delegate;
        }

        @Override
        public DataObject readOperationalData(final InstanceIdentifier<? extends DataObject> path) {
            return delegate.readOperationalData(path);
        }

        @Override
        protected DataObject merge(final InstanceIdentifier<? extends DataObject> path, final Iterable<DataObject> data) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Registration registerConfigurationReader(
                final InstanceIdentifier<? extends DataObject> path,
                final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> reader) {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Registration registerOperationalReader(
                final InstanceIdentifier<? extends DataObject> path,
                final DataReader<InstanceIdentifier<? extends DataObject>, DataObject> reader) {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    @Override
    protected ImmutableList<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> affectedCommitHandlers(
            final Set<InstanceIdentifier<? extends DataObject>> paths) {
        final ImmutableList.Builder<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> handlersBuilder = ImmutableList.builder();
        return handlersBuilder //
                .add(rootCommitHandler) //
                .addAll(super.affectedCommitHandlers(paths)) //
                .build();
    }

    public void setRootCommitHandler(final DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> commitHandler) {
        rootCommitHandler = commitHandler;
    }

}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.BindingDataBroker;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.BindingDataWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * The DataBrokerImpl simply defers to the DOMDataBroker for all its operations.
 * All transactions and listener registrations are wrapped by the DataBrokerImpl
 * to allow binding aware components to use the DataBroker transparently.
 *
 * Besides this the DataBrokerImpl and it's collaborators also cache data that
 * is already transformed from the binding independent to binding aware format
 *
 * TODO : All references in this class to CompositeNode should be switched to
 * NormalizedNode once the MappingService is updated
 *
 */
public class ForwardedBindingDataBroker extends AbstractForwardedDataBroker implements BindingDataBroker {

    public ForwardedBindingDataBroker(final DOMDataBroker domDataBroker, final BindingIndependentMappingService mappingService, final SchemaService schemaService) {
        super(domDataBroker, mappingService,schemaService);
    }

    @Override
    public BindingDataReadOnlyTransaction newReadOnlyTransaction() {
        return new BindingDataReadTransactionImpl(getDelegate().newReadOnlyTransaction(),getCodec());
    }

    @Override
    public BindingDataReadWriteTransaction newReadWriteTransaction() {
        return new BindingDataReadWriteTransactionImpl(getDelegate().newReadWriteTransaction(),getCodec());
    }

    @Override
    public BindingDataWriteTransaction newWriteOnlyTransaction() {
        return new BindingDataWriteTransactionImpl<DOMDataWriteTransaction>(getDelegate().newWriteOnlyTransaction(),getCodec());
    }

    private abstract class AbstractBindingTransaction<T extends AsyncTransaction<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, NormalizedNode<?, ?>>>
            extends AbstractForwardedTransaction<T> implements AsyncTransaction<InstanceIdentifier<?>, DataObject> {

        protected AbstractBindingTransaction(final T delegate, final BindingToNormalizedNodeCodec codec) {
            super(delegate, codec);
        }

        @Override
        public Object getIdentifier() {
            return getDelegate().getIdentifier();
        }

    }

    private class BindingDataReadTransactionImpl extends AbstractBindingTransaction<DOMDataReadOnlyTransaction> implements
            BindingDataReadOnlyTransaction {

        protected BindingDataReadTransactionImpl(final DOMDataReadOnlyTransaction delegate,
                final BindingToNormalizedNodeCodec codec) {
            super(delegate, codec);
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(final LogicalDatastoreType store,
                final InstanceIdentifier<?> path) {
            return doRead(getDelegate(), store, path);
        }

        @Override
        public void close() {
            getDelegate().close();
        }
    }

    private class BindingDataWriteTransactionImpl<T extends DOMDataWriteTransaction> extends
            AbstractBindingTransaction<T> implements BindingDataWriteTransaction {

        protected BindingDataWriteTransactionImpl(final T delegate, final BindingToNormalizedNodeCodec codec) {
            super(delegate, codec);

        }

        @Override
        public boolean cancel() {
            return doCancel(getDelegate());
        }

        @Override
        public void put(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
            doPut(getDelegate(), store, path, data);
        }

        @Override
        public void merge(final LogicalDatastoreType store, final InstanceIdentifier<?> path, final DataObject data) {
            doMerge(getDelegate(), store, path, data);
        }

        @Override
        public void delete(final LogicalDatastoreType store, final InstanceIdentifier<?> path) {
            doDelete(getDelegate(), store, path);
        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            return doCommit(getDelegate());
        }
    }

    private class BindingDataReadWriteTransactionImpl extends
            BindingDataWriteTransactionImpl<DOMDataReadWriteTransaction> implements BindingDataReadWriteTransaction {

        protected BindingDataReadWriteTransactionImpl(final DOMDataReadWriteTransaction delegate,
                final BindingToNormalizedNodeCodec codec) {
            super(delegate, codec);
        }

        @Override
        public ListenableFuture<Optional<DataObject>> read(final LogicalDatastoreType store,
                final InstanceIdentifier<?> path) {
            return doRead(getDelegate(), store, path);
        }
    }
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;


import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.DataOperation;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

interface TransactionWriter<P extends Path<P>, D, T extends AsyncWriteTransaction<P, D>> {

    abstract void write(T tx,P path,D data);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static abstract class BindingTransactionWriter implements TransactionWriter<InstanceIdentifier<?>,DataObject,WriteTransaction> {

        static final BindingTransactionWriter  PUT_WRITER = new BindingTransactionWriter() {
            @Override
            public void write(WriteTransaction tx, InstanceIdentifier path, DataObject data) {
                tx.put(LogicalDatastoreType.CONFIGURATION, path, data);
            }
        };

        static final BindingTransactionWriter  MERGE_WRITER = new BindingTransactionWriter() {
            @Override
            public void write(WriteTransaction tx, InstanceIdentifier path, DataObject data) {
                tx.merge(LogicalDatastoreType.CONFIGURATION, path, data);
            }
        };

        public static BindingTransactionWriter from(DataOperation writeOperation) {
            switch (writeOperation) {
            case PUT:
                return PUT_WRITER;
            case MERGE:
                return MERGE_WRITER;
            default:
                throw new IllegalArgumentException();
            }
        }
    }

    static abstract class DomTransactionWriter implements TransactionWriter<YangInstanceIdentifier, NormalizedNode<?,?>, DOMDataWriteTransaction> {

        static final DomTransactionWriter  PUT_WRITER = new DomTransactionWriter() {
            @Override
            public void write(DOMDataWriteTransaction tx, YangInstanceIdentifier path, NormalizedNode<?,?> data) {
                tx.put(LogicalDatastoreType.CONFIGURATION, path, data);
            }
        };

        static final DomTransactionWriter  MERGE_WRITER = new DomTransactionWriter() {
            @Override
            public void write(DOMDataWriteTransaction tx, YangInstanceIdentifier path, NormalizedNode<?,?> data) {
                tx.merge(LogicalDatastoreType.CONFIGURATION, path, data);
            }
        };

        public static DomTransactionWriter from(DataOperation writeOperation) {
            switch (writeOperation) {
            case PUT:
                return PUT_WRITER;
            case MERGE:
                return MERGE_WRITER;
            default:
                throw new IllegalArgumentException();
            }
        }
    }
}

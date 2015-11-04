package org.opendaylight.controller.sal.connect.netconf.sal;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class SchemalessDataBroker implements DOMDataBroker{

    private final DOMRpcService rpcService;

    public SchemalessDataBroker(final DOMRpcService rpcService) {
        this.rpcService = rpcService;
    }

    @Override public DOMDataReadOnlyTransaction newReadOnlyTransaction() {

        return new SchemalessDOMDataReadOnlyTransaction(rpcService);
    }

    @Override public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new SchemalessDOMDataReadWriteTransaction(new SchemalessDOMDataReadOnlyTransaction(rpcService),
            new SchemalessDOMDataWriteTransaction(rpcService));
    }

    @Override public DOMDataWriteTransaction newWriteOnlyTransaction() {

        return new SchemalessDOMDataWriteTransaction(rpcService);
    }

    @Override public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(
        final LogicalDatastoreType store, final YangInstanceIdentifier path, final DOMDataChangeListener listener,
        final DataChangeScope triggeringScope) {
        return null;
    }

    @Override public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        return null;
    }

    @Nonnull @Override public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return null;
    }

    private static class SchemalessDOMDataWriteTransaction implements DOMDataWriteTransaction {

        private DOMRpcService rpcService;
        private SchemalessTransformer schemalessTransformer;

        public SchemalessDOMDataWriteTransaction(final DOMRpcService rpcService) {
            this.rpcService = rpcService;
            this.schemalessTransformer = new SchemalessTransformer();
            // Locking device
        }

        @Override public Object getIdentifier() {
            return this;
        }

        @Override public boolean cancel() {
            return false;
        }

        @Override public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {

        }

        @Override public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            rpcService.invokeRpc(null, schemalessTransformer.getCommitInput());
            return null;
        }

        @Override public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            return null;
        }

        @Override public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {

            Preconditions.checkArgument(data instanceof AnyXmlNode);
            AnyXmlNode editCfgRpcInput = schemalessTransformer.getEditCfgRpc(path, ((AnyXmlNode) data), "running");
            final CheckedFuture<DOMRpcResult, DOMRpcException> editCfgFuture = rpcService
                .invokeRpc(null, editCfgRpcInput);

            try {
                final DOMRpcResult domRpcResult = editCfgFuture.checkedGet(1,  TimeUnit.MINUTES);
            } catch (DOMRpcException e) {

                // RPC invocation failed
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException("Timeout", e);
            }
        }

        @Override public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {

        }
    }

    private class SchemalessDOMDataReadOnlyTransaction implements DOMDataReadOnlyTransaction {
        private DOMRpcService rpcService;

        public SchemalessDOMDataReadOnlyTransaction(final DOMRpcService rpcService) {
            this.rpcService = rpcService;
        }

        @Override public Object getIdentifier() {
            return this;
        }

        @Override public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final LogicalDatastoreType store, final YangInstanceIdentifier path) {
            // depending on the datastore type
            final AnyXmlNode getConfigStructure = new SchemalessTransformer().getGetConfigStructure();
            final CheckedFuture<DOMRpcResult, DOMRpcException> domRpcResultDOMRpcExceptionCheckedFuture = rpcService
                .invokeRpc(null, getConfigStructure);

            final ListenableFuture<Optional<NormalizedNode<?, ?>>> transformedFuture = Futures
                .transform(domRpcResultDOMRpcExceptionCheckedFuture,
                    new Function<DOMRpcResult, Optional<NormalizedNode<?, ?>>>() {
                        @Override public Optional<NormalizedNode<?, ?>> apply(final DOMRpcResult result) {
                            checkReadSuccess(result, path);

                            return result.getResult() == null ?
                                Optional.<NormalizedNode<?, ?>>absent() :
                                Optional.<NormalizedNode<?, ?>>of(result.getResult());
                        }
                    });

            return MappingCheckedFuture.create(transformedFuture, ReadFailedException.MAPPER);
        }

        private void checkReadSuccess(final DOMRpcResult result, final YangInstanceIdentifier path) {
            try {
                Preconditions.checkArgument(isSuccess(result), "%s: Unable to read data: %s, errors: %s", "ID HERE TODO", path,
                    result.getErrors());
            } catch (final IllegalArgumentException e) {
                throw e;
            }
        }

        boolean isSuccess(final DOMRpcResult result) {
            return result.getErrors().isEmpty();
        }

        @Override public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
            return null;
        }

        @Override public void close() {
            // TODO probably noop
        }
    }

    private class SchemalessDOMDataReadWriteTransaction implements DOMDataReadWriteTransaction {
        private SchemalessDOMDataReadOnlyTransaction schemalessDOMDataReadOnlyTransaction;
        private SchemalessDOMDataWriteTransaction schemalessDOMDataWriteTransaction;

        public SchemalessDOMDataReadWriteTransaction(
            final SchemalessDOMDataReadOnlyTransaction schemalessDOMDataReadOnlyTransaction,
            final SchemalessDOMDataWriteTransaction schemalessDOMDataWriteTransaction) {
            this.schemalessDOMDataReadOnlyTransaction = schemalessDOMDataReadOnlyTransaction;
            this.schemalessDOMDataWriteTransaction = schemalessDOMDataWriteTransaction;
        }

        @Override public boolean cancel() {
            final boolean cancel = schemalessDOMDataWriteTransaction.cancel();
            return cancel;
        }

        @Override public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            return schemalessDOMDataWriteTransaction.commit();
        }

        @Override public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
            schemalessDOMDataWriteTransaction.delete(store, path);
        }


        @Override public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
            return schemalessDOMDataReadOnlyTransaction.exists(store, path);
        }

        @Override public Object getIdentifier() {
            return Lists.newArrayList(schemalessDOMDataReadOnlyTransaction.getIdentifier(), schemalessDOMDataWriteTransaction.getIdentifier());
        }

        @Override public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
            schemalessDOMDataWriteTransaction.merge(store, path, data);
        }

        @Override public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
            schemalessDOMDataWriteTransaction.put(store, path, data);
        }

        @Override public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            return schemalessDOMDataWriteTransaction.submit();
        }

        @Override public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final LogicalDatastoreType store, final YangInstanceIdentifier path) {
            return schemalessDOMDataReadOnlyTransaction.read(store, path);
        }


    }
}

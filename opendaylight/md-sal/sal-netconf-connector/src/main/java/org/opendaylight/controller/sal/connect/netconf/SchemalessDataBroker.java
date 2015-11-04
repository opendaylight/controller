package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Created by mmarsale on 4.11.2015.
 */
public class SchemalessDataBroker implements DOMDataBroker {

    @Override public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return null;
    }

    @Override public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return null;
    }

    @Override public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new DOMDataWriteTransaction() {
            @Override public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                final NormalizedNode<?, ?> data) {

                Preconditions.checkArgument(data instanceof AnyXmlNode);
                final DOMSource value = ((AnyXmlNode) data).getValue();

                // wrap in edit-config

                // send to device

            }

            @Override public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                final NormalizedNode<?, ?> data) {

            }

            @Override public boolean cancel() {
                return false;
            }

            @Override public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {

            }

            @Override public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                return null;
            }

            @Override public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                return null;
            }

            @Override public Object getIdentifier() {
                return null;
            }
        };
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
}

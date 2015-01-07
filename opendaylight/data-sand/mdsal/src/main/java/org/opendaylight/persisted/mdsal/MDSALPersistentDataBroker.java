package org.opendaylight.persisted.mdsal;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.datasand.store.ObjectDataStore;
import org.opendaylight.datasand.store.bytearray.ByteArrayObjectDataStore;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class MDSALPersistentDataBroker implements DataBroker {

    private ObjectDataStore db = null;

    public MDSALPersistentDataBroker() {
        db = new ByteArrayObjectDataStore("MDSALObjectStore",true);
    }

    public void close() {
        db.close();
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            LogicalDatastoreType arg0, InstanceIdentifier<?> arg1,
            DataChangeListener arg2, DataChangeScope arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectDataStore getDataBase() {
        return this.db;
    }

    @Override
    public BindingTransactionChain createTransactionChain(
            TransactionChainListener arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new MyReadWriteTransaction();
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return null;
    }

    public class MyReadWriteTransaction implements ReadWriteTransaction {

        private int readIndex = 0;
        private Class<? extends DataObject> readType = null;

        public void setReadIndex(int i) {
            this.readIndex = i;
        }

        public int getReadIndex() {
            return this.readIndex;
        }

        public void setReadType(Class<? extends DataObject> cls) {
            this.readType = cls;
        }

        public Class<? extends DataObject> getReadType() {
            return this.readType;
        }

        @Override
        public void delete(LogicalDatastoreType arg0, InstanceIdentifier<?> arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType arg0,
                InstanceIdentifier<T> arg1, T arg2, boolean arg3) {
            // TODO Auto-generated method stub

        }

        @Override
        public <T extends DataObject> void merge(LogicalDatastoreType arg0,
                InstanceIdentifier<T> arg1, T arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType arg0,
                InstanceIdentifier<T> arg1, T arg2, boolean arg3) {
            db.write(arg2, -1);
        }

        @Override
        public <T extends DataObject> void put(LogicalDatastoreType arg0,
                InstanceIdentifier<T> arg1, T arg2) {
            db.write(arg2, -1);
        }

        @Override
        public boolean cancel() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public ListenableFuture<RpcResult<TransactionStatus>> commit() {
            db.commit();
            return null;
        }

        @Override
        public CheckedFuture<Void, TransactionCommitFailedException> submit() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <T extends DataObject> CheckedFuture<Optional<T>, ReadFailedException> read(
                LogicalDatastoreType arg0, InstanceIdentifier<T> arg1) {
            if (arg1 == null) {
                T dObject = (T) db.read(readType, readIndex);
                return Futures.immediateCheckedFuture(Optional.of(dObject));
            }
            return null;
        }

        @Override
        public Object getIdentifier() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}

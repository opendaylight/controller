package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import org.opendaylight.yangtools.concepts.ListenerRegistration
import com.google.common.collect.Multimap
import static com.google.common.base.Preconditions.*;
import java.util.List
import com.google.common.collect.HashMultimap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.controller.sal.common.util.Rpcs
import java.util.Collections
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import java.util.ArrayList
import org.opendaylight.controller.sal.binding.impl.util.BindingAwareDataReaderRouter
import org.opendaylight.yangtools.concepts.CompositeObjectRegistration
import java.util.Arrays

class DataBrokerImpl extends DeprecatedDataAPISupport implements DataProviderService {

    @Property
    var ExecutorService executor;

    val dataReadRouter = new BindingAwareDataReaderRouter;

    Multimap<InstanceIdentifier, DataChangeListenerRegistration> listeners = HashMultimap.create();
    Multimap<InstanceIdentifier, DataCommitHandlerRegistration> commitHandlers = HashMultimap.create();

    override beginTransaction() {
        return new DataTransactionImpl(this);
    }

    override readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        return dataReadRouter.readConfigurationData(path);
    }

    override readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        return dataReadRouter.readOperationalData(path);
    }

    override registerCommitHandler(InstanceIdentifier<? extends DataObject> path,
        DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> commitHandler) {
            val registration = new DataCommitHandlerRegistration(path,commitHandler,this);
            commitHandlers.put(path,registration)
            return registration;
    }

    override registerDataChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener listener) {
        val reg = new DataChangeListenerRegistration(path, listener, this);
        listeners.put(path, reg);
        return reg;
    }

    override registerDataReader(InstanceIdentifier<? extends DataObject> path,DataReader<InstanceIdentifier<? extends DataObject>,DataObject> reader) {
        
        val confReg = dataReadRouter.registerConfigurationReader(path,reader);
        val dataReg = dataReadRouter.registerOperationalReader(path,reader);
        
        return new CompositeObjectRegistration(reader,Arrays.asList(confReg,dataReg));
    }

    protected def removeListener(DataChangeListenerRegistration registration) {
        listeners.remove(registration.path, registration);
    }

    protected def removeCommitHandler(DataCommitHandlerRegistration registration) {
        commitHandlers.remove(registration.path, registration);
    }
    
    protected def getActiveCommitHandlers() {
        return commitHandlers.entries.map[ value.instance].toSet
    }

    protected def commit(DataTransactionImpl transaction) {
        checkNotNull(transaction);
        transaction.changeStatus(TransactionStatus.SUBMITED);
        val task = new TwoPhaseCommit(transaction, this);
        return executor.submit(task);
    }

}

package class DataChangeListenerRegistration extends AbstractObjectRegistration<DataChangeListener> implements ListenerRegistration<DataChangeListener> {

    DataBrokerImpl dataBroker;

    @Property
    val InstanceIdentifier<?> path;

    new(InstanceIdentifier<?> path, DataChangeListener instance, DataBrokerImpl broker) {
        super(instance)
        dataBroker = broker;
        _path = path;
    }

    override protected removeRegistration() {
        dataBroker.removeListener(this);
        dataBroker = null;
    }

}

package class DataCommitHandlerRegistration //
extends AbstractObjectRegistration<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> {

    DataBrokerImpl dataBroker;

    @Property
    val InstanceIdentifier<?> path;

    new(InstanceIdentifier<?> path, DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> instance,
        DataBrokerImpl broker) {
        super(instance)
        dataBroker = broker;
        _path = path;
    }

    override protected removeRegistration() {
        dataBroker.removeCommitHandler(this);
        dataBroker = null;
    }

}

package class TwoPhaseCommit implements Callable<RpcResult<TransactionStatus>> {

    val DataTransactionImpl transaction;
    val DataBrokerImpl dataBroker;

    new(DataTransactionImpl transaction, DataBrokerImpl broker) {
        this.transaction = transaction;
        this.dataBroker = broker;
    }

    override call() throws Exception {

        val Iterable<DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject>> commitHandlers = dataBroker.activeCommitHandlers;

        // requesting commits
        val List<DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject>> handlerTransactions = new ArrayList();
        try {
            for (handler : commitHandlers) {
                handlerTransactions.add(handler.requestCommit(transaction));
            }
        } catch (Exception e) {
            return rollback(handlerTransactions,e);
        }
        val List<RpcResult<Void>> results = new ArrayList();
        try {
            for (subtransaction : handlerTransactions) {
                results.add(subtransaction.finish());
            }
        } catch (Exception e) {
            return rollback(handlerTransactions,e);
        }

        return Rpcs.getRpcResult(true, TransactionStatus.COMMITED, Collections.emptySet());
    }

    def rollback(List<DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject>> transactions,Exception e) {
        for (transaction : transactions) {
            transaction.rollback()
        }
        // FIXME return encoutered error.
        return Rpcs.getRpcResult(false, TransactionStatus.FAILED, Collections.emptySet());
    }
}

package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction.DataTransactionListener
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.md.sal.common.impl.AbstractDataModification
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import org.opendaylight.yangtools.concepts.ListenerRegistration
import static extension org.opendaylight.controller.sal.binding.impl.util.MapUtils.*;
import java.util.Collection
import java.util.Map.Entry
import java.util.HashSet
import java.util.Set
import com.google.common.collect.Multimap
import static com.google.common.base.Preconditions.*;
import java.util.List
import java.util.LinkedList
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider
import com.google.common.collect.HashMultimap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.controller.sal.common.util.Rpcs
import java.util.Collections
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import java.util.ArrayList
import org.opendaylight.controller.sal.common.util.RpcErrors

class DataBrokerImpl extends DeprecatedDataAPISupport implements DataProviderService {

    @Property
    var ExecutorService executor;

    Multimap<InstanceIdentifier, DataReaderRegistration> configReaders = HashMultimap.create();
    Multimap<InstanceIdentifier, DataReaderRegistration> operationalReaders = HashMultimap.create();
    Multimap<InstanceIdentifier, DataChangeListenerRegistration> listeners = HashMultimap.create();
    Multimap<InstanceIdentifier, DataCommitHandlerRegistration> commitHandlers = HashMultimap.create();

    override beginTransaction() {
        return new DataTransactionImpl(this);
    }

    override readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        val readers = configReaders.getAllChildren(path);
        return readers.readConfiguration(path);
    }

    override readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        val readers = operationalReaders.getAllChildren(path);
        return readers.readOperational(path);
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

    override registerDataReader(InstanceIdentifier<? extends DataObject> path,
        DataReader<InstanceIdentifier<? extends DataObject>, DataObject> provider) {
        val ret = new DataReaderRegistration(provider, this);
        ret.paths.add(path);
        configReaders.put(path, ret);
        operationalReaders.put(path, ret);
        return ret;
    }

    protected def removeReader(DataReaderRegistration reader) {
        for (path : reader.paths) {
            operationalReaders.remove(path, reader);
            configReaders.remove(path, reader);
        }
    }

    protected def removeListener(DataChangeListenerRegistration registration) {
        listeners.remove(registration.path, registration);
    }

    protected def removeCommitHandler(DataCommitHandlerRegistration registration) {
        commitHandlers.remove(registration.path, registration);
    }

    protected def DataObject readConfiguration(
        Collection<Entry<? extends InstanceIdentifier, ? extends DataReaderRegistration>> entries,
        InstanceIdentifier<? extends DataObject> path) {

        val List<DataObject> partialResults = new LinkedList();
        for (entry : entries) {
            partialResults.add(entry.value.instance.readConfigurationData(path))
        }
        return merge(path, partialResults);
    }

    protected def DataObject readOperational(
        Collection<Entry<? extends InstanceIdentifier, ? extends DataReaderRegistration>> entries,
        InstanceIdentifier<? extends DataObject> path) {

        val List<DataObject> partialResults = new LinkedList();
        for (entry : entries) {
            partialResults.add(entry.value.instance.readOperationalData(path))
        }
        return merge(path, partialResults);
    }

    protected def DataObject merge(InstanceIdentifier<? extends DataObject> identifier, List<DataObject> objects) {

        // FIXME: implement real merge
        if (objects.size > 0) {
            return objects.get(0);
        }
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

package class DataReaderRegistration extends //
AbstractObjectRegistration<DataReader<InstanceIdentifier<? extends DataObject>, DataObject>> {

    DataBrokerImpl dataBroker;

    @Property
    val Set<InstanceIdentifier<? extends DataObject>> paths;

    new(DataReader<InstanceIdentifier<? extends DataObject>, DataObject> instance, DataBrokerImpl broker) {
        super(instance)
        dataBroker = broker;
        _paths = new HashSet();
    }

    override protected removeRegistration() {
        dataBroker.removeReader(this);
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

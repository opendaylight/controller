package org.opendaylight.controller.md.sal.common.impl.service

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
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
import java.util.Collections
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import java.util.ArrayList
import org.opendaylight.yangtools.concepts.CompositeObjectRegistration
import java.util.Arrays
import org.opendaylight.controller.md.sal.common.api.data.DataProvisionService
import org.opendaylight.controller.md.sal.common.api.data.DataModificationTransactionFactory
import org.opendaylight.controller.md.sal.common.api.data.DataChangePublisher
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener
import org.opendaylight.controller.sal.common.util.Rpcs
import org.opendaylight.controller.md.sal.common.impl.AbstractDataModification
import java.util.concurrent.Future
import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter
import org.opendaylight.yangtools.concepts.Path
import org.slf4j.LoggerFactory

abstract class AbstractDataBroker<P extends Path<P>,D,DCL extends DataChangeListener<P,D>> implements 
DataModificationTransactionFactory<P, D>, //
DataReader<P, D>, //
DataChangePublisher<P, D, DCL>, //
DataProvisionService<P,D> {

    @Property
    var ExecutorService executor;

    @Property
    var AbstractDataReadRouter<P,D> dataReadRouter;

    Multimap<P, DataChangeListenerRegistration<P,D,DCL>> listeners = HashMultimap.create();
    Multimap<P, DataCommitHandlerRegistration<P,D>> commitHandlers = HashMultimap.create();


    public new() {
        
    }

    override final readConfigurationData(P path) {
        return dataReadRouter.readConfigurationData(path);
    }

    override final readOperationalData(P path) {
        return dataReadRouter.readOperationalData(path);
    }

    override final registerCommitHandler(P path,
        DataCommitHandler<P, D> commitHandler) {
            val registration = new DataCommitHandlerRegistration(path,commitHandler,this);
            commitHandlers.put(path,registration)
            return registration;
    }

    override final def registerDataChangeListener(P path, DCL listener) {
        val reg = new DataChangeListenerRegistration(path, listener, this);
        listeners.put(path, reg);
        return reg;
    }

     final def registerDataReader(P path,DataReader<P,D> reader) {
        
        val confReg = dataReadRouter.registerConfigurationReader(path,reader);
        val dataReg = dataReadRouter.registerOperationalReader(path,reader);
        
        return new CompositeObjectRegistration(reader,Arrays.asList(confReg,dataReg));
    }

    protected  final def removeListener(DataChangeListenerRegistration<P,D,DCL> registration) {
        listeners.remove(registration.path, registration);
    }

    protected  final def removeCommitHandler(DataCommitHandlerRegistration<P,D> registration) {
        commitHandlers.remove(registration.path, registration);
    }
    
    protected  final def getActiveCommitHandlers() {
        return commitHandlers.entries.map[ value.instance].toSet
    }

    package final def Future<RpcResult<TransactionStatus>>  commit(AbstractDataTransaction<P,D> transaction) {
        checkNotNull(transaction);
        transaction.changeStatus(TransactionStatus.SUBMITED);
        val task = new TwoPhaseCommit(transaction, this);
        return executor.submit(task);
    }

}

package class DataChangeListenerRegistration<P extends Path<P>,D,DCL extends DataChangeListener<P,D>> extends AbstractObjectRegistration<DCL> implements ListenerRegistration<DCL> {

    AbstractDataBroker<P,D,DCL> dataBroker;

    @Property
    val P path;

    new(P path, DCL instance, AbstractDataBroker<P,D,DCL> broker) {
        super(instance)
        dataBroker = broker;
        _path = path;
    }

    override protected removeRegistration() {
        dataBroker.removeListener(this);
        dataBroker = null;
    }

}

package class DataCommitHandlerRegistration<P extends Path<P>,D>
extends AbstractObjectRegistration<DataCommitHandler<P, D>> {

    AbstractDataBroker<P,D,?> dataBroker;

    @Property
    val P path;

    new(P path, DataCommitHandler<P, D> instance,
        AbstractDataBroker<P,D,?> broker) {
        super(instance)
        dataBroker = broker;
        _path = path;
    }

    override protected removeRegistration() {
        dataBroker.removeCommitHandler(this);
        dataBroker = null;
    }

}

package class TwoPhaseCommit<P extends Path<P>,D> implements Callable<RpcResult<TransactionStatus>> {
    
    private static val log = LoggerFactory.getLogger(TwoPhaseCommit);

    val AbstractDataTransaction<P,D> transaction;
    val AbstractDataBroker<P,D,?> dataBroker;

    new(AbstractDataTransaction<P,D> transaction, AbstractDataBroker<P,D,?> broker) {
        this.transaction = transaction;
        this.dataBroker = broker;
    }

    override call() throws Exception {

        val Iterable<DataCommitHandler<P, D>> commitHandlers = dataBroker.activeCommitHandlers;

        // requesting commits
        val List<DataCommitTransaction<P, D>> handlerTransactions = new ArrayList();
        try {
            for (handler : commitHandlers) {
                handlerTransactions.add(handler.requestCommit(transaction));
            }
        } catch (Exception e) {
            log.error("Request Commit failded",e);
            return rollback(handlerTransactions,e);
        }
        val List<RpcResult<Void>> results = new ArrayList();
        try {
            for (subtransaction : handlerTransactions) {
                results.add(subtransaction.finish());
            }
        } catch (Exception e) {
            log.error("Finish Commit failed",e);
            return rollback(handlerTransactions,e);
        }

        return Rpcs.getRpcResult(true, TransactionStatus.COMMITED, Collections.emptySet());
    }

    def rollback(List<DataCommitTransaction<P, D>> transactions,Exception e) {
        for (transaction : transactions) {
            transaction.rollback()
        }
        // FIXME return encountered error.
        return Rpcs.getRpcResult(false, TransactionStatus.FAILED, Collections.emptySet());
    }
}

public abstract class AbstractDataTransaction<P extends Path<P>, D> extends AbstractDataModification<P, D> {

    @Property
    private val Object identifier;

    
    var TransactionStatus status;
    
    
    var AbstractDataBroker<P, D, ?> broker;

    protected new (AbstractDataBroker<P,D,?> dataBroker) {
        super(dataBroker);
        _identifier = new Object();
        broker = dataBroker;
        status = TransactionStatus.NEW;
        //listeners = new ListenerRegistry<>();
    }

    override  commit() {
        return broker.commit(this);
    }

    override readConfigurationData(P path) {
        return broker.readConfigurationData(path);
    }

    override readOperationalData(P path) {
        return broker.readOperationalData(path);
    }

    override hashCode() {
        return identifier.hashCode;
    }

    override equals(Object obj) {
        if (this === obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        val other = (obj as AbstractDataTransaction<P,D>) ;
        if (broker == null) {
            if (other.broker != null)
                return false;
        } else if (!broker.equals(other.broker))
            return false;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        return true;
    }

    override TransactionStatus getStatus() {
        return status;
    }

    
    protected abstract def void onStatusChange(TransactionStatus status);
    
    public def changeStatus(TransactionStatus status) {
        this.status = status;
        onStatusChange(status);
    }
    
}

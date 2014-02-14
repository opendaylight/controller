/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service

import com.google.common.collect.FluentIterable
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.Set
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import org.opendaylight.controller.md.sal.common.api.RegistrationListener
import org.opendaylight.controller.md.sal.common.api.TransactionStatus
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener
import org.opendaylight.controller.md.sal.common.api.data.DataChangePublisher
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration
import org.opendaylight.controller.md.sal.common.api.data.DataModificationTransactionFactory
import org.opendaylight.controller.md.sal.common.api.data.DataProvisionService
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.md.sal.common.impl.AbstractDataModification
import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration
import org.opendaylight.yangtools.concepts.CompositeObjectRegistration
import org.opendaylight.yangtools.concepts.ListenerRegistration
import org.opendaylight.yangtools.concepts.Path
import org.opendaylight.yangtools.concepts.util.ListenerRegistry
import org.opendaylight.yangtools.yang.common.RpcResult
import org.slf4j.LoggerFactory

import static com.google.common.base.Preconditions.*
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent
import com.google.common.collect.Multimaps
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.Map
import com.google.common.base.Predicate
import com.google.common.util.concurrent.MoreExecutors

abstract class AbstractDataBroker<P extends Path<P>, D, DCL extends DataChangeListener<P, D>> implements DataModificationTransactionFactory<P, D>, //
DataReader<P, D>, //
DataChangePublisher<P, D, DCL>, //
DataProvisionService<P, D> {

    private static val LOG = LoggerFactory.getLogger(AbstractDataBroker);

    @Property
    var ExecutorService executor;
    
    @Property
    var ExecutorService notificationExecutor = MoreExecutors.sameThreadExecutor;

    @Property
    var AbstractDataReadRouter<P, D> dataReadRouter;

    @Property
    private val AtomicLong submittedTransactionsCount = new AtomicLong;

    @Property
    private val AtomicLong failedTransactionsCount = new AtomicLong

    @Property
    private val AtomicLong finishedTransactionsCount = new AtomicLong

    Multimap<P, DataChangeListenerRegistration<P, D, DCL>> listeners = Multimaps.
        synchronizedSetMultimap(HashMultimap.create());
    Multimap<P, DataCommitHandlerRegistrationImpl<P, D>> commitHandlers = Multimaps.
        synchronizedSetMultimap(HashMultimap.create());

    private val Lock registrationLock = new ReentrantLock;

    val ListenerRegistry<RegistrationListener<DataCommitHandlerRegistration<P, D>>> commitHandlerRegistrationListeners = new ListenerRegistry();

    public new() {
    }

    protected def /*Iterator<Entry<Collection<DataChangeListenerRegistration<P,D,DCL>>,D>>*/ affectedCommitHandlers(
        Set<P> paths) {
        return withLock(registrationLock) [ |
            return FluentIterable.from(commitHandlers.asMap.entrySet).filter[key.isAffectedBy(paths)] //
            .transformAndConcat[value] //
            .transform[instance].toList()
        ]
    }

    protected def /*Iterator<Entry<Collection<DataChangeListenerRegistration<P,D,DCL>>,D>>*/ probablyAffectedCommitHandlers(
        HashSet<P> paths) {
        return withLock(registrationLock) [ |
            return FluentIterable.from(commitHandlers.asMap.entrySet).filter[key.isProbablyAffectedBy(paths)] //
            .
                transformAndConcat[value] //
                .transform[instance].toList()
        ]
    }
    
    protected def Map<P,D> deepGetBySubpath(Map<P,D> dataSet,P path) {
        return Collections.emptyMap();
    }

    override final readConfigurationData(P path) {
        return dataReadRouter.readConfigurationData(path);
    }

    override final readOperationalData(P path) {
        return dataReadRouter.readOperationalData(path);
    }

    private static def <T> withLock(Lock lock, Callable<T> method) {
        lock.lock
        try {
            return method.call
        } finally {
            lock.unlock
        }
    }

    override final registerCommitHandler(P path, DataCommitHandler<P, D> commitHandler) {
        return withLock(registrationLock) [ |
            val registration = new DataCommitHandlerRegistrationImpl(path, commitHandler, this);
            commitHandlers.put(path, registration)
            LOG.trace("Registering Commit Handler {} for path: {}", commitHandler, path);
            for (listener : commitHandlerRegistrationListeners) {
                try {
                    listener.instance.onRegister(registration);
                } catch (Exception e) {
                    LOG.error("Unexpected exception in listener {} during invoking onRegister", listener.instance, e);
                }
            }
            return registration;
        ]
    }

    override final def registerDataChangeListener(P path, DCL listener) {
        return withLock(registrationLock) [ |
            val reg = new DataChangeListenerRegistration(path, listener, this);
            listeners.put(path, reg);
            val initialConfig = dataReadRouter.readConfigurationData(path);
            val initialOperational = dataReadRouter.readOperationalData(path);
            val event = createInitialListenerEvent(path, initialConfig, initialOperational);
            listener.onDataChanged(event);
            return reg;
        ]
    }

    final def registerDataReader(P path, DataReader<P, D> reader) {
        return withLock(registrationLock) [ |
            val confReg = dataReadRouter.registerConfigurationReader(path, reader);
            val dataReg = dataReadRouter.registerOperationalReader(path, reader);
            return new CompositeObjectRegistration(reader, Arrays.asList(confReg, dataReg));
        ]
    }

    override registerCommitHandlerListener(
        RegistrationListener<DataCommitHandlerRegistration<P, D>> commitHandlerListener) {
        val ret = commitHandlerRegistrationListeners.register(commitHandlerListener);
        return ret;
    }

    protected def DataChangeEvent<P, D> createInitialListenerEvent(P path, D initialConfig, D initialOperational) {
        return new InitialDataChangeEventImpl<P, D>(initialConfig, initialOperational);

    }

    protected final def removeListener(DataChangeListenerRegistration<P, D, DCL> registration) {
        return withLock(registrationLock) [ |
            listeners.remove(registration.path, registration);
        ]
    }

    protected final def removeCommitHandler(DataCommitHandlerRegistrationImpl<P, D> registration) {
        return withLock(registrationLock) [ |
            commitHandlers.remove(registration.path, registration);
            LOG.trace("Removing Commit Handler {} for path: {}", registration.instance, registration.path);
            for (listener : commitHandlerRegistrationListeners) {
                try {
                    listener.instance.onUnregister(registration);
                } catch (Exception e) {
                    LOG.error("Unexpected exception in listener {} during invoking onUnregister", listener.instance, e);
                }
            }
            return null;
        ]
    }

    protected final def getActiveCommitHandlers() {
        return commitHandlers.entries;
    }

    protected def /*Iterator<Entry<Collection<DataChangeListenerRegistration<P,D,DCL>>,D>>*/ affectedListeners(
        Set<P> paths) {
        return withLock(registrationLock) [ |
            return FluentIterable.from(listeners.asMap.entrySet).filter[key.isAffectedBy(paths)].transform [
                return new ListenerStateCapture<P,D,DCL>(key, value,createContainsPredicate(key))
            ].toList()
        ]
    }

    protected def /*Iterator<Entry<Collection<DataChangeListenerRegistration<P,D,DCL>>,D>>*/ probablyAffectedListeners(
        Set<P> paths) {
        return withLock(registrationLock) [ |
            return FluentIterable.from(listeners.asMap.entrySet).filter[key.isProbablyAffectedBy(paths)].transform [
                return new ListenerStateCapture(key, value,createContainsPredicate(key))
            ].toList()
        ]
    }
    
    
    protected def Predicate<P> createContainsPredicate(P key) {
        return [ other | key.contains(other) ]
    }
    
    protected def Predicate<P> createIsContainedPredicate(P key) {
        return [ other | other.contains(key) ]
    }

    protected def boolean isAffectedBy(P key,Set<P> paths) {
        val contains = createContainsPredicate(key)
        if (paths.contains(key)) {
            return true;
        }
        for (path : paths) {
            if (contains.apply(path)) {
                return true;
            }
        }

        return false;
    }

    protected def boolean isProbablyAffectedBy(P key, Set<P> paths) {
        val isContained = createIsContainedPredicate(key)
        for (path : paths) {
            if (isContained.apply(path)) {
                return true;
            }
        }
        return false;
    }

    package final def Future<RpcResult<TransactionStatus>> commit(AbstractDataTransaction<P, D> transaction) {
        checkNotNull(transaction);
        transaction.changeStatus(TransactionStatus.SUBMITED);
        val task = new TwoPhaseCommit(transaction, this);
        submittedTransactionsCount.andIncrement;
        return executor.submit(task);
    }

}

package class DataChangeListenerRegistration<P extends Path<P>, D, DCL extends DataChangeListener<P, D>> extends AbstractObjectRegistration<DCL> implements ListenerRegistration<DCL> {

    AbstractDataBroker<P, D, DCL> dataBroker;

    @Property
    val P path;

    new(P path, DCL instance, AbstractDataBroker<P, D, DCL> broker) {
        super(instance)
        dataBroker = broker;
        _path = path;
    }

    override protected removeRegistration() {
        dataBroker.removeListener(this);
        dataBroker = null;
    }

}

package class DataCommitHandlerRegistrationImpl<P extends Path<P>, D> //
extends AbstractObjectRegistration<DataCommitHandler<P, D>> //
implements DataCommitHandlerRegistration<P, D> {

    AbstractDataBroker<P, D, ?> dataBroker;

    @Property
    val P path;

    new(P path, DataCommitHandler<P, D> instance, AbstractDataBroker<P, D, ?> broker) {
        super(instance)
        dataBroker = broker;
        _path = path;
    }

    override protected removeRegistration() {
        dataBroker.removeCommitHandler(this);
        dataBroker = null;
    }
}

public abstract class AbstractDataTransaction<P extends Path<P>, D> extends AbstractDataModification<P, D> {

    private static val LOG = LoggerFactory.getLogger(AbstractDataTransaction);

    @Property
    private val Object identifier;

    var TransactionStatus status;

    var AbstractDataBroker<P, D, ?> broker;

    protected new(Object identifier, AbstractDataBroker<P, D, ?> dataBroker) {
        super(dataBroker);
        _identifier = identifier;
        broker = dataBroker;
        status = TransactionStatus.NEW;
        LOG.debug("Transaction {} Allocated.", identifier);

    //listeners = new ListenerRegistry<>();
    }

    override commit() {
        return broker.commit(this);
    }

    override readConfigurationData(P path) {
        val local = this.updatedConfigurationData.get(path);
        if (local != null) {
            return local;
        }

        return broker.readConfigurationData(path);
    }

    override readOperationalData(P path) {
        val local = this.updatedOperationalData.get(path);
        if (local != null) {
            return local;
        }
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
        val other = (obj as AbstractDataTransaction<P,D>);
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
        LOG.debug("Transaction {} transitioned from {} to {}", identifier, this.status, status);
        this.status = status;
        onStatusChange(status);
    }

}

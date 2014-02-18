/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.xtext.xbase.lib.Exceptions;
import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.DataChangePublisher;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataModificationTransactionFactory;
import org.opendaylight.controller.md.sal.common.api.data.DataProvisionService;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.routing.AbstractDataReadRouter;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.CompositeObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.MoreExecutors;

public abstract class AbstractDataBroker<P extends Path<P>, D extends Object, DCL extends DataChangeListener<P, D>>
        implements DataModificationTransactionFactory<P, D>, DataReader<P, D>, DataChangePublisher<P, D, DCL>,
        DataProvisionService<P, D> {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDataBroker.class);

    private ExecutorService executor;

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public void setExecutor(final ExecutorService executor) {
        this.executor = executor;
    }

    private ExecutorService notificationExecutor = MoreExecutors.sameThreadExecutor();

    public ExecutorService getNotificationExecutor() {
        return this.notificationExecutor;
    }

    public void setNotificationExecutor(final ExecutorService notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    private AbstractDataReadRouter<P, D> dataReadRouter;

    private final AtomicLong submittedTransactionsCount = new AtomicLong();

    private final AtomicLong failedTransactionsCount = new AtomicLong();

    private final AtomicLong finishedTransactionsCount = new AtomicLong();

    public AbstractDataReadRouter<P, D> getDataReadRouter() {
        return this.dataReadRouter;
    }

    public void setDataReadRouter(final AbstractDataReadRouter<P, D> dataReadRouter) {
        this.dataReadRouter = dataReadRouter;
    }

    public AtomicLong getSubmittedTransactionsCount() {
        return this.submittedTransactionsCount;
    }

    public AtomicLong getFailedTransactionsCount() {
        return this.failedTransactionsCount;
    }

    public AtomicLong getFinishedTransactionsCount() {
        return this.finishedTransactionsCount;
    }

    private final Multimap<P, DataChangeListenerRegistration<P, D, DCL>> listeners = Multimaps
            .synchronizedSetMultimap(HashMultimap.<P, DataChangeListenerRegistration<P, D, DCL>> create());

    private final Multimap<P, DataCommitHandlerRegistrationImpl<P, D>> commitHandlers = Multimaps
            .synchronizedSetMultimap(HashMultimap.<P, DataCommitHandlerRegistrationImpl<P, D>> create());

    private final Lock registrationLock = new ReentrantLock();

    private final ListenerRegistry<RegistrationListener<DataCommitHandlerRegistration<P, D>>> commitHandlerRegistrationListeners = new ListenerRegistry<RegistrationListener<DataCommitHandlerRegistration<P, D>>>();

    public AbstractDataBroker() {
    }

    protected ImmutableList<DataCommitHandler<P, D>> affectedCommitHandlers(final Set<P> paths) {
        final Callable<ImmutableList<DataCommitHandler<P, D>>> _function = new Callable<ImmutableList<DataCommitHandler<P, D>>>() {
            @Override
            public ImmutableList<DataCommitHandler<P, D>> call() throws Exception {
                Map<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>> _asMap = commitHandlers.asMap();
                Set<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _entrySet = _asMap.entrySet();
                FluentIterable<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _from = FluentIterable
                        .<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> from(_entrySet);
                final Predicate<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _function = new Predicate<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>>() {
                    @Override
                    public boolean apply(final Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>> it) {
                        P _key = it.getKey();
                        boolean _isAffectedBy = isAffectedBy(_key, paths);
                        return _isAffectedBy;
                    }
                };
                FluentIterable<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _filter = _from
                        .filter(_function);
                final Function<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>, Collection<DataCommitHandlerRegistrationImpl<P, D>>> _function_1 = new Function<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>, Collection<DataCommitHandlerRegistrationImpl<P, D>>>() {
                    @Override
                    public Collection<DataCommitHandlerRegistrationImpl<P, D>> apply(
                            final Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>> it) {
                        Collection<DataCommitHandlerRegistrationImpl<P, D>> _value = it.getValue();
                        return _value;
                    }
                };
                FluentIterable<DataCommitHandlerRegistrationImpl<P, D>> _transformAndConcat = _filter
                        .<DataCommitHandlerRegistrationImpl<P, D>> transformAndConcat(_function_1);
                final Function<DataCommitHandlerRegistrationImpl<P, D>, DataCommitHandler<P, D>> _function_2 = new Function<DataCommitHandlerRegistrationImpl<P, D>, DataCommitHandler<P, D>>() {
                    @Override
                    public DataCommitHandler<P, D> apply(final DataCommitHandlerRegistrationImpl<P, D> it) {
                        DataCommitHandler<P, D> _instance = it.getInstance();
                        return _instance;
                    }
                };
                FluentIterable<DataCommitHandler<P, D>> _transform = _transformAndConcat
                        .<DataCommitHandler<P, D>> transform(_function_2);
                return _transform.toList();
            }
        };
        return AbstractDataBroker.<ImmutableList<DataCommitHandler<P, D>>> withLock(this.registrationLock, _function);
    }

    protected ImmutableList<DataCommitHandler<P, D>> probablyAffectedCommitHandlers(final HashSet<P> paths) {
        final Callable<ImmutableList<DataCommitHandler<P, D>>> _function = new Callable<ImmutableList<DataCommitHandler<P, D>>>() {
            @Override
            public ImmutableList<DataCommitHandler<P, D>> call() throws Exception {
                Map<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>> _asMap = commitHandlers.asMap();
                Set<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _entrySet = _asMap.entrySet();
                FluentIterable<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _from = FluentIterable
                        .<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> from(_entrySet);
                final Predicate<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _function = new Predicate<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>>() {
                    @Override
                    public boolean apply(final Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>> it) {
                        P _key = it.getKey();
                        boolean _isProbablyAffectedBy = isProbablyAffectedBy(_key, paths);
                        return _isProbablyAffectedBy;
                    }
                };
                FluentIterable<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>> _filter = _from
                        .filter(_function);
                final Function<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>, Collection<DataCommitHandlerRegistrationImpl<P, D>>> _function_1 = new Function<Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>>, Collection<DataCommitHandlerRegistrationImpl<P, D>>>() {
                    @Override
                    public Collection<DataCommitHandlerRegistrationImpl<P, D>> apply(
                            final Entry<P, Collection<DataCommitHandlerRegistrationImpl<P, D>>> it) {
                        Collection<DataCommitHandlerRegistrationImpl<P, D>> _value = it.getValue();
                        return _value;
                    }
                };
                FluentIterable<DataCommitHandlerRegistrationImpl<P, D>> _transformAndConcat = _filter
                        .<DataCommitHandlerRegistrationImpl<P, D>> transformAndConcat(_function_1);
                final Function<DataCommitHandlerRegistrationImpl<P, D>, DataCommitHandler<P, D>> _function_2 = new Function<DataCommitHandlerRegistrationImpl<P, D>, DataCommitHandler<P, D>>() {
                    @Override
                    public DataCommitHandler<P, D> apply(final DataCommitHandlerRegistrationImpl<P, D> it) {
                        DataCommitHandler<P, D> _instance = it.getInstance();
                        return _instance;
                    }
                };
                FluentIterable<DataCommitHandler<P, D>> _transform = _transformAndConcat
                        .<DataCommitHandler<P, D>> transform(_function_2);
                return _transform.toList();
            }
        };
        return AbstractDataBroker.<ImmutableList<DataCommitHandler<P, D>>> withLock(this.registrationLock, _function);
    }

    protected Map<P, D> deepGetBySubpath(final Map<P, D> dataSet, final P path) {
        return Collections.<P, D> emptyMap();
    }

    @Override
    public final D readConfigurationData(final P path) {
        AbstractDataReadRouter<P, D> _dataReadRouter = this.getDataReadRouter();
        return _dataReadRouter.readConfigurationData(path);
    }

    @Override
    public final D readOperationalData(final P path) {
        AbstractDataReadRouter<P, D> _dataReadRouter = this.getDataReadRouter();
        return _dataReadRouter.readOperationalData(path);
    }

    private static <T extends Object> T withLock(final Lock lock, final Callable<T> method) {
        lock.lock();
        try {
            return method.call();
        } catch (Exception e) {
            throw Exceptions.sneakyThrow(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final Registration<DataCommitHandler<P, D>> registerCommitHandler(final P path,
            final DataCommitHandler<P, D> commitHandler) {
        synchronized (commitHandler) {
            final DataCommitHandlerRegistrationImpl<P, D> registration = new DataCommitHandlerRegistrationImpl<P, D>(
                    path, commitHandler, this);
            commitHandlers.put(path, registration);
            LOG.trace("Registering Commit Handler {} for path: {}", commitHandler, path);
            for (final ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<P, D>>> listener : commitHandlerRegistrationListeners) {
                try {
                    listener.getInstance().onRegister(registration);
                } catch (Exception e) {
                    LOG.error("Unexpected exception in listener {} during invoking onRegister", listener.getInstance(),
                            e);
                }
            }
            return registration;
        }
    }

    @Override
    public final ListenerRegistration<DCL> registerDataChangeListener(final P path, final DCL listener) {
        synchronized (listeners) {
            final DataChangeListenerRegistration<P, D, DCL> reg = new DataChangeListenerRegistration<P, D, DCL>(path,
                    listener, AbstractDataBroker.this);
            listeners.put(path, reg);
            final D initialConfig = getDataReadRouter().readConfigurationData(path);
            final D initialOperational = getDataReadRouter().readOperationalData(path);
            final DataChangeEvent<P, D> event = createInitialListenerEvent(path, initialConfig, initialOperational);
            listener.onDataChanged(event);
            return reg;
        }
    }

    public final CompositeObjectRegistration<DataReader<P, D>> registerDataReader(final P path,
            final DataReader<P, D> reader) {

        final Registration<DataReader<P, D>> confReg = getDataReadRouter().registerConfigurationReader(path, reader);
        final Registration<DataReader<P, D>> dataReg = getDataReadRouter().registerOperationalReader(path, reader);
        return new CompositeObjectRegistration<DataReader<P, D>>(reader, Arrays.asList(confReg, dataReg));
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<P, D>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<P, D>> commitHandlerListener) {
        final ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<P, D>>> ret = this.commitHandlerRegistrationListeners
                .register(commitHandlerListener);
        return ret;
    }

    protected DataChangeEvent<P, D> createInitialListenerEvent(final P path, final D initialConfig,
            final D initialOperational) {
        InitialDataChangeEventImpl<P, D> _initialDataChangeEventImpl = new InitialDataChangeEventImpl<P, D>(
                initialConfig, initialOperational);
        return _initialDataChangeEventImpl;
    }

    protected final void removeListener(final DataChangeListenerRegistration<P, D, DCL> registration) {
        synchronized (listeners) {
            listeners.remove(registration.getPath(), registration);
        }
    }

    protected final void removeCommitHandler(final DataCommitHandlerRegistrationImpl<P, D> registration) {
        synchronized (commitHandlers) {

            commitHandlers.remove(registration.getPath(), registration);
            LOG.trace("Removing Commit Handler {} for path: {}", registration.getInstance(), registration.getPath());
            for (final ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<P, D>>> listener : commitHandlerRegistrationListeners) {
                try {
                    listener.getInstance().onUnregister(registration);
                } catch (Exception e) {
                    LOG.error("Unexpected exception in listener {} during invoking onUnregister",
                            listener.getInstance(), e);
                }
            }
        }

    }

    protected final Collection<Entry<P, DataCommitHandlerRegistrationImpl<P, D>>> getActiveCommitHandlers() {
        return commitHandlers.entries();
    }

    protected ImmutableList<ListenerStateCapture<P, D, DCL>> affectedListeners(final Set<P> paths) {

        synchronized (listeners) {
            return FluentIterable //
                    .from(listeners.asMap().entrySet()) //
                    .filter(new Predicate<Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>>>() {
                        @Override
                        public boolean apply(final Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>> it) {
                            return isAffectedBy(it.getKey(), paths);
                        }
                    }) //
                    .transform(
                            new Function<Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>>, ListenerStateCapture<P, D, DCL>>() {
                                @Override
                                public ListenerStateCapture<P, D, DCL> apply(
                                        final Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>> it) {
                                    return new ListenerStateCapture<P, D, DCL>(it.getKey(), it.getValue(),
                                            createContainsPredicate(it.getKey()));
                                }
                            }) //
                    .toList();
        }
    }

    protected ImmutableList<ListenerStateCapture<P, D, DCL>> probablyAffectedListeners(final Set<P> paths) {
        synchronized (listeners) {
            return FluentIterable //
                    .from(listeners.asMap().entrySet()) //
                    .filter(new Predicate<Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>>>() {
                        @Override
                        public boolean apply(final Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>> it) {
                            return isProbablyAffectedBy(it.getKey(), paths);
                        }
                    }) //
                    .transform(
                            new Function<Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>>, ListenerStateCapture<P, D, DCL>>() {
                                @Override
                                public ListenerStateCapture<P, D, DCL> apply(
                                        final Entry<P, Collection<DataChangeListenerRegistration<P, D, DCL>>> it) {
                                    return new ListenerStateCapture<P, D, DCL>(it.getKey(), it.getValue(),
                                            createIsContainedPredicate(it.getKey()));
                                }
                            }) //
                    .toList();
        }
    }

    protected Predicate<P> createContainsPredicate(final P key) {
        return new Predicate<P>() {
            @Override
            public boolean apply(final P other) {
                return key.contains(other);
            }
        };
    }

    protected Predicate<P> createIsContainedPredicate(final P key) {
        return new Predicate<P>() {
            @Override
            public boolean apply(final P other) {
                return other.contains(key);
            }
        };
    }

    protected boolean isAffectedBy(final P key, final Set<P> paths) {
        final Predicate<P> contains = this.createContainsPredicate(key);
        if (paths.contains(key)) {
            return true;
        }
        for (final P path : paths) {
            if (contains.apply(path)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isProbablyAffectedBy(final P key, final Set<P> paths) {
        final Predicate<P> isContained = this.createIsContainedPredicate(key);
        for (final P path : paths) {
            if (isContained.apply(path)) {
                return true;
            }
        }
        return false;
    }

    final Future<RpcResult<TransactionStatus>> commit(final AbstractDataTransaction<P, D> transaction) {
        Preconditions.checkNotNull(transaction);
        transaction.changeStatus(TransactionStatus.SUBMITED);
        final TwoPhaseCommit<P, D, DCL> task = new TwoPhaseCommit<P, D, DCL>(transaction, this);

        this.getSubmittedTransactionsCount().getAndIncrement();
        return this.getExecutor().submit(task);
    }

    private static class DataCommitHandlerRegistrationImpl<P extends Path<P>, D extends Object> //
            extends AbstractObjectRegistration<DataCommitHandler<P, D>> //
            implements DataCommitHandlerRegistration<P, D> {

        private AbstractDataBroker<P, D, ? extends Object> dataBroker;
        private final P path;

        @Override
        public P getPath() {
            return this.path;
        }

        public DataCommitHandlerRegistrationImpl(final P path, final DataCommitHandler<P, D> instance,
                final AbstractDataBroker<P, D, ? extends Object> broker) {
            super(instance);
            this.dataBroker = broker;
            this.path = path;
        }

        @Override
        protected void removeRegistration() {
            this.dataBroker.removeCommitHandler(this);
            this.dataBroker = null;
        }
    }
}

/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipChange;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntryBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Pantelis
 */
public class CarProvider implements CarService {
    private static final Logger log = LoggerFactory.getLogger(PurchaseCarProvider.class);

    private static final Logger LOG = LoggerFactory.getLogger(CarProvider.class);

    private static final String ENTITY_TYPE = "cars";
    private static final InstanceIdentifier CARS_IID = InstanceIdentifier.builder(Cars.class).build();
    private static final DataTreeIdentifier<Cars> CARS_DTID = new DataTreeIdentifier<>(
            LogicalDatastoreType.CONFIGURATION, CARS_IID);

    private final DataBroker dataProvider;
    private final DOMDataBroker domDataBroker;
    private final EntityOwnershipService ownershipService;
    private final AtomicLong succcessCounter = new AtomicLong();
    private final AtomicLong failureCounter = new AtomicLong();

    private final CarEntityOwnershipListener ownershipListener = new CarEntityOwnershipListener();
    private final AtomicBoolean registeredListener = new AtomicBoolean();

    private final Collection<ListenerRegistration<DataChangeListener>> carsDclRegistrations =
            Sets.newConcurrentHashSet();
    private final Collection<ListenerRegistration<CarDataTreeChangeListener>> carsDtclRegistrations =
            Sets.newConcurrentHashSet();

    private volatile Thread testThread;
    private volatile boolean stopThread;
    private final AtomicReference<DOMDataTreeCommitCohortRegistration<CarEntryDataTreeCommitCohort>> commitCohortReg =
            new AtomicReference<>();

    public CarProvider(DataBroker dataProvider, EntityOwnershipService ownershipService,
            DOMDataBroker domDataBroker) {
        this.dataProvider = dataProvider;
        this.ownershipService = ownershipService;
        this.domDataBroker = domDataBroker;
    }

    public void close() {
        stopThread();
        unregisterCommitCohort();
    }

    private void stopThread() {
        if(testThread != null) {
            stopThread = true;
            testThread.interrupt();
            try {
                testThread.join();
            } catch (InterruptedException e) {}
            testThread = null;
        }
    }

    @Override
    public Future<RpcResult<Void>> stressTest(StressTestInput input) {
        final int inputRate;
        final long inputCount;

        // If rate is not provided, or given as zero, then just return.
        if (input.getRate() == null || input.getRate() == 0) {
            log.info("Exiting stress test as no rate is given.");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed()
                    .withError(ErrorType.PROTOCOL, "invalid rate")
                    .build());
        } else {
            inputRate = input.getRate();
        }

        if (input.getCount() != null) {
            inputCount = input.getCount();
        } else {
            inputCount = 0;
        }

        log.info("Stress test starting : rate: {} count: {}", inputRate, inputCount);

        stopThread();
        // clear counters
        succcessCounter.set(0);
        failureCounter.set(0);

        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<Cars> carsId = InstanceIdentifier.<Cars>builder(Cars.class).build();
        tx.merge(LogicalDatastoreType.CONFIGURATION, carsId, new CarsBuilder().build());
        try {
            tx.submit().checkedGet(5, TimeUnit.SECONDS);
        } catch (TransactionCommitFailedException | TimeoutException e) {
            log.error("Put Cars failed",e);
            return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
        }

        stopThread = false;
        final long sleep = TimeUnit.NANOSECONDS.convert(1000,TimeUnit.MILLISECONDS) / inputRate;
        final Stopwatch sw = Stopwatch.createUnstarted();
        testThread = new Thread() {
            @Override
            public void run() {
                sw.start();
                AtomicLong count = new AtomicLong();
                while(!stopThread) {
                    long id = count.incrementAndGet();
                    WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
                    CarEntry car = new CarEntryBuilder().setId(new CarId("car"+id)).build();
                    tx.put(LogicalDatastoreType.CONFIGURATION,
                            InstanceIdentifier.<Cars>builder(Cars.class).child(CarEntry.class, car.getKey()).build(),
                            car);
                    CheckedFuture<Void, TransactionCommitFailedException> future =  tx.submit();
                    Futures.addCallback(future, new FutureCallback<Void>() {

                        @Override
                        public void onSuccess(final Void result) {
                            // Transaction succeeded
                            succcessCounter.getAndIncrement();
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            // Transaction failed
                            failureCounter.getAndIncrement();
                            LOG.error("Put Cars failed", t);
                        }
                    });
                    try {
                        TimeUnit.NANOSECONDS.sleep(sleep);
                    } catch (InterruptedException e) {
                        break;
                    }

                    if(count.get() % 1000 == 0) {
                        log.info("Cars created {}, time: {}",count.get(),sw.elapsed(TimeUnit.SECONDS));
                    }

                    // Check if a count is specified in input and we have created that many cars.
                    if (inputCount != 0 && count.get() >= inputCount) {
                        stopThread = true;
                    }
                }

                log.info("Stress test thread stopping after creating {} cars.", count.get());
            }
        };
        testThread.start();

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<StopStressTestOutput>> stopStressTest() {
        stopThread();
        StopStressTestOutputBuilder stopStressTestOutput;
        stopStressTestOutput = new StopStressTestOutputBuilder()
                .setSuccessCount(succcessCounter.longValue())
                .setFailureCount(failureCounter.longValue());

        StopStressTestOutput result = stopStressTestOutput.build();
        log.info("Executed Stop Stress test; No. of cars created {}; " +
                "No. of cars failed {}; ", succcessCounter, failureCounter);
        // clear counters
        succcessCounter.set(0);
        failureCounter.set(0);
        return Futures.immediateFuture(RpcResultBuilder.<StopStressTestOutput>success(result).build());
    }


    @Override
    public Future<RpcResult<Void>> registerOwnership(RegisterOwnershipInput input) {
        if(registeredListener.compareAndSet(false, true)) {
            ownershipService.registerListener(ENTITY_TYPE, ownershipListener);
        }

        Entity entity = new Entity(ENTITY_TYPE, input.getCarId());
        try {
            ownershipService.registerCandidate(entity);
        } catch (CandidateAlreadyRegisteredException e) {
            return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION,
                    "Could not register for car " + input.getCarId(), e).buildFuture();
        }

        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> unregisterOwnership(UnregisterOwnershipInput input) {
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    private static class CarEntityOwnershipListener implements EntityOwnershipListener {
        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            LOG.info("ownershipChanged: {}", ownershipChange);
        }
    }

    @Override
    public Future<RpcResult<java.lang.Void>> registerLoggingDcl() {
        LOG.info("Registering a new CarDataChangeListener");
        final ListenerRegistration carsDclRegistration = dataProvider.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, CARS_IID, new CarDataChangeListener(),
                AsyncDataBroker.DataChangeScope.SUBTREE);

        if (carsDclRegistration != null) {
            carsDclRegistrations.add(carsDclRegistration);
            return RpcResultBuilder.<Void>success().buildFuture();
        }
        return RpcResultBuilder.<Void>failed().buildFuture();
    }

    @Override
    public Future<RpcResult<java.lang.Void>> registerLoggingDtcl() {
        LOG.info("Registering a new CarDataTreeChangeListener");
        final ListenerRegistration<CarDataTreeChangeListener> carsDtclRegistration =
                dataProvider.registerDataTreeChangeListener(CARS_DTID, new CarDataTreeChangeListener());

        if (carsDtclRegistration != null) {
            carsDtclRegistrations.add(carsDtclRegistration);
            return RpcResultBuilder.<Void>success().buildFuture();
        }
        return RpcResultBuilder.<Void>failed().buildFuture();
    }

    @Override
    public Future<RpcResult<java.lang.Void>> unregisterLoggingDcls() {
        LOG.info("Unregistering the CarDataChangeListener(s)");
        synchronized (carsDclRegistrations) {
            int numListeners = 0;
            for (ListenerRegistration<DataChangeListener> carsDclRegistration : carsDclRegistrations) {
                carsDclRegistration.close();
                numListeners++;
            }
            carsDclRegistrations.clear();
            LOG.info("Unregistered {} CarDataChangeListener(s)", numListeners);
        }
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public Future<RpcResult<java.lang.Void>> unregisterLoggingDtcls() {
        LOG.info("Unregistering the CarDataTreeChangeListener(s)");
        synchronized (carsDtclRegistrations) {
            int numListeners = 0;
            for (ListenerRegistration<CarDataTreeChangeListener> carsDtclRegistration : carsDtclRegistrations) {
                carsDtclRegistration.close();
                numListeners++;
            }
            carsDtclRegistrations.clear();
            LOG.info("Unregistered {} CaraDataTreeChangeListener(s)", numListeners);
        }
        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Future<RpcResult<Void>> unregisterCommitCohort() {
        final DOMDataTreeCommitCohortRegistration<CarEntryDataTreeCommitCohort> reg = commitCohortReg.getAndSet(null);
        if (reg != null) {
            try {
                reg.close();
                LOG.info("Unregistered commit cohort");
            } catch (Exception e) {
                return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION,
                        "Error closing commit cohort registration", e).buildFuture();
            }
        }

        return RpcResultBuilder.<Void>success().buildFuture();
    }

    @Override
    public synchronized Future<RpcResult<Void>> registerCommitCohort() {
        if (commitCohortReg.get() != null) {
            return RpcResultBuilder.<Void>success().buildFuture();
        }

        final DOMDataTreeCommitCohortRegistry commitCohortRegistry = (DOMDataTreeCommitCohortRegistry)
                domDataBroker.getSupportedExtensions().get(DOMDataTreeCommitCohortRegistry.class);

        if (commitCohortRegistry == null) {
            // Shouldn't happen
            return RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION,
                    "DOMDataTreeCommitCohortRegistry not found").buildFuture();
        }

        // Note: it may look strange that we specify the CarEntry.QNAME twice in the path below. This must be done in
        // order to register the commit cohort for CarEntry instances. In the underlying data tree, a yang list is
        // represented as a MapNode with MapEntryNodes representing the child list entries. Therefore, in order to
        // address a list entry, you must specify the path argument for the MapNode and the path argument for the
        // MapEntryNode. In the path below, the first CarEntry.QNAME argument addresses the MapNode and, since we want
        // to address all list entries, the second path argument is wild-carded by specifying just the CarEntry.QNAME.
        final YangInstanceIdentifier carEntryPath = YangInstanceIdentifier.builder(
                YangInstanceIdentifier.of(Cars.QNAME)).node(CarEntry.QNAME).node(CarEntry.QNAME).build();
        commitCohortReg.set(commitCohortRegistry.registerCommitCohort(
                new org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier(
                    org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION,
                        carEntryPath), new CarEntryDataTreeCommitCohort()));

        LOG.info("Registered commit cohort");

        return RpcResultBuilder.<Void>success().buildFuture();
    }
}

/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipChange;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterCommitCohortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterCommitCohortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterCommitCohortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterLoggingDtclInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterLoggingDtclOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterLoggingDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntryBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of CarService.
 *
 * @author Thomas Pantelis
 */
@Singleton
@Component(service = { })
@SuppressFBWarnings("SLF4J_ILLEGAL_PASSED_CLASS")
public final class CarProvider implements CarService {
    private static final Logger LOG_PURCHASE_CAR = LoggerFactory.getLogger(PurchaseCarProvider.class);

    private static final Logger LOG_CAR_PROVIDER = LoggerFactory.getLogger(CarProvider.class);

    private static final String ENTITY_TYPE = "cars";
    private static final InstanceIdentifier<Cars> CARS_IID = InstanceIdentifier.builder(Cars.class).build();
    private static final DataTreeIdentifier<Cars> CARS_DTID = DataTreeIdentifier.create(
            LogicalDatastoreType.CONFIGURATION, CARS_IID);

    private final DataBroker dataProvider;
    private final DOMDataBroker domDataBroker;
    private final EntityOwnershipService ownershipService;
    private final AtomicLong succcessCounter = new AtomicLong();
    private final AtomicLong failureCounter = new AtomicLong();

    private final CarEntityOwnershipListener ownershipListener = new CarEntityOwnershipListener();
    private final AtomicBoolean registeredListener = new AtomicBoolean();

    private final Set<ListenerRegistration<?>> carsDclRegistrations = ConcurrentHashMap.newKeySet();

    private final Set<ObjectRegistration<?>> regs = new HashSet<>();
    private final Set<ListenerRegistration<CarDataTreeChangeListener>> carsDtclRegistrations =
            ConcurrentHashMap.newKeySet();

    private volatile Thread testThread;
    private volatile boolean stopThread;
    private final AtomicReference<DOMDataTreeCommitCohortRegistration<CarEntryDataTreeCommitCohort>> commitCohortReg =
            new AtomicReference<>();

    @Inject
    @Activate
    public CarProvider(@Reference final DataBroker dataProvider,
            @Reference final EntityOwnershipService ownershipService, @Reference final DOMDataBroker domDataBroker,
            @Reference final RpcProviderService rpcProviderService) {
        this.dataProvider = dataProvider;
        this.ownershipService = ownershipService;
        this.domDataBroker = domDataBroker;
        regs.add(rpcProviderService.registerRpcImplementation(CarService.class, this));
    }

    @PreDestroy
    @Deactivate
    public void close() {
        stopThread();
        closeCommitCohortRegistration();
        regs.forEach(ObjectRegistration::close);
        regs.clear();
    }

    private void stopThread() {
        if (testThread != null) {
            stopThread = true;
            testThread.interrupt();
            try {
                testThread.join();
            } catch (InterruptedException e) {
                // don't care
            }
            testThread = null;
        }
    }

    @Override
    public ListenableFuture<RpcResult<StressTestOutput>> stressTest(final StressTestInput input) {
        final int inputRate;
        final long inputCount;

        // If rate is not provided, or given as zero, then just return.
        if (input.getRate() == null || input.getRate().toJava() == 0) {
            LOG_PURCHASE_CAR.info("Exiting stress test as no rate is given.");
            return Futures.immediateFuture(RpcResultBuilder.<StressTestOutput>failed()
                    .withError(ErrorType.PROTOCOL, "invalid rate")
                    .build());
        }

        inputRate = input.getRate().toJava();
        if (input.getCount() != null) {
            inputCount = input.getCount().toJava();
        } else {
            inputCount = 0;
        }

        LOG_PURCHASE_CAR.info("Stress test starting : rate: {} count: {}", inputRate, inputCount);

        stopThread();
        // clear counters
        succcessCounter.set(0);
        failureCounter.set(0);

        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<Cars> carsId = InstanceIdentifier.create(Cars.class);
        tx.merge(LogicalDatastoreType.CONFIGURATION, carsId, new CarsBuilder().build());
        try {
            tx.commit().get(5, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG_PURCHASE_CAR.error("Put Cars failed",e);
            return Futures.immediateFuture(RpcResultBuilder.success(new StressTestOutputBuilder().build()).build());
        }

        stopThread = false;
        final long sleep = TimeUnit.NANOSECONDS.convert(1000,TimeUnit.MILLISECONDS) / inputRate;
        final Stopwatch sw = Stopwatch.createUnstarted();
        testThread = new Thread(() -> {
            sw.start();
            AtomicLong count = new AtomicLong();
            while (!stopThread) {
                long id = count.incrementAndGet();
                WriteTransaction tx1 = dataProvider.newWriteOnlyTransaction();
                CarEntry car = new CarEntryBuilder().setId(new CarId("car" + id)).build();
                tx1.put(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.<Cars>builder(Cars.class).child(CarEntry.class, car.key()).build(), car);
                tx1.commit().addCallback(new FutureCallback<CommitInfo>() {

                    @Override
                    public void onSuccess(final CommitInfo result) {
                        // Transaction succeeded
                        succcessCounter.getAndIncrement();
                    }

                    @Override
                    public void onFailure(final Throwable ex) {
                        // Transaction failed
                        failureCounter.getAndIncrement();
                        LOG_CAR_PROVIDER.error("Put Cars failed", ex);
                    }
                }, MoreExecutors.directExecutor());
                try {
                    TimeUnit.NANOSECONDS.sleep(sleep);
                } catch (InterruptedException e) {
                    break;
                }

                if (count.get() % 1000 == 0) {
                    LOG_PURCHASE_CAR.info("Cars created {}, time: {}", count.get(), sw.elapsed(TimeUnit.SECONDS));
                }

                // Check if a count is specified in input and we have created that many cars.
                if (inputCount != 0 && count.get() >= inputCount) {
                    stopThread = true;
                }
            }

            LOG_PURCHASE_CAR.info("Stress test thread stopping after creating {} cars.", count.get());
        });
        testThread.start();

        return Futures.immediateFuture(RpcResultBuilder.success(new StressTestOutputBuilder().build()).build());
    }

    @Override
    public ListenableFuture<RpcResult<StopStressTestOutput>> stopStressTest(final StopStressTestInput input) {
        stopThread();
        StopStressTestOutputBuilder stopStressTestOutput;
        stopStressTestOutput = new StopStressTestOutputBuilder()
                .setSuccessCount(Uint32.valueOf(succcessCounter.longValue()))
                .setFailureCount(Uint32.valueOf(failureCounter.longValue()));

        final StopStressTestOutput result = stopStressTestOutput.build();
        LOG_PURCHASE_CAR.info("Executed Stop Stress test; No. of cars created {}; "
                + "No. of cars failed {}; ", succcessCounter, failureCounter);
        // clear counters
        succcessCounter.set(0);
        failureCounter.set(0);
        return Futures.immediateFuture(RpcResultBuilder.<StopStressTestOutput>success(result).build());
    }


    @Override
    public ListenableFuture<RpcResult<RegisterOwnershipOutput>> registerOwnership(final RegisterOwnershipInput input) {
        if (registeredListener.compareAndSet(false, true)) {
            ownershipService.registerListener(ENTITY_TYPE, ownershipListener);
        }

        Entity entity = new Entity(ENTITY_TYPE, input.getCarId());
        try {
            ownershipService.registerCandidate(entity);
        } catch (CandidateAlreadyRegisteredException e) {
            return RpcResultBuilder.<RegisterOwnershipOutput>failed().withError(ErrorType.APPLICATION,
                    "Could not register for car " + input.getCarId(), e).buildFuture();
        }

        return RpcResultBuilder.success(new RegisterOwnershipOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UnregisterOwnershipOutput>> unregisterOwnership(
            final UnregisterOwnershipInput input) {
        return RpcResultBuilder.success(new UnregisterOwnershipOutputBuilder().build()).buildFuture();
    }

    private static class CarEntityOwnershipListener implements EntityOwnershipListener {
        @Override
        public void ownershipChanged(final EntityOwnershipChange ownershipChange) {
            LOG_CAR_PROVIDER.info("ownershipChanged: {}", ownershipChange);
        }
    }

    @Override
    public ListenableFuture<RpcResult<RegisterLoggingDtclOutput>> registerLoggingDtcl(
            final RegisterLoggingDtclInput input) {
        LOG_CAR_PROVIDER.info("Registering a new CarDataTreeChangeListener");
        final ListenerRegistration<CarDataTreeChangeListener> carsDtclRegistration =
                dataProvider.registerDataTreeChangeListener(CARS_DTID, new CarDataTreeChangeListener());

        carsDtclRegistrations.add(carsDtclRegistration);
        return RpcResultBuilder.success(new RegisterLoggingDtclOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UnregisterLoggingDtclsOutput>> unregisterLoggingDtcls(
            final UnregisterLoggingDtclsInput input) {
        LOG_CAR_PROVIDER.info("Unregistering the CarDataTreeChangeListener(s)");
        synchronized (carsDtclRegistrations) {
            int numListeners = 0;
            for (ListenerRegistration<CarDataTreeChangeListener> carsDtclRegistration : carsDtclRegistrations) {
                carsDtclRegistration.close();
                numListeners++;
            }
            carsDtclRegistrations.clear();
            LOG_CAR_PROVIDER.info("Unregistered {} CaraDataTreeChangeListener(s)", numListeners);
        }
        return RpcResultBuilder.success(new UnregisterLoggingDtclsOutputBuilder().build()).buildFuture();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<RpcResult<UnregisterCommitCohortOutput>> unregisterCommitCohort(
            final UnregisterCommitCohortInput input) {
        closeCommitCohortRegistration();

        return RpcResultBuilder.success(new UnregisterCommitCohortOutputBuilder().build()).buildFuture();
    }

    private void closeCommitCohortRegistration() {
        final DOMDataTreeCommitCohortRegistration<CarEntryDataTreeCommitCohort> reg = commitCohortReg.getAndSet(null);
        if (reg != null) {
            reg.close();
            LOG_CAR_PROVIDER.info("Unregistered commit cohort");
        }
    }

    @Override
    public synchronized ListenableFuture<RpcResult<RegisterCommitCohortOutput>> registerCommitCohort(
            final RegisterCommitCohortInput input) {
        if (commitCohortReg.get() != null) {
            return RpcResultBuilder.success(new RegisterCommitCohortOutputBuilder().build()).buildFuture();
        }

        final DOMDataTreeCommitCohortRegistry commitCohortRegistry = domDataBroker.getExtensions().getInstance(
            DOMDataTreeCommitCohortRegistry.class);

        if (commitCohortRegistry == null) {
            // Shouldn't happen
            return RpcResultBuilder.<RegisterCommitCohortOutput>failed().withError(ErrorType.APPLICATION,
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
        commitCohortReg.set(commitCohortRegistry.registerCommitCohort(new DOMDataTreeIdentifier(
            LogicalDatastoreType.CONFIGURATION, carEntryPath), new CarEntryDataTreeCommitCohort()));

        LOG_CAR_PROVIDER.info("Registered commit cohort");

        return RpcResultBuilder.success(new RegisterCommitCohortOutputBuilder().build()).buildFuture();
    }
}

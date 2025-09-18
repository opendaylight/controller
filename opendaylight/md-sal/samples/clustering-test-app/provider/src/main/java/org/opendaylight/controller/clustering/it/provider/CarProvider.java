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
import org.opendaylight.mdsal.dom.api.DOMDataBroker.CommitCohortExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterCommitCohort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterCommitCohortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterCommitCohortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterCommitCohortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterLoggingDtcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterLoggingDtclInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterLoggingDtclOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterLoggingDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.RegisterOwnershipOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StopStressTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.StressTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterCommitCohortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterLoggingDtclsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.UnregisterOwnershipOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntryBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
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
public final class CarProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CarProvider.class);

    private static final String ENTITY_TYPE = "cars";
    private static final InstanceIdentifier<Cars> CARS_IID = InstanceIdentifier.builder(Cars.class).build();
    private static final DataTreeIdentifier<Cars> CARS_DTID = DataTreeIdentifier.of(
            LogicalDatastoreType.CONFIGURATION, CARS_IID);

    private final DataBroker dataProvider;
    private final DOMDataBroker domDataBroker;
    private final EntityOwnershipService ownershipService;
    private final AtomicLong succcessCounter = new AtomicLong();
    private final AtomicLong failureCounter = new AtomicLong();

    private final EntityOwnershipListener ownershipListener = (entity, change, inJeopardy) ->
        LOG.info("ownershipChanged: entity={} change={} inJeopardy={}", entity, change, inJeopardy);

    private final AtomicBoolean registeredListener = new AtomicBoolean();
    private final AtomicReference<Registration> commitCohortReg = new AtomicReference<>();
    private final Set<ObjectRegistration<?>> carsDclRegistrations = ConcurrentHashMap.newKeySet();
    private final Set<Registration> regs = new HashSet<>();
    private final Set<Registration> carsDtclRegistrations = ConcurrentHashMap.newKeySet();

    private volatile Thread testThread;
    private volatile boolean stopThread;

    @Inject
    @Activate
    public CarProvider(@Reference final DataBroker dataProvider,
            @Reference final EntityOwnershipService ownershipService, @Reference final DOMDataBroker domDataBroker,
            @Reference final RpcProviderService rpcProviderService) {
        this.dataProvider = dataProvider;
        this.ownershipService = ownershipService;
        this.domDataBroker = domDataBroker;
        regs.add(rpcProviderService.registerRpcImplementations(
            (StressTest) this::stressTest,
            (StopStressTest) this::stopStressTest,
            (RegisterOwnership) this::registerOwnership,
            (UnregisterOwnership) this::unregisterOwnership,
            (RegisterLoggingDtcl) this::registerLoggingDtcl,
            (UnregisterLoggingDtcls) this::unregisterLoggingDtcls,
            (RegisterCommitCohort) this::registerCommitCohort,
            (UnregisterCommitCohort) this::unregisterCommitCohort));
    }

    @PreDestroy
    @Deactivate
    public void close() {
        stopThread();
        closeCommitCohortRegistration();
        regs.forEach(Registration::close);
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

    private ListenableFuture<RpcResult<StressTestOutput>> stressTest(final StressTestInput input) {
        final int inputRate;
        final long inputCount;

        // If rate is not provided, or given as zero, then just return.
        if (input.getRate() == null || input.getRate().toJava() == 0) {
            LOG.info("Exiting stress test as no rate is given.");
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

        LOG.info("Stress test starting : rate: {} count: {}", inputRate, inputCount);

        stopThread();
        // clear counters
        succcessCounter.set(0);
        failureCounter.set(0);

        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, DataObjectIdentifier.builder(Cars.class).build(),
            new CarsBuilder().build());
        try {
            tx.commit().get(5, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.error("Put Cars failed",e);
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
                        DataObjectIdentifier.builder(Cars.class).child(CarEntry.class, car.key()).build(), car);
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
                        LOG.error("Put Cars failed", ex);
                    }
                }, MoreExecutors.directExecutor());
                try {
                    TimeUnit.NANOSECONDS.sleep(sleep);
                } catch (InterruptedException e) {
                    break;
                }

                if (count.get() % 1000 == 0) {
                    LOG.info("Cars created {}, time: {}", count.get(), sw.elapsed(TimeUnit.SECONDS));
                }

                // Check if a count is specified in input and we have created that many cars.
                if (inputCount != 0 && count.get() >= inputCount) {
                    stopThread = true;
                }
            }

            LOG.info("Stress test thread stopping after creating {} cars.", count.get());
        });
        testThread.start();

        return Futures.immediateFuture(RpcResultBuilder.success(new StressTestOutputBuilder().build()).build());
    }

    private ListenableFuture<RpcResult<StopStressTestOutput>> stopStressTest(final StopStressTestInput input) {
        stopThread();
        StopStressTestOutputBuilder stopStressTestOutput;
        stopStressTestOutput = new StopStressTestOutputBuilder()
                .setSuccessCount(Uint32.valueOf(succcessCounter.longValue()))
                .setFailureCount(Uint32.valueOf(failureCounter.longValue()));

        final StopStressTestOutput result = stopStressTestOutput.build();
        LOG.info("Executed Stop Stress test; No. of cars created {}; No. of cars failed {}; ",
            succcessCounter, failureCounter);
        // clear counters
        succcessCounter.set(0);
        failureCounter.set(0);
        return Futures.immediateFuture(RpcResultBuilder.<StopStressTestOutput>success(result).build());
    }

    private ListenableFuture<RpcResult<RegisterOwnershipOutput>> registerOwnership(final RegisterOwnershipInput input) {
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

    private ListenableFuture<RpcResult<UnregisterOwnershipOutput>> unregisterOwnership(
            final UnregisterOwnershipInput input) {
        return RpcResultBuilder.success(new UnregisterOwnershipOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<RegisterLoggingDtclOutput>> registerLoggingDtcl(
            final RegisterLoggingDtclInput input) {
        LOG.info("Registering a new CarDataTreeChangeListener");
        final var reg = dataProvider.registerTreeChangeListener(CARS_DTID, new CarDataTreeChangeListener());
        carsDtclRegistrations.add(reg);
        return RpcResultBuilder.success(new RegisterLoggingDtclOutputBuilder().build()).buildFuture();
    }

    private ListenableFuture<RpcResult<UnregisterLoggingDtclsOutput>> unregisterLoggingDtcls(
            final UnregisterLoggingDtclsInput input) {
        LOG.info("Unregistering the CarDataTreeChangeListener(s)");
        synchronized (carsDtclRegistrations) {
            int numListeners = 0;
            for (var carsDtclRegistration : carsDtclRegistrations) {
                carsDtclRegistration.close();
                numListeners++;
            }
            carsDtclRegistrations.clear();
            LOG.info("Unregistered {} CaraDataTreeChangeListener(s)", numListeners);
        }
        return RpcResultBuilder.success(new UnregisterLoggingDtclsOutputBuilder().build()).buildFuture();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private ListenableFuture<RpcResult<UnregisterCommitCohortOutput>> unregisterCommitCohort(
            final UnregisterCommitCohortInput input) {
        closeCommitCohortRegistration();

        return RpcResultBuilder.success(new UnregisterCommitCohortOutputBuilder().build()).buildFuture();
    }

    private void closeCommitCohortRegistration() {
        final var reg = commitCohortReg.getAndSet(null);
        if (reg != null) {
            reg.close();
            LOG.info("Unregistered commit cohort");
        }
    }

    private synchronized ListenableFuture<RpcResult<RegisterCommitCohortOutput>> registerCommitCohort(
            final RegisterCommitCohortInput input) {
        if (commitCohortReg.get() != null) {
            return RpcResultBuilder.success(new RegisterCommitCohortOutputBuilder().build()).buildFuture();
        }

        final var commitCohortRegistry = domDataBroker.extension(CommitCohortExtension.class);
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
        commitCohortReg.set(commitCohortRegistry.registerCommitCohort(DOMDataTreeIdentifier.of(
            LogicalDatastoreType.CONFIGURATION, carEntryPath), new CarEntryDataTreeCommitCohort()));

        LOG.info("Registered commit cohort");

        return RpcResultBuilder.success(new RegisterCommitCohortOutputBuilder().build()).buildFuture();
    }
}

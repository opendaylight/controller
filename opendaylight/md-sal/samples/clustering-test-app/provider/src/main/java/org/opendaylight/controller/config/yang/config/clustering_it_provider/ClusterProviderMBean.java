/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.clustering_it_provider;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.Cars;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.CarsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.Parts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.PartsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.parts.Engine;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.parts.EngineBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.parts.engine.Cylinders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.parts.engine.CylindersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.parts.engine.cylinders.Inner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.car.entry.parts.engine.cylinders.InnerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.car.rev140818.cars.CarEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.People;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PeopleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.PersonId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.people.Person;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.sal.clustering.it.people.rev140818.people.PersonBuilder;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Pantelis
 */
public class ClusterProviderMBean implements ClusteringItProviderRuntimeMXBean{
    private static final Logger log = LoggerFactory.getLogger(ClusterProviderMBean.class);

    private final DataBroker dataProvider;

    public ClusterProviderMBean(DataBroker dataProvider) {
        super();
        this.dataProvider = dataProvider;
    }

    @Override
    public void addPerson(Integer index) {

        int i = index;
        final Person person = new PersonBuilder().setAddress(i+" Main St").setAge((long)i).setContactNo("ContactNo"+1)
                .setGender("Male").setId(new PersonId("Person"+i)).build();
        People people = new PeopleBuilder().setPerson(Arrays.asList(person)).build();

        log.info("RPC addPerson : {}", person);

        // Each entry will be identifiable by a unique key, we have to create that identifier
        final InstanceIdentifier<People> personId = InstanceIdentifier.<People>builder(People.class).build();

        // Place entry in data store tree
        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, personId, people);

        Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
          @Override
          public void onSuccess(final Void result) {
            log.info("RPC addPerson : added successfully for {}", person);
          }

          @Override
          public void onFailure(final Throwable t) {
            log.error("RPC addPerson : addition failed for {}", person, t);
          }
        });

    }

    @Override
    public void addCar(Integer index) {
        int i = index;
        final CarEntry car = new CarEntryBuilder().setCategory("Category"+1).setId(new CarId("car"+i))
                .build();
        Cars cars = new CarsBuilder().setCarEntry(Arrays.asList(car)).build();

        log.info("RPC addCar : {}", car);

        // Each entry will be identifiable by a unique key, we have to create that identifier
        final InstanceIdentifier<Cars> carId = InstanceIdentifier.<Cars>builder(Cars.class).build();

        // Place entry in data store tree
        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, carId, cars);

        Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
          @Override
          public void onSuccess(final Void result) {
            log.info("RPC addCar : added successfully for {}", car);
          }

          @Override
          public void onFailure(final Throwable t) {
            log.error("RPC addCar : failed for {}", car, t);
          }
        });

    }

    @Override
    public void carsStressTest(final Integer numCars, Integer numThreads, final Boolean useFullCars) {
        log.info("carsStressTest starting : numCars: {}, numThreads: {}, useFullCars: {}", numCars, numThreads,useFullCars);

        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<Cars> carsId = InstanceIdentifier.<Cars>builder(Cars.class).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, carsId, new CarsBuilder().build());
        try {
            tx.submit().checkedGet(5, TimeUnit.SECONDS);
        } catch (TransactionCommitFailedException | TimeoutException e) {
            log.error("Put Cars failed",e);
            return;
        }

        final DurationStatisticsTracker statsTracker = DurationStatisticsTracker.createConcurrent();
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithCapacity(numThreads);
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads));
        Stopwatch sw = Stopwatch.createStarted();
        final AtomicLong count = new AtomicLong();
        for(int i = 0; i< numThreads; i++) {
            Callable<Void> task = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Stopwatch sw = Stopwatch.createStarted();
                    ListenableFuture<Void> lastFuture = null;
                    int times = numCars / 2;
                    for(int j = 0; j < times ; j++) {
                        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
//                        WriteTransaction tx = writeOnly ? dataProvider.newWriteOnlyTransaction() :
//                            dataProvider.newReadWriteTransaction();
                        for(int i = 0; i < 2; i++) {
                            long id = count.incrementAndGet();
                            CarEntry car = useFullCars? createFullCar(id) :
                                new CarEntryBuilder().setId(new CarId("car"+id)).build();
                            tx.merge(LogicalDatastoreType.CONFIGURATION,
                                    InstanceIdentifier.<Cars>builder(Cars.class).child(CarEntry.class, car.getKey()).build(),
                                    car);
                        }

                        lastFuture = tx.submit();
                    }

                    try {
                        lastFuture.get();
                        //Futures.allAsList(futures).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Cars test failed",e);
                    }

                    sw.stop();
                    statsTracker.addDuration(sw.elapsed(TimeUnit.NANOSECONDS));
                    return null;
                }
            };

            futures.add(executor.submit(task));
        }

        try {
            Futures.allAsList(futures).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Cars test failed",e);
        }

        sw.stop();

        executor.shutdownNow();
        log.info("carsStressTest ending: elapsed time: {}, ave: {}, shortest: {}, longest: {}",sw.toString(),statsTracker.getDisplayableAverageDuration(),
                statsTracker.getDisplayableShortestDuration(),statsTracker.getDisplayableLongestDuration());

        try {
            int nCars = dataProvider.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, carsId).checkedGet().get().getCarEntry().size();
            log.info("Num cars created: {}",nCars);
        } catch (ReadFailedException e) {
            log.error("Error reading cars",e);
        }
    }

    private CarEntry createFullCar(long id) {
        List<Cylinders> cylinders = new ArrayList<>(4);
        for(int i = 1;i <= 4; i++) {
            Inner inner = new InnerBuilder().setInnerLeaf(Long.valueOf(i)).build();
            cylinders.add(new CylindersBuilder().setNum(Integer.valueOf(i)).setInner(inner ).build());
        }

        Engine engine = new EngineBuilder().setCylinders(cylinders).build();
        Parts parts = new PartsBuilder().setEngine(engine ).build();
        return new CarEntryBuilder().setId(new CarId("car"+id)).setModel("Optima").
                setManufacturer("Kia").setCategory("sedan").setYear(2012L).setParts(parts).build();
    }

    @Override
    public void chainedSinglePutTest(Integer numCars, Boolean writeOnly) {
        log.info("chainedSinglePutTest starting : numCars: {}, writeOnly: {}", numCars, writeOnly);

        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<Cars> carsId = InstanceIdentifier.<Cars>builder(Cars.class).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, carsId, new CarsBuilder().build());
        try {
            tx.submit().checkedGet(5, TimeUnit.SECONDS);
        } catch (TransactionCommitFailedException | TimeoutException e) {
            log.error("Put Cars failed",e);
            return;
        }

        TransactionChainListener listener = new TransactionChainListener() {
            @Override
            public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
            }

            @Override
            public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
                    Throwable cause) {
                log.error("Transaction chain failed",cause);
            }
        };
        BindingTransactionChain chain = dataProvider.createTransactionChain(listener);

        List<ListenableFuture<Void>> futures = Lists.newArrayListWithCapacity(numCars);
        Stopwatch total = Stopwatch.createStarted();
        ListenableFuture<Void> lastFuture = null;
        for(int i = 0; i < numCars; i++) {
            tx = writeOnly ? chain.newWriteOnlyTransaction() : chain.newReadWriteTransaction();

            CarEntry car = new CarEntryBuilder().setId(new CarId("car"+i)).build();
            tx.put(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.<Cars>builder(Cars.class).child(CarEntry.class, car.getKey()).build(),
                    car);

            lastFuture = tx.submit();
            //futures.add(tx.submit());
        }

        try {
            lastFuture.get();
            //Futures.allAsList(futures).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Cars test failed",e);
        }

        total.stop();

        log.info("chainedSinglePutTest ending: elapsed time: {}",total.toString());

        chain.close();

        try {
            int nCars = dataProvider.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, carsId).checkedGet().get().getCarEntry().size();
            log.info("Num cars created: {}",nCars);
        } catch (ReadFailedException e) {
            log.error("Error reading cars",e);
        }
    }

    @Override
    public void chainedMultiPutTest(Integer numCars, Integer times, Boolean writeOnly) {
        log.info("chainedSinglePutTest starting : numCars: {}, writeOnly: {}, times: {}", numCars, writeOnly, times);

        if(times == 0) {
            return;
        }

        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        InstanceIdentifier<Cars> carsId = InstanceIdentifier.<Cars>builder(Cars.class).build();
        tx.put(LogicalDatastoreType.CONFIGURATION, carsId, new CarsBuilder().build());
        try {
            tx.submit().checkedGet(5, TimeUnit.SECONDS);
        } catch (TransactionCommitFailedException | TimeoutException e) {
            log.error("Put Cars failed",e);
            return;
        }

        TransactionChainListener listener = new TransactionChainListener() {
            @Override
            public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
            }

            @Override
            public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
                    Throwable cause) {
                log.error("Transaction chain failed",cause);
            }
        };
        BindingTransactionChain chain = dataProvider.createTransactionChain(listener);

        Stopwatch total = Stopwatch.createStarted();
        ListenableFuture<Void> lastFuture = null;
        int n = 1;
        for(int j = 0; j < times; j++) {
            tx = writeOnly ? chain.newWriteOnlyTransaction() : chain.newReadWriteTransaction();
            for(int i = 0; i < numCars; i++) {
                CarEntry car = new CarEntryBuilder().setId(new CarId("car"+n++)).build();
                tx.put(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.<Cars>builder(Cars.class).child(CarEntry.class, car.getKey()).build(),
                        car);
            }

            lastFuture = tx.submit();
        }

        try {
            lastFuture.get();
            //Futures.allAsList(futures).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Cars test failed",e);
        }

        total.stop();

        log.info("chainedSinglePutTest ending: elapsed time: {}",total.toString());

        chain.close();

        try {
            int nCars = dataProvider.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, carsId).checkedGet().get().getCarEntry().size();
            log.info("Num cars created: {}, expected: {}",nCars,(numCars*times));
        } catch (ReadFailedException e) {
            log.error("Error reading cars",e);
        }
    }
}

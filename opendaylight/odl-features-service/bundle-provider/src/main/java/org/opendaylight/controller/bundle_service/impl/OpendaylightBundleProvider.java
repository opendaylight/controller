package org.opendaylight.controller.bundle_service.impl;
/*
 * Copyright (c) 2015 Inocybe Technologies inc, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.Bundle;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.Bundle.BundleServiceStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.BundleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.BundleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.InstallBundleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.InstallBundleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.UninstallBundleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.UninstallBundleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.UninstallBundleOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.service.bundle.common.rev150111.Location;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.service.bundle.common.rev150111.Version;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class OpendaylightBundleProvider implements BundleService, AutoCloseable, DataChangeListener{

    public static final InstanceIdentifier<Bundle> BUNDLE_CONFIG_ID =
            InstanceIdentifier.builder(Bundle.class).build();

    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightBundleProvider.class);
    private final ExecutorService executor;
    private DataBroker dataProvider;
    private final AtomicReference<Future<?>> currentInstallBundleTask = new AtomicReference<>();
    private final AtomicReference<Future<?>> currentUninstallBundleTask = new AtomicReference<>();
    protected org.osgi.framework.BundleContext bundleContext;
    //TODO remove suppress warning
    @SuppressWarnings("unused")
    private NotificationProviderService notificationProvider;
    final BindingAwareBroker.RpcRegistration<BundleService> rpcRegistration;

    public OpendaylightBundleProvider(DataBroker dataProvider, RpcProviderRegistry rpcRegistry){
        super();
        bundleContext = FrameworkUtil.getBundle(OpendaylightBundleProvider.class).getBundleContext();
        this.dataProvider = dataProvider;
        executor = Executors.newScheduledThreadPool(1);
        if (rpcRegistry != null){
            this.rpcRegistration = rpcRegistry.addRpcImplementation(BundleService.class, this);
        }
        else {
            this.rpcRegistration = null;
        }
        if (this.dataProvider != null){
            setBundleServiceStatusAvailable(null);
////            nodesReg = dataProvider.registerDataChangeListener(
////                    LogicalDatastoreType.OPERATIONAL, BUNDLE_CONFIG_ID,
////                    new NodesListener(), DataChangeScope.SUBTREE);
        }
    }

    public void setNotificationProvider(NotificationProviderService salService) {
        this.notificationProvider = salService;
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        executor.shutdown();

        if (dataProvider != null) {
            WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.OPERATIONAL,BUNDLE_CONFIG_ID);
            Futures.addCallback( tx.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess( final Void result ) {
                    LOG.debug( "Delete FeaturesServiceImpl commit result: " + result );
                }

                @Override
                public void onFailure( final Throwable t ) {
                    LOG.error( "Delete of FeaturesServiceImpl failed", t );
                }
            } );
        }
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        System.out.println("Data change event triggered on the FeaturesServiceImpl");
        DataObject dataObject = change.getUpdatedSubtree();
        if( dataObject instanceof BundleService )
        {
            //TODO remove suppress warnings
            @SuppressWarnings("unused")
            BundleService bundleService = (BundleService) dataObject;
//            Long temperature = cup.getCupTemperatureFactor();
//            if( temperature != null )
//            {
//                System.out.println("Cup temperature (longValue): "+temperature.longValue());
//                cupTemperatureFactor.set( temperature );
//            }
        }
    }

    public void setDataProvider(DataBroker dataProvider){
        this.dataProvider = dataProvider;
    }

    // We want this data in the Operational Tree and get the data with Get
    private Bundle buildBundleService(final BundleServiceStatus status) {
        // We get the bundle list from bundleContext
        org.osgi.framework.Bundle[] bundles = bundleContext
                .getBundles();

        // We create the bundle type defined in yang
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.bundle.Bundle> list =
                new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.bundle.Bundle>();

        // We convert the org.osgi.framework.Bundle to a yang bundle
        for (org.osgi.framework.Bundle b: bundles){
            list.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.bundle.BundleBuilder()
                         .setLocation(new Location(b.getLocation()))
                         .setVersion(new Version(b.getVersion().toString()))
                         .build());
        }
        return new BundleBuilder()
                  .setBundleServiceStatus(BundleServiceStatus.Available)
                  .setBundle(list).build();
    }

    public void setBundleServiceStatusAvailable(final Function<Boolean,Void> resultCallback){
        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, BUNDLE_CONFIG_ID,
                buildBundleService(BundleServiceStatus.Available));

        Futures.addCallback( tx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess( final Void result ) {
                notifyCallback( true );
            }

            @Override
            public void onFailure( final Throwable t ) {
                LOG.error( "Failed to update bundle service status", t );

                notifyCallback( false );
            }

            void notifyCallback( final boolean result ) {
                if( resultCallback != null ) {
                    resultCallback.apply( result );
                }
            }
        } );
    }

    private void checkStatusAndInstallBundle(final InstallBundleInput input,
                                             final SettableFuture<RpcResult<InstallBundleOutput>> futureResult,
                                             final int tries){
        final ReadWriteTransaction tx = dataProvider.newReadWriteTransaction();
        ListenableFuture<Optional<Bundle>> readFuture =
                tx.read( LogicalDatastoreType.OPERATIONAL, BUNDLE_CONFIG_ID);

        final ListenableFuture<Void> commitFuture = Futures.transform(
                readFuture, new AsyncFunction<Optional<Bundle>, Void>() {

                    @Override
                    public ListenableFuture<Void> apply(
                            final Optional<Bundle> bundleData)
                                    throws Exception {

                        BundleServiceStatus bundleServiceStatus = BundleServiceStatus.Available;
                        if (bundleData.isPresent()) {
                            bundleServiceStatus = bundleData.get()
                                    .getBundleServiceStatus();
                        }

                        LOG.debug("Read bundleServiceStatus status: {}", bundleServiceStatus);

                        if (bundleServiceStatus == BundleServiceStatus.Available) {

                            LOG.debug("Setting bundle service to unavailable (in use)");

                            tx.put(LogicalDatastoreType.OPERATIONAL, BUNDLE_CONFIG_ID,
                                    buildBundleService(BundleServiceStatus.Unavailable));
                            return tx.submit();
                        }

                        LOG.debug("Something went wrong while installing bundle: " + input.getLocation());

                        return Futures
                                .immediateFailedCheckedFuture(new TransactionCommitFailedException(""));
                    }
                });

        Futures.addCallback(commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                // OK to install bundle
                currentInstallBundleTask.set(executor.submit(new InstallBundlesTask(input,
                        futureResult, bundleContext)));
            }

            @Override
            public void onFailure(final Throwable ex) {
                if (ex instanceof OptimisticLockFailedException) {

                    // Another thread is likely trying to install a bundle
                    // simultaneously and updated the
                    // status before us. Try reading the status again - if
                    // another install is
                    // now in progress, we should get BundlesService.available and fail.

                    if ((tries - 1) > 0) {
                        LOG.debug("Got OptimisticLockFailedException - trying again");

                        checkStatusAndInstallBundle(input, futureResult, tries - 1);
                    } else {
                        futureResult.set(RpcResultBuilder
                                .<InstallBundleOutput> failed()
                                .withError(ErrorType.APPLICATION,
                                ex.getMessage()).build());
                        }

                    } else {

                    LOG.debug("Failed to commit BundlesService status", ex);

                    futureResult.set(RpcResultBuilder
                            .<InstallBundleOutput> failed()
                            .withRpcErrors(
                                    ((TransactionCommitFailedException) ex)
                                            .getErrorList()).build());
                }
            }
        });
    }

    private void checkStatusAndUninstallBundle(
            final UninstallBundleInput input,
            final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult,
            final int tries) {
        final ReadWriteTransaction tx = dataProvider.newReadWriteTransaction();
        ListenableFuture<Optional<Bundle>> readFuture = tx.read(
                LogicalDatastoreType.OPERATIONAL, BUNDLE_CONFIG_ID);

        final ListenableFuture<Void> commitFuture = Futures.transform(
                readFuture, new AsyncFunction<Optional<Bundle>, Void>() {

                    @Override
                    public ListenableFuture<Void> apply(
                            final Optional<Bundle> bundleData)
                            throws Exception {

                        BundleServiceStatus bundleServiceStatus = BundleServiceStatus.Available;
                        if (bundleData.isPresent()) {
                            bundleServiceStatus = bundleData.get()
                                    .getBundleServiceStatus();
                        }

                        LOG.debug("Read bundleServiceStatus status: {}",
                                bundleServiceStatus);

                        if (bundleServiceStatus == BundleServiceStatus.Available) {

                            LOG.debug("Setting bundle service to unavailable (in use)");

                            tx.put(LogicalDatastoreType.OPERATIONAL,
                                    BUNDLE_CONFIG_ID,
                                    buildBundleService(BundleServiceStatus.Unavailable));
                            return tx.submit();
                        }

                        LOG.debug("Something went wrong while uninstalling bundle: "
                                + input.getLocation());

                        return Futures
                                .immediateFailedCheckedFuture(new TransactionCommitFailedException(
                                        ""));
                    }
                });

        Futures.addCallback(commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                // OK to install bundle
                currentUninstallBundleTask.set(executor
                        .submit(new UninstallBundlesTask(input, futureResult)));
            }

            @Override
            public void onFailure(final Throwable ex) {
                if (ex instanceof OptimisticLockFailedException) {

                    // Another thread is likely trying to install a bundle
                    // simultaneously and updated the
                    // status before us. Try reading the status again - if
                    // another install is
                    // now in progress, we should get BundlesService.available
                    // and fail.

                    if ((tries - 1) > 0) {
                        LOG.debug("Got OptimisticLockFailedException - trying again");

                        checkStatusAndUninstallBundle(input, futureResult,
                                tries - 1);
                    } else {
                        futureResult.set(RpcResultBuilder
                                .<UninstallBundleOutput> failed()
                                .withError(ErrorType.APPLICATION,
                                        ex.getMessage()).build());
                    }

                } else {

                    LOG.debug("Failed to commit BundlesService status", ex);

                    futureResult.set(RpcResultBuilder
                            .<UninstallBundleOutput> failed()
                            .withRpcErrors(
                                    ((TransactionCommitFailedException) ex)
                                            .getErrorList()).build());
                }
            }
        });
    }

    private class InstallBundlesTask implements Callable<Void> {

        final InstallBundleInput installRequest;
        final SettableFuture<RpcResult<InstallBundleOutput>> futureResult;
        private BundleContext context;

        public InstallBundlesTask(
                final InstallBundleInput installRequest,
                final SettableFuture<RpcResult<InstallBundleOutput>> futureResult,
                BundleContext bundleContext) {
            this.installRequest = installRequest;
            this.futureResult = futureResult;
            this.context = bundleContext;
        }

        @Override
        public Void call() {
            try {
                if (context == null) {
                    throw new Exception("BundleContext is null");
                }
                org.osgi.framework.Bundle b = context
                        .installBundle(installRequest.getLocation().getValue());
                LOG.info("Bundle " + b.getLocation() + " was installed using RESTConf.");
            } catch (BundleException be) {
                LOG.error("Error installing bundle: "
                        + installRequest.getLocation().getValue());
                LOG.error(be.getStackTrace().toString());
                setBundleServiceStatusAvailable(null);
            } catch (Exception e) {
            }

            setBundleServiceStatusAvailable(new Function<Boolean, Void>() {
                @Override
                public Void apply(final Boolean result) {

                    currentInstallBundleTask.set(null);

                    LOG.debug("Bundle Service ready");

                    futureResult.set(RpcResultBuilder
                            .<InstallBundleOutput> success().build());

                    return null;
                }
            });

            return null;
        }
    }

    private class UninstallBundlesTask implements
            Callable<UninstallBundleOutput> {

        final UninstallBundleInput uninstallRequest;
        final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult;

        public UninstallBundlesTask(
                final UninstallBundleInput uninstallRequest,
                final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult) {
            this.uninstallRequest = uninstallRequest;
            this.futureResult = futureResult;
        }

        @Override
        public UninstallBundleOutput call() {
            UninstallBundleOutput output = null;
            try {
                org.osgi.framework.Bundle b = bundleContext
                        .getBundle(uninstallRequest.getLocation().getValue());
                if (b == null) {
                    output = new UninstallBundleOutputBuilder()
                            .setFailure(true).build();
                    output = new UninstallBundleOutputBuilder().setSuccess(
                            false).build();
                    LOG.error("Bundle " + uninstallRequest.getLocation()
                            + " doesn't exist.");
                } else {
                    b.uninstall();
                    output = new UninstallBundleOutputBuilder().setFailure(
                            false).build();
                    output = new UninstallBundleOutputBuilder()
                            .setSuccess(true).build();
                }
            } catch (BundleException be) {
                LOG.error("Error uninstalling bundle: "
                        + uninstallRequest.getLocation().getValue());
                LOG.error(be.getStackTrace().toString());
                setBundleServiceStatusAvailable(null);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }

            setBundleServiceStatusAvailable(new Function<Boolean, Void>() {
                @Override
                public Void apply(final Boolean result) {

                    currentUninstallBundleTask.set(null);

                    LOG.debug("Bundle Service ready");

                    futureResult.set(RpcResultBuilder
                            .<UninstallBundleOutput> success().build());
                    return null;
                }
            });

            return output;
        }
    }

    /**
     * This is the function that is called from RPC RESTConf.
     */
    @Override
    public Future<RpcResult<InstallBundleOutput>> installBundle(
            InstallBundleInput input) {
        final SettableFuture<RpcResult<InstallBundleOutput>> futureResult = SettableFuture
                .create();
        checkStatusAndInstallBundle(input, futureResult, 2);
        return futureResult;
    }

    @Override
    public Future<RpcResult<UninstallBundleOutput>> uninstallBundle(
            UninstallBundleInput input) {
        final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult = SettableFuture
                .create();
        checkStatusAndUninstallBundle(input, futureResult, 2);
        return futureResult;
    }
}

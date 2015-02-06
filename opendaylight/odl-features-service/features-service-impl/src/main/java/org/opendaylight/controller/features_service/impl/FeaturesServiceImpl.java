package org.opendaylight.controller.features_service.impl;
/*
 * Copyright (c) 2015 Inocybe Technologies inc, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.Features;
import org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.Features.FeaturesServiceStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.FeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.FeaturesService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.InstallFeatureInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.UninstallFeatureInput;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class FeaturesServiceImpl implements org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.FeaturesService,
                                            AutoCloseable, DataChangeListener{

    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.Features>
                         FEATURE_SERVICE_IID = InstanceIdentifier.builder(org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.Features.class).build();
    private static final Logger LOG = LoggerFactory.getLogger(FeaturesServiceImpl.class);
    private final ExecutorService executor;
    private DataBroker dataProvider;
    private final AtomicReference<Future<?>> currentInstallFeaturesTask = new AtomicReference<>();
    //private BundleContext karafFeaturesServiceBundle;
    //private org.apache.karaf.features.FeaturesService karafResources;
    // TODO remove suppress warnings
    @SuppressWarnings("unused")
    private NotificationProviderService notificationProvider;

    public FeaturesServiceImpl(){
        //karafFeaturesServiceBundle =
        //FrameworkUtil.getBundle(org.apache.karaf.features.FeaturesService.class).getBundleContext();
        //karafResources = (org.apache.karaf.features.FeaturesService) karafFeaturesServiceBundle.getServiceReference(FeaturesService.class);
        executor = Executors.newFixedThreadPool(1);
        System.out.println("FeaturesServiceImpl");
    }

    @Override
    public void close() throws ExecutionException, InterruptedException {
        executor.shutdown();

        if (dataProvider != null) {
            WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.OPERATIONAL,FEATURE_SERVICE_IID);
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

    public void setNotificationProvider(NotificationProviderService salService) {
        this.notificationProvider = salService;
    }

    public void setDataProvider(DataBroker dataProvider){
        this.dataProvider = dataProvider;
        setFeaturesServiceStatusAvailable(null);
    }

    public void setFeaturesServiceStatusAvailable(final Function<Boolean,Void> resultCallback){
        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
        tx.put( LogicalDatastoreType.OPERATIONAL,FEATURE_SERVICE_IID, buildFeatures( FeaturesServiceStatus.Available ) );

        ListenableFuture<Void> commitFuture = tx.submit();

        Futures.addCallback( commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess( final Void result ) {
                notifyCallback( true );
            }

            @Override
            public void onFailure( final Throwable t ) {
                LOG.error( "Failed to update features service status", t );

                notifyCallback( false );
            }

            void notifyCallback( final boolean result ) {
                if( resultCallback != null ) {
                    resultCallback.apply( result );
                }
            }
        } );
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.features.features.rev150111.Features buildFeatures( final FeaturesServiceStatus status ) {
        return new FeaturesBuilder().setFeaturesServiceStatus( status ).build();
    }

    /**
     * This is the function that is called
     * from RPC RESTConf.
     */
    @Override
    public Future<RpcResult<Void>> installFeature(InstallFeatureInput input) {
        final SettableFuture<RpcResult<Void>> futureResult = SettableFuture.create();
        checkStatusAndInstallFeature(input, futureResult, 2);
        return futureResult;
    }

    private void checkStatusAndInstallFeature(final InstallFeatureInput input,
                                              final SettableFuture<RpcResult<Void>> futureResult,
                                              final int tries){
        final ReadWriteTransaction tx = dataProvider.newReadWriteTransaction();
        ListenableFuture<Optional<Features>> readFuture =
                tx.read( LogicalDatastoreType.OPERATIONAL, FEATURE_SERVICE_IID);

        final ListenableFuture<Void> commitFuture = Futures.transform(
                  readFuture, new AsyncFunction<Optional<Features>, Void>() {

                    @Override
                    public ListenableFuture<Void> apply(
                        final Optional<Features> featuresData)
                           throws Exception {

                        FeaturesServiceStatus featuresServiceStatus = FeaturesServiceStatus.Available;
                        if (featuresData.isPresent()) {
                            featuresServiceStatus = featuresData.get().getFeaturesServiceStatus();
                        }

                        LOG.debug("Read featuresServiceStatus status: {}", featuresServiceStatus);

                        if (featuresServiceStatus == FeaturesServiceStatus.Available) {

                        LOG.debug("Setting features service to unavailable (in use)");

                            tx.put(LogicalDatastoreType.OPERATIONAL, FEATURE_SERVICE_IID,
                                 buildFeatures(FeaturesServiceStatus.Unavailable));
                            return tx.submit();
                        }

                        LOG.debug("Something went wrong while installing feature: " + input.getName());

                        return Futures
                                .immediateFailedCheckedFuture(new TransactionCommitFailedException(
                                     "", makeFeaturesServiceInUseError()));
                    }
               });

        Futures.addCallback(commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                // OK to install a feature
                currentInstallFeaturesTask.set(executor.submit(new InstallFeaturesTask(input,futureResult)));
            }

            @Override
            public void onFailure(final Throwable ex) {
                if (ex instanceof OptimisticLockFailedException) {

                    // Another thread is likely trying to install a feature
                    // simultaneously and updated the
                    // status before us. Try reading the status again - if
                    // another install is now in progress, we should
                    // get FeaturesService.available and fail.

                    if ((tries - 1) > 0) {
                        LOG.debug("Got OptimisticLockFailedException - trying again");
                        checkStatusAndInstallFeature(input, futureResult, tries - 1);
                    } else {
                        futureResult.set(RpcResultBuilder
                                .<Void> failed()
                                .withError(ErrorType.APPLICATION,
                                        ex.getMessage()).build());
                    }

                    } else {

                    LOG.debug("Failed to commit FeaturesService status", ex);

                    futureResult.set(RpcResultBuilder
                            .<Void> failed()
                            .withRpcErrors(
                                    ((TransactionCommitFailedException) ex)
                                    .getErrorList()).build());
                    }
            }
        });
    }

    private class InstallFeaturesTask implements Callable<Void> {

        final InstallFeatureInput installRequest;
        final SettableFuture<RpcResult<Void>> futureResult;

        public InstallFeaturesTask( final InstallFeatureInput installRequest,
                            final SettableFuture<RpcResult<Void>> futureResult) {
            this.installRequest = installRequest;
            this.futureResult = futureResult;
        }

        @Override
        public Void call() {
            try {
                //TODO replace this
                //resources.installFeature(installRequest.getName().getValue());
                System.out.println("Installing " + installRequest.getName().getValue());
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }

            setFeaturesServiceStatusAvailable( new Function<Boolean,Void>() {
                @Override
                public Void apply( final Boolean result ) {

                    currentInstallFeaturesTask.set( null );

                    LOG.debug("Cup ready");

                    futureResult.set( RpcResultBuilder.<Void>success().build() );

                    return null;
                }
            } );
           return null;
        }
    }

    @Override
    public Future<RpcResult<Void>> uninstallFeature(UninstallFeatureInput input) {
        //TODO
        System.out.println("Installing feature "+input.getName());
        return null;
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        System.out.println("Data change event triggered on the FeaturesServiceImpl");
        DataObject dataObject = change.getUpdatedSubtree();
        if( dataObject instanceof FeaturesService )
        {
            //TODO remove suppress warnings
            @SuppressWarnings("unused")
            FeaturesService featuresService = (FeaturesService) dataObject;
//            Long temperature = cup.getCupTemperatureFactor();
//            if( temperature != null )
//            {
//                System.out.println("Cup temperature (longValue): "+temperature.longValue());
//                cupTemperatureFactor.set( temperature );
//            }
        }
    }

    /**
     *
     * @return The RPC error in use.
     */
    private RpcError makeFeaturesServiceInUseError() {
        return RpcResultBuilder.newWarning( ErrorType.APPLICATION, "in-use",
                "FeaturesService is busy (in-use)", null, null, null );
    }
}
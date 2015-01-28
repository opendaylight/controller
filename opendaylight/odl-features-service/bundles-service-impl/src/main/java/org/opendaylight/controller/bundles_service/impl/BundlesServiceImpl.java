package org.opendaylight.controller.bundles_service.impl;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.Bundle;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.Bundle.BundleServiceStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.BundleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.BundlesService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.InstallBundleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.InstallBundleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.ListBundlesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.UninstallBundleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.UninstallBundleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundles.rev150111.UninstallBundleOutputBuilder;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class BundlesServiceImpl implements BundlesService, AutoCloseable, DataChangeListener{

    public static final InstanceIdentifier<Bundle> BUNDLE_IID = 
            InstanceIdentifier.builder(Bundle.class).build();

    private static final Logger LOG = LoggerFactory.getLogger(BundlesServiceImpl.class);
    private final ExecutorService executor;
    private DataBroker dataProvider;
    private final AtomicReference<Future<?>> currentInstallBundleTask = new AtomicReference<>();
    private final AtomicReference<Future<?>> currentUninstallBundleTask = new AtomicReference<>();
    private final AtomicReference<Future<?>> currentListBundleTask = new AtomicReference<>();
    protected org.osgi.framework.BundleContext bundleContext;
    //TODO remove suppress warning
	@SuppressWarnings("unused")
	private NotificationProviderService notificationProvider;

    public BundlesServiceImpl(){
        executor = Executors.newFixedThreadPool(1);
        bundleContext = FrameworkUtil.getBundle(BundlesServiceImpl.class).getBundleContext();
    }

    public void setNotificationProvider(NotificationProviderService salService) {
        this.notificationProvider = salService;
    }

	@Override
    public void close() throws ExecutionException, InterruptedException {
        executor.shutdown();

        if (dataProvider != null) {
            WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
            tx.delete(LogicalDatastoreType.OPERATIONAL,BUNDLE_IID);
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
        if( dataObject instanceof BundlesService )
        {
        	//TODO remove suppress warnings
            @SuppressWarnings("unused")
			BundlesService bundleService = (BundlesService) dataObject;
//            Long temperature = cup.getCupTemperatureFactor();
//            if( temperature != null )
//            {
//                System.out.println("Cup temperature (longValue): "+temperature.longValue());
//                cupTemperatureFactor.set( temperature );
//            }
        }
    }

    public void setDataProvider(DataBroker dataProvider){
    	System.out.println("setDataProvider()");
        this.dataProvider = dataProvider;
        setBundlesServiceStatusAvailable(null);
    }

//    private Bundle buildBundlesService(final BundleServiceStatus status) {
//    	System.out.println("buildBundle()");
//        return new BundleBuilder().setBundleServiceStatus(status).build();
//    }

    private Bundle buildBundlesService(final BundleServiceStatus status) {
        System.out.println("buildBundle()");
        return new BundleBuilder().setBundleServiceStatus(status).build();
    }

    public void setBundlesServiceStatusAvailable(final Function<Boolean,Void> resultCallback){
    	System.out.println("setBundlesServiceStatusAvailable()");
        WriteTransaction tx = dataProvider.newWriteOnlyTransaction();

        System.out.println(tx.getClass().toString());
        Bundle b = buildBundlesService(BundleServiceStatus.Available);

        System.out.println("bundle service status " + b.getBundleServiceStatus());
        System.out.println("tx.put()");
        //tx.put( LogicalDatastoreType.OPERATIONAL,BUNDLE_IID, buildBundlesService(BundleServiceStatus.Available));

        System.out.println("tx.submit()");
        ListenableFuture<Void> commitFuture = tx.submit();

        System.out.println("addCallback()");
        Futures.addCallback( commitFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess( final Void result ) {
            	System.out.println("onSuccess()");
                notifyCallback( true );
            }

            @Override
            public void onFailure( final Throwable t ) {
            	System.out.println("onFailure()");
                LOG.error( "Failed to update features service status", t );

                notifyCallback( false );
            }

            void notifyCallback( final boolean result ) {
            	System.out.println("notifyCallback");
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
                tx.read( LogicalDatastoreType.OPERATIONAL, BUNDLE_IID);

		final ListenableFuture<Void> commitFuture = Futures.transform(
				readFuture, new AsyncFunction<Optional<Bundle>, Void>() {

					@Override
					public ListenableFuture<Void> apply(
							final Optional<Bundle> featuresData)
							throws Exception {

						BundleServiceStatus bundlesServiceStatus = BundleServiceStatus.Available;
						if (featuresData.isPresent()) {
							bundlesServiceStatus = featuresData.get()
									.getBundleServiceStatus();
						}

						LOG.debug("Read bundleServiceStatus status: {}", bundlesServiceStatus);

						if (bundlesServiceStatus == BundleServiceStatus.Available) {

							LOG.debug("Setting features service to unavailable (in use)");

							tx.put(LogicalDatastoreType.OPERATIONAL, BUNDLE_IID,
									buildBundlesService(BundleServiceStatus.Unavailable));
							return tx.submit();
						}

						LOG.debug("Something went wrong while installing feature: " + input.getLocation());

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

	private void checkStatusAndUninstallBundle(final UninstallBundleInput input,
			                                   final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult, 
			                                   final int tries) {
		final ReadWriteTransaction tx = dataProvider.newReadWriteTransaction();
		ListenableFuture<Optional<Bundle>> readFuture = tx.read(
				LogicalDatastoreType.OPERATIONAL, BUNDLE_IID);

		final ListenableFuture<Void> commitFuture = Futures.transform(
				readFuture, new AsyncFunction<Optional<Bundle>, Void>() {

					@Override
					public ListenableFuture<Void> apply(
							final Optional<Bundle> featuresData)
							throws Exception {

						BundleServiceStatus bundlesServiceStatus = BundleServiceStatus.Available;
						if (featuresData.isPresent()) {
							bundlesServiceStatus = featuresData.get()
									.getBundleServiceStatus();
						}

						LOG.debug("Read bundleServiceStatus status: {}",
								bundlesServiceStatus);

						if (bundlesServiceStatus == BundleServiceStatus.Available) {

							LOG.debug("Setting features service to unavailable (in use)");

							tx.put(LogicalDatastoreType.OPERATIONAL,
									BUNDLE_IID,
									buildBundlesService(BundleServiceStatus.Unavailable));
							return tx.submit();
						}

						LOG.debug("Something went wrong while uninstalling feature: "
								+ input.getLocation());

						return Futures
								.immediateFailedCheckedFuture(new TransactionCommitFailedException(""));
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

        public InstallBundlesTask( final InstallBundleInput installRequest,
                                   final SettableFuture<RpcResult<InstallBundleOutput>> futureResult,
                                   BundleContext bundleContext) {
            this.installRequest = installRequest;
            this.futureResult = futureResult;
            this.context = bundleContext;
        }

        @Override
        public Void call(){
            	try {
            		if (context == null){
            			throw new Exception("BundleContext is null");
            		}
					org.osgi.framework.Bundle b = context.installBundle(installRequest.getLocation().getValue());
				} catch (BundleException be) {
					LOG.error("Error install bundle: " + installRequest.getLocation().getValue());
					LOG.error(be.getStackTrace().toString());
					setBundlesServiceStatusAvailable(null);
				}
			      catch (Exception e) {
				     LOG.error(e.getMessage());
			      }

            	setBundlesServiceStatusAvailable( new Function<Boolean,Void>() {
                @Override
                public Void apply( final Boolean result ) {

                	currentInstallBundleTask.set( null );

                    LOG.debug("Bundle Service ready");

                    futureResult.set( RpcResultBuilder.<InstallBundleOutput>success().build() );

                    return null;
                }
            } );

            return null;
        }
    }

    private class UninstallBundlesTask implements Callable<UninstallBundleOutput> {

        final UninstallBundleInput uninstallRequest;
        final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult;

        public UninstallBundlesTask( final UninstallBundleInput uninstallRequest,
                                     final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult) {
            this.uninstallRequest = uninstallRequest;
            this.futureResult = futureResult;
        }

        @Override
        public UninstallBundleOutput call(){
        	UninstallBundleOutput output = null;
            	try {
					org.osgi.framework.Bundle b = bundleContext.getBundle(uninstallRequest.getLocation().getValue());
            		if (b == null){
            			output = new UninstallBundleOutputBuilder().setFailure(true).build();
            			output = new UninstallBundleOutputBuilder().setSuccess(false).build();
            			LOG.error("Bundle " + uninstallRequest.getLocation() + " doesn't exist.");
            		}
            		else {
            			b.uninstall();
            			output = new UninstallBundleOutputBuilder().setFailure(false).build();
            			output = new UninstallBundleOutputBuilder().setSuccess(true).build();
            		}
				} catch (BundleException be) {
					LOG.error("Error uninstalling bundle: " + uninstallRequest.getLocation().getValue());
					LOG.error(be.getStackTrace().toString());
					setBundlesServiceStatusAvailable(null);
				}
			      catch (Exception e) {
				     LOG.error(e.getMessage());
			      }

            	setBundlesServiceStatusAvailable( new Function<Boolean,Void>() {
                @Override
                public Void apply( final Boolean result ) {

                	currentUninstallBundleTask.set( null );

                    LOG.debug("Bundle Service ready");

                    futureResult.set( RpcResultBuilder.<UninstallBundleOutput>success().build() );
                    return null;
                }
            } );

            return output;
        }
    }

    /**
     * This is the function that is called
     * from RPC RESTConf.
     */
    @Override
    public Future<RpcResult<InstallBundleOutput>> installBundle(InstallBundleInput input) {
        final SettableFuture<RpcResult<InstallBundleOutput>> futureResult = SettableFuture.create();
        checkStatusAndInstallBundle(input, futureResult, 2);
        return futureResult;
    }

    @Override
    public Future<RpcResult<UninstallBundleOutput>> uninstallBundle(UninstallBundleInput input) {
        final SettableFuture<RpcResult<UninstallBundleOutput>> futureResult = SettableFuture.create();
        checkStatusAndUninstallBundle(input, futureResult, 2);
        return futureResult;
    }

	@Override
	public Future<RpcResult<ListBundlesOutput>> listBundles() {
		// TODO Auto-generated method stub
		return null;
	}
}

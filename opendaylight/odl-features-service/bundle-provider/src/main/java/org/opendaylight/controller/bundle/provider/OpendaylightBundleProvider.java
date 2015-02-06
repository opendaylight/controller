package org.opendaylight.controller.bundle.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.Bundle;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.Bundle.BundleServiceStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.BundleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.BundleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.InstallBundleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.InstallBundleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.UninstallBundleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.rev150111.UninstallBundleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.service.bundle.common.rev150111.Location;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.bundle.service.bundle.common.rev150111.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;


public class OpendaylightBundleProvider implements BundleService, AutoCloseable{

    public static final InstanceIdentifier<Bundle> BUNDLE_CONFIG_ID =
            InstanceIdentifier.builder(Bundle.class).build();
    private static final Logger LOG = LoggerFactory.getLogger(OpendaylightBundleProvider.class);
    protected org.osgi.framework.BundleContext bundleContext;
    private DataBroker dataProvider;
    private final ExecutorService executor;

    public OpendaylightBundleProvider(DataBroker dataProvider, RpcProviderRegistry rpcRegistry){
        super();
        bundleContext = FrameworkUtil.getBundle(OpendaylightBundleProvider.class).getBundleContext();
        this.dataProvider = dataProvider;
        executor = Executors.newScheduledThreadPool(1);
        setBundleServiceStatusAvailable(null);
    }

    @Override
    public Future<RpcResult<InstallBundleOutput>> installBundle(
            InstallBundleInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<UninstallBundleOutput>> uninstallBundle(
            UninstallBundleInput input) {
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public void close() throws Exception {
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
}

package org.opendaylight.controller.sample.kitchen.impl;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.config.yang.config.kitchen_service.impl.KitchenServiceRuntimeMXBean;
import org.opendaylight.controller.sample.kitchen.api.EggsType;
import org.opendaylight.controller.sample.kitchen.api.KitchenService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterListener;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterOutOfBread;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestocked;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WheatBread;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class KitchenServiceImpl implements KitchenService, KitchenServiceRuntimeMXBean, ToasterListener {

    private static final Logger log = LoggerFactory.getLogger( KitchenServiceImpl.class );

    private final ToasterService toaster;

    private final ListeningExecutorService executor =
                                   MoreExecutors.listeningDecorator( Executors.newCachedThreadPool() );

    private volatile boolean toasterOutOfBread;

    public KitchenServiceImpl(ToasterService toaster) {
        this.toaster = toaster;
    }

    @Override
    public Future<RpcResult<Void>> makeBreakfast( EggsType eggsType, Class<? extends ToastType> toastType,
                                                  int toastDoneness ) {

        // Call makeToast and use JdkFutureAdapters to convert the Future to a ListenableFuture,
        // The OpendaylightToaster impl already returns a ListenableFuture so the conversion is
        // actually a no-op.

        ListenableFuture<RpcResult<Void>> makeToastFuture = JdkFutureAdapters.listenInPoolThread(
                makeToast( toastType, toastDoneness ), executor );

        ListenableFuture<RpcResult<Void>> makeEggsFuture = makeEggs( eggsType );

        // Combine the 2 ListenableFutures into 1 containing a list of RpcResults.

        ListenableFuture<List<RpcResult<Void>>> combinedFutures =
                Futures.allAsList( ImmutableList.of( makeToastFuture, makeEggsFuture ) );

        // Then transform the RpcResults into 1.

        return Futures.transform( combinedFutures,
            new AsyncFunction<List<RpcResult<Void>>,RpcResult<Void>>() {
                @Override
                public ListenableFuture<RpcResult<Void>> apply( List<RpcResult<Void>> results )
                                                                                 throws Exception {
                    boolean atLeastOneSucceeded = false;
                    Builder<RpcError> errorList = ImmutableList.builder();
                    for( RpcResult<Void> result: results ) {
                        if( result.isSuccessful() ) {
                            atLeastOneSucceeded = true;
                        }

                        if( result.getErrors() != null ) {
                            errorList.addAll( result.getErrors() );
                        }
                    }

                    return Futures.immediateFuture(
                              RpcResultBuilder.<Void> status( atLeastOneSucceeded )
                                              .withRpcErrors( errorList.build() ).build() );
                }
        } );
    }

    private ListenableFuture<RpcResult<Void>> makeEggs( EggsType eggsType ) {

        return executor.submit( new Callable<RpcResult<Void>>() {

            @Override
            public RpcResult<Void> call() throws Exception {

                // We don't actually do anything here - just return a successful result.
                return RpcResultBuilder.<Void> success().build();
            }
        } );
    }

    private Future<RpcResult<Void>> makeToast( Class<? extends ToastType> toastType,
                                               int toastDoneness ) {

        if( toasterOutOfBread )
        {
            log.info( "We're out of toast but we can make eggs" );
            return Futures.immediateFuture( RpcResultBuilder.<Void> success()
                     .withWarning( ErrorType.APPLICATION, "partial-operation",
                                      "Toaster is out of bread but we can make you eggs" ).build() );
        }

        // Access the ToasterService to make the toast.

        MakeToastInput toastInput = new MakeToastInputBuilder()
            .setToasterDoneness( (long) toastDoneness )
            .setToasterToastType( toastType )
            .build();

        return toaster.makeToast( toastInput );
    }

    @Override
    public Boolean makeScrambledWithWheat() {
        try {
            // This call has to block since we must return a result to the JMX client.
            RpcResult<Void> result = makeBreakfast( EggsType.SCRAMBLED, WheatBread.class, 2 ).get();
            if( result.isSuccessful() ) {
                log.info( "makeBreakfast succeeded" );
            } else {
                log.warn( "makeBreakfast failed: " + result.getErrors() );
            }

            return result.isSuccessful();

        } catch( InterruptedException | ExecutionException e ) {
            log.warn( "An error occurred while maing breakfast: " + e );
        }

        return Boolean.FALSE;
    }

    /**
     * Implemented from the ToasterListener interface.
     */
    @Override
    public void onToasterOutOfBread( ToasterOutOfBread notification ) {
        log.info( "ToasterOutOfBread notification" );
        toasterOutOfBread = true;
    }

    /**
     * Implemented from the ToasterListener interface.
     */
    @Override
    public void onToasterRestocked( ToasterRestocked notification ) {
        log.info( "ToasterRestocked notification - amountOfBread: " + notification.getAmountOfBread() );
        toasterOutOfBread = false;
    }
}

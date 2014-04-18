package org.opendaylight.controller.sample.kitchen.impl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.config.yang.config.kitchen_service.impl.KitchenServiceRuntimeMXBean;
import org.opendaylight.controller.sample.kitchen.api.EggsType;
import org.opendaylight.controller.sample.kitchen.api.KitchenService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterListener;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterOutOfBread;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterRestocked;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WheatBread;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KitchenServiceImpl implements KitchenService, KitchenServiceRuntimeMXBean, ToasterListener {

    private static final Logger log = LoggerFactory.getLogger( KitchenServiceImpl.class );

    private final ToasterService toaster;

    private volatile boolean toasterOutOfBread;

    public KitchenServiceImpl(ToasterService toaster) {
        this.toaster = toaster;
    }

    @Override
    public boolean makeBreakfast( EggsType eggs, Class<? extends ToastType> toast, int toastDoneness ) {

        if( toasterOutOfBread )
        {
            log.info( "We're out of toast but we can make eggs" );
            return true;
        }

        // Access the ToasterService to make the toast.
        // We don't actually make the eggs for this example - sorry.
        MakeToastInputBuilder toastInput = new MakeToastInputBuilder();
        toastInput.setToasterDoneness( (long) toastDoneness);
        toastInput.setToasterToastType( toast );

        try {
            RpcResult<Void> result = toaster.makeToast( toastInput.build() ).get();

            if( result.isSuccessful() ) {
                log.info( "makeToast succeeded" );
            } else {
                log.warn( "makeToast failed: " + result.getErrors() );
            }

            return result.isSuccessful();
        } catch( InterruptedException | ExecutionException e ) {
            log.warn( "Error occurred during toast creation" );
        }
        return false;
    }

    @Override
    public Boolean makeScrambledWithWheat() {
        return makeBreakfast( EggsType.SCRAMBLED, WheatBread.class, 2 );
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

package org.opendaylight.controller.sample.kitchen.api;

import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;

public interface KitchenService {
    boolean makeBreakfast( EggsType eggs, Class<? extends ToastType> toast, int toastDoneness );
}

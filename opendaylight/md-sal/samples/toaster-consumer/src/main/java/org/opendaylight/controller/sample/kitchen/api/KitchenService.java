package org.opendaylight.controller.sample.kitchen.api;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;
import org.opendaylight.yangtools.yang.common.RpcResult;

public interface KitchenService {
    Future<RpcResult<Void>> makeBreakfast( EggsType eggs, Class<? extends ToastType> toast,
                                           int toastDoneness );
}

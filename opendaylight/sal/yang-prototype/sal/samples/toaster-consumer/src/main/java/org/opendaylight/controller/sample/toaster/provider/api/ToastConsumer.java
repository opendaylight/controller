package org.opendaylight.controller.sample.toaster.provider.api;

import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToastType;

public interface ToastConsumer {
	
	boolean createRandomToast();
	
	boolean createToast(Class<? extends ToastType> type,int doneness);

}

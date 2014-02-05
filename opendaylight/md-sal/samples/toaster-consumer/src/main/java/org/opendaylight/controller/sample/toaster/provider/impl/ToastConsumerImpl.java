/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider.impl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.config.yang.config.toaster_consumer.impl.ToasterConsumerRuntimeMXBean;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sample.toaster.provider.api.ToastConsumer;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToastConsumerImpl implements
        ToastConsumer,
        NotificationListener<ToastDone>,ToasterConsumerRuntimeMXBean {

    private static final Logger log = LoggerFactory.getLogger(ToastConsumerImpl.class);

    private ToasterService toaster;

    public ToastConsumerImpl(ToasterService toaster) {
        this.toaster = toaster;
    }

    @Override
    public boolean createToast(Class<? extends ToastType> type, int doneness) {
        MakeToastInputBuilder toastInput = new MakeToastInputBuilder();
        toastInput.setToasterDoneness((long) doneness);
        toastInput.setToasterToastType(type);

        try {
            RpcResult<Void> result = toaster.makeToast(toastInput.build()).get();

            if (result.isSuccessful()) {
                log.trace("Toast was successfully finished");
            } else {
                log.warn("Toast was not successfully finished");
            }
            return result.isSuccessful();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Error occurred during toast creation");
        }
        return false;

    }

    @Override
    public void onNotification(ToastDone notification) {
        log.trace("ToastDone Notification Received: {} ",notification.getToastStatus());
    }

    @Override
    public Boolean makeHashBrownToast(Integer doneness) {
        return createToast(HashBrown.class, doneness);
    }
}

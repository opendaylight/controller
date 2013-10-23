/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider.impl;

import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.common.GlobalDataStore;
import org.opendaylight.controller.sample.toaster.provider.api.ToastConsumer;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastDone;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterData;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToasterService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToastConsumerImpl extends AbstractBindingAwareConsumer implements BundleActivator, BindingAwareConsumer, ToastConsumer,
        NotificationListener<ToastDone> {

    private static final Logger log = LoggerFactory.getLogger(ToastConsumerImpl.class);

    private ToasterService toaster;

    private ConsumerContext session;

    @Override
    public boolean createToast(Class<? extends ToastType> type, int doneness) {
        MakeToastInputBuilder toastInput = new MakeToastInputBuilder();
        toastInput.setToasterDoneness((long) doneness);
        toastInput.setToasterToastType(type);

        try {
            RpcResult<Void> result = getToastService().makeToast(toastInput.build()).get();

            if (result.isSuccessful()) {
                log.info("Toast was successfuly finished");
            } else {
                log.info("Toast was not successfuly finished");
            }
            return result.isSuccessful();
        } catch (InterruptedException | ExecutionException e) {
            log.info("Error occured during toast creation");
        }
        return false;

    }
    
    @Override
    @Deprecated
    protected void startImpl(BundleContext context) {
        context.registerService(ToastConsumer.class, this, new Hashtable<String,String>());
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        this.session = session;
        NotificationService notificationService = session.getSALService(NotificationService.class);
        notificationService.addNotificationListener(ToastDone.class, this);
    }

    @Override
    public void onNotification(ToastDone notification) {
        log.info("ToastDone Notification Received: {} ",notification.getToastStatus());

    }

    private ToasterService getToastService() {
        if (toaster == null) {
            toaster = session.getRpcService(ToasterService.class);
        }
        return toaster;
    }

}

/*
 * Copyright (c) 2013 Ericsson , Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FRMConsumerImpl extends AbstractBindingAwareProvider implements CommandProvider {
    protected static final Logger logger = LoggerFactory.getLogger(FRMConsumerImpl.class);
    private static ProviderContext p_session;
    private static DataBrokerService dataBrokerService;
    private static NotificationService notificationService;
    private FlowConsumerImpl flowImplRef;
    private GroupConsumerImpl groupImplRef;
    private static DataProviderService dataProviderService;

	private static IClusterContainerServices clusterContainerService = null;
	private static IContainer container;
	
	@Override
    public void onSessionInitiated(ProviderContext session) {

        FRMConsumerImpl.p_session = session;

        if (!getDependentModule()) {
            logger.error("Unable to fetch handlers for dependent modules");
            System.out.println("Unable to fetch handlers for dependent modules");
            return;
        }

        if (null != session) {
            notificationService = session.getSALService(NotificationService.class);

            if (null != notificationService) {
                dataBrokerService = session.getSALService(DataBrokerService.class);

                if (null != dataBrokerService) {
                    dataProviderService = session.getSALService(DataProviderService.class);

                    if (null != dataProviderService) {
                        flowImplRef = new FlowConsumerImpl();
                        // groupImplRef = new GroupConsumerImpl();
                        registerWithOSGIConsole();
                    } else {
                        logger.error("Data Provider Service is down or NULL. "
                                + "Accessing data from configuration data store will not be possible");
                        System.out.println("Data Broker Service is down or NULL.");
                    }

                } else {
                    logger.error("Data Broker Service is down or NULL.");
                    System.out.println("Data Broker Service is down or NULL.");
                }
            } else {
                logger.error("Notification Service is down or NULL.");
                System.out.println("Notification Service is down or NULL.");
            }
        } else {
            logger.error("Consumer session is NULL. Please check if provider is registered");
            System.out.println("Consumer session is NULL. Please check if provider is registered");
        }

    }

    public static IClusterContainerServices getClusterContainerService() {
        return clusterContainerService;
    }

    public static void setClusterContainerService(IClusterContainerServices clusterContainerService) {
        FRMConsumerImpl.clusterContainerService = clusterContainerService;
    }

    public static IContainer getContainer() {
        return container;
    }

    public static void setContainer(IContainer container) {
        FRMConsumerImpl.container = container;
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this, null);
    }
	
	private boolean getDependentModule() {
	    do {
        clusterContainerService = (IClusterContainerServices) ServiceHelper.getGlobalInstance(IClusterContainerServices.class, this);
        try {
            Thread.sleep(4);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	    } while(clusterContainerService == null);
	    
	    do {
	        
	    
        container = (IContainer) ServiceHelper.getGlobalInstance(IContainer.class, this);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	    } while (container == null);
	    
	   
        return true;
	}

	

    public static DataProviderService getDataProviderService() {
        return dataProviderService;
    }

    public FlowConsumerImpl getFlowImplRef() {
        return flowImplRef;
    }

    public GroupConsumerImpl getGroupImplRef() {
	return groupImplRef;
    }
		

    public static ProviderContext getProviderSession() {
        return p_session;
    }

    public static NotificationService getNotificationService() {
        return notificationService;
    }

    public static DataBrokerService getDataBrokerService() {
        return dataBrokerService;
    }

    /*
     * OSGI COMMANDS
     */
    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        return help.toString();
    }

}

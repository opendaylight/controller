
/*
 * Copyright (c) 2013 Ericsson , Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager_mdsal.consumer.impl;


import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FRMConsumerImpl extends AbstractBindingAwareProvider {
	protected static final Logger logger = LoggerFactory.getLogger(FRMConsumerImpl.class);
	private static ProviderContext p_session;
    private static DataBrokerService dataBrokerService; 	 
    private static NotificationService notificationService;	
    private FlowConsumerImpl flowImplRef;
	private GroupConsumerImpl groupImplRef;
	private static DataProviderService dataProviderService;  

	@Override
    public void onSessionInitiated(ProviderContext session) {
    	
        FRMConsumerImpl.p_session = session;
        
        if (null != session) {
          	notificationService = session.getSALService(NotificationService.class);
        	
        	if (null != notificationService) {
        		dataBrokerService = session.getSALService(DataBrokerService.class);
        		
        		if (null != dataBrokerService) {
        			dataProviderService = session.getSALService(DataProviderService.class);
        			
        			if (null != dataProviderService) {
        				flowImplRef = new FlowConsumerImpl();
        				groupImplRef = new GroupConsumerImpl();
        			}
        			else {
        				logger.error("Data Provider Service is down or NULL. " +
        						"Accessing data from configuration data store will not be possible");
                    	System.out.println("Data Broker Service is down or NULL.");
        			}
        				
        		}
        		else {
        			logger.error("Data Broker Service is down or NULL.");
                	System.out.println("Data Broker Service is down or NULL.");
        		}
        	}
        	else {
        		logger.error("Notification Service is down or NULL.");
            	System.out.println("Notification Service is down or NULL.");
        	}        		
        }
        else {
        	logger.error("Consumer session is NULL. Please check if provider is registered");
        	System.out.println("Consumer session is NULL. Please check if provider is registered");
        }
  
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

}
	

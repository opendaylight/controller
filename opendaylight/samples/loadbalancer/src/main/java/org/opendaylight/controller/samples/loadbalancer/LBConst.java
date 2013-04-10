/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer;

/**
 * Class defines all the constants used by load balancer service
 *
 */
public class LBConst {
    
    public static final int FORWARD_DIRECTION_LB_FLOW = 0;
    
    public static final int REVERSE_DIRECTION_LB_FLOW = 1;
    
    public static final String ROUND_ROBIN_LB_METHOD = "roundrobin";
    
    public static final String RANDOM_LB_METHOD = "random";
    
    public static final String STATUS_ACTIVE="active";
    
    public static final String STATUS_INACTIVE="inactive";
    
    public static final String STATUS_PENDING="pending";
    
    public static final String STATUS_ERROR="error";
	
}


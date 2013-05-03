/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugins.stub.internal;

import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;



/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class FlowProgrammerService implements IPluginInFlowProgrammerService
  {
    void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     * 
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     * 
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     * 
     */
    void stop() {
    }
    
    
    /**
     * Synchronously add a flow to the network node
     * 
     * @param node
     * @param flow
     */
    public Status addFlow(Node node, Flow flow){
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Synchronously modify existing flow on the switch
     * 
     * @param node
     * @param flow
     */
    public Status modifyFlow(Node node, Flow oldFlow, Flow newFlow){
        return new Status(StatusCode.SUCCESS);
    }
    /**
     * Synchronously remove the flow from the network node
     * 
     * @param node
     * @param flow
     */
    public Status removeFlow(Node node, Flow flow){
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Asynchronously add a flow to the network node
     * 
     * @param node
     * @param flow
     * @param rid
     */
    public Status addFlowAsync(Node node, Flow flow, long rid){
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Asynchronously modify existing flow on the switch
     * 
     * @param node
     * @param flow
     * @param rid
     */
    public Status modifyFlowAsync(Node node, Flow oldFlow, Flow newFlow, long rid){
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Asynchronously remove the flow from the network node
     * 
     * @param node
     * @param flow
     * @param rid
     */
    public Status removeFlowAsync(Node node, Flow flow, long rid){
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Remove all flows present on the network node
     * 
     * @param node
     */
    public Status removeAllFlows(Node node){
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Send Barrier message synchronously. The caller will be blocked until the
     * Barrier reply arrives.
     * 
     * @param node
     */
    public Status syncSendBarrierMessage(Node node){
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Send Barrier message asynchronously. The caller is not blocked.
     * 
     * @param node
     */
    public Status asyncSendBarrierMessage(Node node){
        return new Status(StatusCode.SUCCESS);
    }
  }
/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sample.pingdiscovery;

/**
 * Abstract the ability to mount a yang module onto a node with a given ID.
 * @author Devin Avery
 * @author Greg Hall
 */
public interface DeviceMountHandler {

    void mountIcmpDataNode( String nodeId );

}

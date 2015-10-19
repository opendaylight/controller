/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.topology;

import akka.actor.TypedActor.Receiver;
import com.google.common.annotations.Beta;

/**
 * Node manager that handles communication between node managers and delegates calls to the customizable NodeManagerCallback
 */
@Beta
public interface NodeManager extends InitialStateProvider, NodeListener, Receiver, RemoteNodeListener {

}

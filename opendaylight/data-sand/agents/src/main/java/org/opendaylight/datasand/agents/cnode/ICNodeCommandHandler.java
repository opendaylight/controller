/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cnode;

import org.opendaylight.datasand.network.NetworkID;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public interface ICNodeCommandHandler<DataType,DataTypeElement> {
    public void handle(CNodeCommand<DataTypeElement> cNodeCommand, boolean isUnreachable,NetworkID source,NetworkID sourceForUnreachable,CPeerEntry<DataType> peerEntry,CNode<DataType,DataTypeElement> node);
}

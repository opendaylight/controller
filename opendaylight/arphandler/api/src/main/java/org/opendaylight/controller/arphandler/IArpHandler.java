
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.arphandler;

import org.opendaylight.controller.hosttracker.IfHostListener;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.hostAware.IHostFinder;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.switchmanager.ISwitchManager;


/**
 * Container Manager interface
 *
 *
 */
public interface IArpHandler extends IHostFinder, IListenDataPacket {
    
    public void setHostListener(IfHostListener s);

    public void unsetHostListener(IfHostListener s);

    public void setDataPacketService(IDataPacketService s);

    public void unsetDataPacketService(IDataPacketService s);
    
    public void setSwitchManager(ISwitchManager s);

    public void unsetSwitchManager(ISwitchManager s);
    
    public IfIptoHost getHostTracker();

    public void setHostTracker(IfIptoHost hostTracker);

    public void unsetHostTracker(IfIptoHost s);
    
    public byte[] getControllerMAC();
}

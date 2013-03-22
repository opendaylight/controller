
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import org.openflow.protocol.OFError;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFError.OFHelloFailedCode;
import org.openflow.protocol.OFError.OFPortModFailedCode;
import org.openflow.protocol.OFError.OFQueueOpFailedCode;

public abstract class Utils {
    public static String getOFErrorString(OFError error) {
        OFErrorType et = OFErrorType.values()[0xffff & error.getErrorType()];
        String errorStr = "Error : " + et.toString();
        switch (et) {
        case OFPET_HELLO_FAILED:
            OFHelloFailedCode hfc = OFHelloFailedCode.values()[0xffff & error
                    .getErrorCode()];
            errorStr += " " + hfc.toString();
            break;
        case OFPET_BAD_REQUEST:
            OFBadRequestCode brc = OFBadRequestCode.values()[0xffff & error
                    .getErrorCode()];
            errorStr += " " + brc.toString();
            break;
        case OFPET_BAD_ACTION:
            OFBadActionCode bac = OFBadActionCode.values()[0xffff & error
                    .getErrorCode()];
            errorStr += " " + bac.toString();
            break;
        case OFPET_FLOW_MOD_FAILED:
            OFFlowModFailedCode fmfc = OFFlowModFailedCode.values()[0xffff & error
                    .getErrorCode()];
            errorStr += " " + fmfc.toString();
            break;
        case OFPET_PORT_MOD_FAILED:
            OFPortModFailedCode pmfc = OFPortModFailedCode.values()[0xffff & error
                    .getErrorCode()];
            errorStr += " " + pmfc.toString();
            break;
        case OFPET_QUEUE_OP_FAILED:
            OFQueueOpFailedCode qofc = OFQueueOpFailedCode.values()[0xffff & error
                    .getErrorCode()];
            errorStr += " " + qofc.toString();
            break;
        default:
            break;
        }
        return errorStr;
    }
}

/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

import org.opendaylight.controller.subchannel.generic.api.procedure.client.ClientIdentify;

/**
 * Created by HanJie on 2017/2/15.
 *
 * @author Han Jie
 */
public class SliceMessageTimeOut<T> extends AbstractSliceMessageTimeOut<T> {
    public SliceMessageTimeOut(ClientIdentify<T> clientId, long messageId){
        super(clientId,messageId);
    }
}

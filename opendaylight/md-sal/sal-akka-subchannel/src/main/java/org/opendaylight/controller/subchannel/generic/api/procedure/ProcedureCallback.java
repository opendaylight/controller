/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.procedure;

import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public interface ProcedureCallback<T,C> {
    void sendCall(T receiver,Object message);
    void receiveCall(T sender,T receiver,byte[] bytes);
    C newTimerCall(FiniteDuration timeout, Object message);
    void stopTimerCall(C timer);
}

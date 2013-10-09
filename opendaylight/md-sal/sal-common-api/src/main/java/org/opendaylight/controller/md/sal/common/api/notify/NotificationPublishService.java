/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.notify;

import java.util.concurrent.ExecutorService;

public interface NotificationPublishService<N> {

    void publish(N notification);
    
    void publish(N notification,ExecutorService executor);
}

/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * DataBrokerTestCustomizer.
 *
 * @deprecated Please use the ConcurrentDataBrokerTestCustomizer instead of
 *             this; see AbstractDataBrokerTest for more details.
 */
@Deprecated
public class DataBrokerTestCustomizer extends AbstractDataBrokerTestCustomizer {

    @Override
    public ListeningExecutorService getCommitCoordinatorExecutor() {
        return MoreExecutors.newDirectExecutorService();
    }

}

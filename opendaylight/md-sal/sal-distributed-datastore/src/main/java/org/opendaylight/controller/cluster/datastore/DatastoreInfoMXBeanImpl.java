/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.DatastoreInfoMXBean;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;

/**
 * Implementation of DatastoreInfoMXBean.
 *
 * @author Thomas Pantelis
 */
final class DatastoreInfoMXBeanImpl extends AbstractMXBean implements DatastoreInfoMXBean {
    private final ActorUtils actorUtils;

    DatastoreInfoMXBeanImpl(final String mxBeanType, final ActorUtils actorUtils) {
        super("GeneralRuntimeInfo", mxBeanType, null);
        this.actorUtils = actorUtils;
    }

    @Override
    public double getTransactionCreationRateLimit() {
        return actorUtils.getTxCreationLimit();
    }

    @Override
    public long getAskTimeoutExceptionCount() {
        return actorUtils.getAskTimeoutExceptionCount();
    }

    @Override
    public void resetAskTimeoutExceptionCount() {
        actorUtils.resetAskTimeoutExceptionCount();
    }
}

/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;

/**
 * Implementation of DatastoreInfoMXBean.
 *
 * @author Thomas Pantelis
 */
public class DatastoreInfoMXBeanImpl extends AbstractMXBean implements DatastoreInfoMXBean {

    private final ActorContext actorContext;

    public DatastoreInfoMXBeanImpl(String mxBeanType, ActorContext actorContext) {
        super("GeneralRuntimeInfo", mxBeanType, null);
        this.actorContext = actorContext;
    }


    @Override
    public double getTransactionCreationRateLimit() {
        return actorContext.getTxCreationLimit();
    }
}

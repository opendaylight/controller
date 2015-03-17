/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import akka.actor.ActorPath;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class RegisterTreeChangeListener implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ActorPath dataTreeChangeListenerPath;
    private final YangInstanceIdentifier path;

    public RegisterTreeChangeListener(YangInstanceIdentifier path, ActorPath dataTreeChangeListenerPath) {
        this.path = Preconditions.checkNotNull(path);
        this.dataTreeChangeListenerPath = Preconditions.checkNotNull(dataTreeChangeListenerPath);
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public ActorPath getDataTreeChangeListenerPath() {
        return dataTreeChangeListenerPath;
    }
}

/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.concepts.Registration;


public interface DataProvisionService<P extends Path<P> , D> {

    public Registration<DataCommitHandler<P, D>> registerCommitHandler(P path, DataCommitHandler<P, D> commitHandler);
    
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<P, D>>> 
        registerCommitHandlerListener(RegistrationListener<DataCommitHandlerRegistration<P, D>> commitHandlerListener);

}

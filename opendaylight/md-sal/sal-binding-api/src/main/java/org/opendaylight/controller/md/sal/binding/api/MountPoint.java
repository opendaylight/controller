/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface MountPoint extends Identifiable<InstanceIdentifier<?>>{

    <T extends BindingService> Optional<T> getService(Class<T> service);

}

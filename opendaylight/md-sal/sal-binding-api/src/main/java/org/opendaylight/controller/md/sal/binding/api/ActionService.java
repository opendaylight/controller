/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Bridge to action invocation.
 *
 * @deprecated Use {@link org.opendaylight.mdsal.binding.api.ActionService} instead
 */
@Deprecated
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE", justification = "Migration")
public interface ActionService extends BindingService, org.opendaylight.mdsal.binding.api.ActionService {

}

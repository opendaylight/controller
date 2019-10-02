/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A single YANG notification.
 * @deprecated Use {@link org.opendaylight.mdsal.dom.api.DOMNotification} instead.
 */
@Deprecated(forRemoval = true)
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE", justification = "Migration")
public interface DOMNotification extends org.opendaylight.mdsal.dom.api.DOMNotification {
}

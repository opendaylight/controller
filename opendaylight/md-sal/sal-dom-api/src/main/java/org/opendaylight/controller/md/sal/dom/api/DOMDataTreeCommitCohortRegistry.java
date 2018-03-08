/*
 * Copyright (c) 2017 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Interface for a DOM commit cohort registry.
 *
 * @author Thomas Pantelis
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public interface DOMDataTreeCommitCohortRegistry extends DOMDataBrokerExtension,
        org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry {
}

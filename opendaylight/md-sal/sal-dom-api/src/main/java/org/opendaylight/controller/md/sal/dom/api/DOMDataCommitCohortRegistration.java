/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Registration of {@link DOMDataCommitCohort}. Used to track and revoke
 * registration with {@link DOMDataCommitHandlerRegistry}.
 *
 * @param <T> Type of {@link DOMDataCommitCohort}
 */
public interface DOMDataCommitCohortRegistration<T extends DOMDataCommitCohort> extends ObjectRegistration<T> {


    @Override
    public void close();
}

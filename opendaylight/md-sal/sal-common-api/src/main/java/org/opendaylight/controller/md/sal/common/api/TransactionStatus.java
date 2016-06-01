/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api;

@Deprecated
public enum TransactionStatus {
    /**
     * The transaction has been freshly allocated. The user is still accessing
     * it and it has not been sealed.
     */
    NEW,
    /**
     * The transaction has been completed by the user and sealed. It is currently
     * awaiting execution.
     */
    SUBMITED,
    /**
     * The transaction has been successfully committed to backing store.
     */
    COMMITED,
    /**
     * The transaction has failed to commit due to some underlying issue.
     */
    FAILED,
    /**
     * Currently unused.
     */
    CANCELED,
}

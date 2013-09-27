/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.util;

import org.opendaylight.yangtools.yang.common.QName;

public class NameConflictException extends RuntimeException {

    private static final String messageBlueprint = "Name conflict for name: %s, first defined in: %s, then defined in: %s";
    private final String conflictingName;
    private final QName secondParentQName;
    private final QName firstParentQName;

    public NameConflictException(String conflictingName,
            QName firstDefinedParentQName, QName secondDefinedParentQName) {
        super(String.format(messageBlueprint, conflictingName,
                firstDefinedParentQName, secondDefinedParentQName));
        this.conflictingName = conflictingName;
        this.firstParentQName = firstDefinedParentQName;
        this.secondParentQName = secondDefinedParentQName;
    }

    // TODO add yang local names

    public String getConflictingName() {
        return conflictingName;
    }

    public QName getSecondParentQName() {
        return secondParentQName;
    }

    public QName getFirstParentQName() {
        return firstParentQName;
    }
}

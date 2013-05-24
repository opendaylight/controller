/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;

final class MustDefinitionImpl implements MustDefinition {
    private final String mustStr;
    private final String description;
    private final String reference;
    private final String errorAppTag;
    private final String errorMessage;

    MustDefinitionImpl(String mustStr, String description, String reference,
            String errorAppTag, String errorMessage) {
        this.mustStr = mustStr;
        this.description = description;
        this.reference = reference;
        this.errorAppTag = errorAppTag;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getErrorAppTag() {
        return errorAppTag;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public RevisionAwareXPath getXpath() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mustStr == null) ? 0 : mustStr.hashCode());
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result
                + ((reference == null) ? 0 : reference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MustDefinitionImpl other = (MustDefinitionImpl) obj;
        if (mustStr == null) {
            if (other.mustStr != null) {
                return false;
            }
        } else if (!mustStr.equals(other.mustStr)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (reference == null) {
            if (other.reference != null) {
                return false;
            }
        } else if (!reference.equals(other.reference)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return mustStr;
    }

}

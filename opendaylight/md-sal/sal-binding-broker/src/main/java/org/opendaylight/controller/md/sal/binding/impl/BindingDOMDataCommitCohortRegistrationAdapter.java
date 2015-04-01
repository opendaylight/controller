/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.DataCommitCohort;
import org.opendaylight.controller.md.sal.binding.api.DataCommitCohortRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMDataCommitCohort;
import org.opendaylight.controller.md.sal.dom.api.DOMDataCommitCohortRegistration;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

class BindingDOMDataCommitCohortRegistrationAdapter<T extends DataCommitCohort> extends AbstractObjectRegistration<T> implements DataCommitCohortRegistration<T> {

    private final DOMDataCommitCohortRegistration<DOMDataCommitCohort> domReg;

    public BindingDOMDataCommitCohortRegistrationAdapter(final T cohort,
            final DOMDataCommitCohortRegistration<DOMDataCommitCohort> domReg) {
        super(cohort);
        this.domReg = domReg;
    }

    @Override
    protected void removeRegistration() {
        domReg.close();
    }

}

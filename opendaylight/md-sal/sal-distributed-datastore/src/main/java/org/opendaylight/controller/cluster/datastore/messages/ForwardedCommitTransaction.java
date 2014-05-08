/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

public class ForwardedCommitTransaction {
  private final DOMStoreThreePhaseCommitCohort cohort;
  private final Modification modification;

  public ForwardedCommitTransaction(DOMStoreThreePhaseCommitCohort cohort, Modification modification){
    this.cohort = cohort;
    this.modification = modification;
  }

  public DOMStoreThreePhaseCommitCohort getCohort() {
    return cohort;
  }

  public Modification getModification() {
    return modification;
  }
}

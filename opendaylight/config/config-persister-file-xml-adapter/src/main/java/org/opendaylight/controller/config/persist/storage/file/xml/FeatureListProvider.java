/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.storage.file.xml;

import java.util.Set;

/**
 * Wrapper for services providing list of features to be stored along with the config snapshot
 */
public interface FeatureListProvider {

    Set<String> listFeatures();
}

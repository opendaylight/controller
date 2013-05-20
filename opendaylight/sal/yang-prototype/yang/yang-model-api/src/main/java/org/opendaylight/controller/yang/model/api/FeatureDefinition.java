/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

/**
 * Interface describing YANG 'feature' statement.
 * <p>
 * The feature statement is used to define a mechanism by which portions of the
 * schema are marked as conditional. A feature name can later be referenced
 * using the 'if-feature' statement.
 * </p>
 */
public interface FeatureDefinition extends SchemaNode {

}

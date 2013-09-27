/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.annotations;

/**
 * Marker interface on all Service Interface annotated classes, each Service
 * Interface must extend this interface. Service Intefaces can form hierarchies,
 * one SI can extend another one, in which case all annotations in hierarchy
 * will be observed.
 */
public abstract interface AbstractServiceInterface {
}

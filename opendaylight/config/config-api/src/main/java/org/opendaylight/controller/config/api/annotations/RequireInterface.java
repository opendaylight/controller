/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates what service interface is expected to be obtained as a dependency
 * of a module. This annotation must be present for each dependency setter in
 * {@link org.opendaylight.controller.config.spi.Module} M(X)Bean interface.
 * Together with name of dependent bean the {@link #value()} will be used to get
 * {@link ObjectName} of dependency.
 *
 * <p>
 * Example:<br>
 *
 * <code>
 *
 * @RequireInterface(value = ThreadPoolServiceInterface.class, optional =
 *                         false)<br/> void setThreadPool(ObjectName on);
 *                         </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireInterface {

    /**
     * Declares dependency on service interface.
     */
    Class<? extends AbstractServiceInterface> value();

}

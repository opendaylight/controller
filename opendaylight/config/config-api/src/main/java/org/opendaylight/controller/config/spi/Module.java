/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.spi;

import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;


/**
 * Represents one service that is to be configured. These methods need to be
 * implemented in addition to the usual attribute getters/setters. Dependencies
 * should always be injected as ObjectName references to the corresponding
 * ConfigBeans.
 * <p>
 * In order to guide dependency resolution, the setter method should be
 * annotated with {@link org.opendaylight.controller.config.api.annotations.RequireInterface}.
 * </p>
 * <p>
 * Thread safety note: implementations of this interface are not required to be
 * thread safe as thread safety is enforced by configuration manager.
 * </p>
 */
@NotThreadSafe
public interface Module extends Identifiable<ModuleIdentifier>{
    /**
     * This method will be called as first phase in two phase commit. Instance
     * can check attributes, but is not allowed to do any kind of work that
     * could leave any resources open. It is prohibited to call
     * {@link #getInstance()} on dependent {@link Module} because it would
     * destroy separation between validation and commit phase.
     *
     */
    void validate();

    /**
     * Returns 'live' object that was configured using this object. It is
     * allowed to call this method only after all ConfigBeans were validated. In
     * this method new resources might be opened or old instance might be
     * modified. This method must be implemented so that it returns same
     * result for a single transaction. Since Module is created per transaction
     * this means that it must be safe to cache result of first call.
     *
     *
     * @return closeable instance: After bundle update the factory might be able
     *         to copy old configuration into new one without being able to cast
     *         Module or the instance. Thus to clean up old instance, it will
     *         call close().
     */
    AutoCloseable getInstance();


    /**
     * Compare current module with oldModule and if the instance/live object
     * produced by the old module can be reused in this module as well return true.
     * Typically true should be returned if the old module had the same configuration.
     *
     *
     * @param oldModule old instance of Module
     * @return true if the instance produced by oldModule can be reused with current instance as well.
     */
    public boolean canReuse(Module oldModule);


}

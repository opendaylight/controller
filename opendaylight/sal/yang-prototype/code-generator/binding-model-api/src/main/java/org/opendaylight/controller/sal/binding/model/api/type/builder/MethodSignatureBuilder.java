/**

 *
 * March 2013
 *
 * Copyright (c) 2013 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;

/**
 * Method Signature Builder serves solely for building Method Signature and
 * returning the <code>new</code> instance of Method Signature.
 * <br>
 * By definition of {@link MethodSignature} the Method in java MUST contain
 * Name, Return Type and Access Modifier. By default the Access Modifier can
 * be set to public. The Method Signature builder does not contain method for
 * addName due to enforce reason that MethodSignatureBuilder SHOULD be
 * instantiated only once with defined method name.
 * <br>
 * The methods as {@link #addAnnotation(String, String)} and {@link #setComment(String)}
 * can be used as optional because not all methods MUST contain annotation or
 * comment definitions.
 *
 *
 * @see MethodSignature
 */
public interface MethodSignatureBuilder extends TypeMemberBuilder {

    /**
     * Sets the flag for declaration of method as abstract or non abstract. If the flag <code>isAbstract == true</code>
     * The instantiated Method Signature MUST have return value for {@link org.opendaylight.controller.sal.binding
     * .model.api.MethodSignature#isAbstract()} also equals to <code>true</code>.
     *
     * @param isAbstract is abstract flag
     */
    public void setAbstract(boolean isAbstract);

    /**
     * Adds Parameter into the List of method parameters. Neither the Name or
     * Type of parameter can be <code>null</code>.
     *
     * <br>
     * In case that any of parameters are defined as <code>null</code> the
     * method SHOULD throw an {@link IllegalArgumentException}
     *
     * @param type Parameter Type
     * @param name Parameter Name
     */
    public void addParameter(final Type type, final String name);

    /**
     * Returns <code>new</code> <i>immutable</i> instance of Method Signature.
     * <br>
     * The <code>definingType</code> param cannot be <code>null</code>. The
     * every method in Java MUST be declared and defined inside the scope of
     * <code>class</code> or <code>interface</code> definition. In case that
     * defining Type will be passed as <code>null</code> reference the method
     * SHOULD thrown {@link IllegalArgumentException}.
     *
     * @param definingType Defining Type of Method Signature
     * @return <code>new</code> <i>immutable</i> instance of Method Signature.
     */
    public MethodSignature toInstance(final Type definingType);
}

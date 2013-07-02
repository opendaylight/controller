/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;

/**
 * Generated Type Builder interface is helper interface for building and
 * defining the GeneratedType.
 * 
 * @see GeneratedType
 */
public interface GeneratedTypeBuilder extends Type {

    /**
     * Adds new Enclosing Type into definition of Generated Type and returns
     * <code>new</code> Instance of Generated Type Builder. <br>
     * There is no need of specifying of Package Name because enclosing Type is
     * already defined inside Generated Type with specific package name. <br>
     * The name of enclosing Type cannot be same as Name of parent type and if
     * there is already defined enclosing type with the same name, the new
     * enclosing type will simply overwrite the older definition. <br>
     * If the name of enclosing type is <code>null</code> the method SHOULD
     * throw {@link IllegalArgumentException}
     * 
     * @param name
     *            Name of Enclosing Type
     * @return <code>new</code> Instance of Generated Type Builder.
     */
    public GeneratedTypeBuilder addEnclosingType(final String name);

    /**
     * Adds new Enclosing Transfer Object into definition of Generated Type and
     * returns <code>new</code> Instance of Generated TO Builder. <br>
     * There is no need of specifying of Package Name because enclosing Type is
     * already defined inside Generated Type with specific package name. <br>
     * The name of enclosing Type cannot be same as Name of parent type and if
     * there is already defined enclosing type with the same name, the new
     * enclosing type will simply overwrite the older definition. <br>
     * If the name of enclosing type is <code>null</code> the method SHOULD
     * throw {@link IllegalArgumentException}
     * 
     * @param name
     *            Name of Enclosing Type
     * @return <code>new</code> Instance of Generated Type Builder.
     */
    public GeneratedTOBuilder addEnclosingTransferObject(final String name);

    /**
     * Adds new Enclosing Transfer Object <code>genTOBuilder</code> into
     * definition of Generated Type
     * 
     * <br>
     * There is no need of specifying of Package Name because enclosing Type is
     * already defined inside Generated Type with specific package name. <br>
     * The name of enclosing Type cannot be same as Name of parent type and if
     * there is already defined enclosing type with the same name, the new
     * enclosing type will simply overwrite the older definition. <br>
     * If the parameter <code>genTOBuilder</code> of enclosing type is
     * <code>null</code> the method SHOULD throw
     * {@link IllegalArgumentException}
     * 
     * @param <code>genTOBuilder</code> Name of Enclosing Type
     */
    public void addEnclosingTransferObject(final GeneratedTOBuilder genTOBuilder);

    /**
     * Adds String definition of comment into Method Signature definition. <br>
     * The comment String MUST NOT contain anny comment specific chars (i.e.
     * "/**" or "//") just plain String text description.
     * 
     * @param comment
     *            Comment String.
     */
    public void addComment(final String comment);

    /**
     * The method creates new AnnotationTypeBuilder containing specified package
     * name an annotation name. <br>
     * Neither the package name or annotation name can contain <code>null</code>
     * references. In case that any of parameters contains <code>null</code> the
     * method SHOULD thrown {@link IllegalArgumentException}
     * 
     * @param packageName
     *            Package Name of Annotation Type
     * @param name
     *            Name of Annotation Type
     * @return <code>new</code> instance of Annotation Type Builder.
     */
    public AnnotationTypeBuilder addAnnotation(final String packageName, final String name);

    /**
     * Sets the <code>abstract</code> flag to define Generated Type as
     * <i>abstract</i> type.
     * 
     * @param isAbstract
     *            abstract flag
     */
    public void setAbstract(boolean isAbstract);

    /**
     * Add Type to implements.
     * 
     * @param genType
     *            Type to implement
     * @return <code>true</code> if the addition of type is successful.
     */
    public boolean addImplementsType(final Type genType);

    /**
     * Adds Constant definition and returns <code>new</code> Constant instance. <br>
     * By definition Constant MUST be defined by return Type, Name and assigned
     * value. The name SHOULD be defined with cpaital letters. Neither of method
     * parameters can be <code>null</code> and the method SHOULD throw
     * {@link IllegalArgumentException} if the contract is broken.
     * 
     * @param type
     *            Constant Type
     * @param name
     *            Name of Constant
     * @param value
     *            Assigned Value
     * @return <code>new</code> Constant instance.
     */
    public Constant addConstant(final Type type, final String name, final Object value);

    /**
     * Adds new Enumeration definition for Generated Type Builder and returns
     * Enum Builder for specifying all Enum parameters. <br>
     * If there is already Enumeration stored with the same name, the old enum
     * will be simply overwritten byt new enum definition. <br>
     * Name of Enumeration cannot be <code>null</code>, if it is
     * <code>null</code> the method SHOULD throw
     * {@link IllegalArgumentException}
     * 
     * @param name
     *            Enumeration Name
     * @return <code>new</code> instance of Enumeration Builder.
     */
    public EnumBuilder addEnumeration(final String name);

    /**
     * Add new Method Signature definition for Generated Type Builder and
     * returns Method Signature Builder for specifying all Method parameters. <br>
     * Name of Method cannot be <code>null</code>, if it is <code>null</code>
     * the method SHOULD throw {@link IllegalArgumentException} <br>
     * By <i>Default</i> the MethodSignatureBuilder SHOULD be pre-set as
     * {@link MethodSignatureBuilder#setAbstract(true)},
     * {@link MethodSignatureBuilder#setFinal(false)} and
     * {@link MethodSignatureBuilder#setAccessModifier(PUBLIC)}
     * 
     * @param name
     *            Name of Method
     * @return <code>new</code> instance of Method Signature Builder.
     */
    public MethodSignatureBuilder addMethod(final String name);

    /**
     * Checks if GeneratedTypeBuilder contains method with name
     * <code>methodName</code>
     * 
     * @param methodName
     *            is method name
     */
    public boolean containsMethod(final String methodName);

    /**
     * Returns the <code>new</code> <i>immutable</i> instance of Generated Type.
     * 
     * @return the <code>new</code> <i>immutable</i> instance of Generated Type.
     */
    public GeneratedType toInstance();
}

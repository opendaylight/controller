package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.Type;

/**
 *
 */
public interface TypeMemberBuilder {

    /**
     * The method creates new AnnotationTypeBuilder containing specified
     * package name an annotation name.
     * <br>
     * Neither the package name or annotation name can contain
     * <code>null</code> references. In case that
     * any of parameters contains <code>null</code> the method SHOULD thrown
     * {@link IllegalArgumentException}
     *
     * @param packageName Package Name of Annotation Type
     * @param name Name of Annotation Type
     * @return <code>new</code> instance of Annotation Type Builder.
     */
    public AnnotationTypeBuilder addAnnotation(final String packageName, final String name);

    /**
     * Returns the name of property.
     *
     * @return the name of property.
     */
    public String getName();

    /**
     * Adds return Type into Builder definition for Generated Property.
     * <br>
     * The return Type MUST NOT be <code>null</code>,
     * otherwise the method SHOULD throw {@link IllegalArgumentException}
     *
     * @param returnType Return Type of property.
     */
    public void setReturnType(final Type returnType);

    /**
     * Sets the access modifier of property.
     *
     * @param modifier Access Modifier value.
     */
    public void setAccessModifier(final AccessModifier modifier);

    /**
     * Adds String definition of comment into Method Signature definition.
     * <br>
     * The comment String MUST NOT contain anny comment specific chars (i.e.
     * "/**" or "//") just plain String text description.
     *
     * @param comment Comment String.
     */
    public void setComment(final String comment);

    /**
     * Sets the flag final for method signature. If this is set the method will be prohibited from overriding.
     * <br>
     * This setting is irrelevant for methods designated to be defined in interface definitions because interface
     * can't have final method.
     *
     * @param isFinal Is Final
     */
    public void setFinal(final boolean isFinal);
}

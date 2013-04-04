/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */

/**
 * Definition of structures and DOM Like API of processed YANG schema
 * 
 * <h3>YANG Statement mapping</h3>
 * 
 * <dl>
 * <dt>anyxml
 *   <dd>{@link org.opendaylight.controller.yang.model.api.AnyXmlSchemaNode}
 * 
 * <dt>argument
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ExtensionDefinition#getArgumentName()}
 * 
 * 
 * <dt>augment
 *   <dd>{@link org.opendaylight.controller.yang.model.api.AugmentationSchema}
 * 
 * <dt>base
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.IdentityrefTypeDefinition#getIdentity()}
 * 
 * <dt>belongs-to
 *   <dd>
 * 
 * <dt>bit
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition#getBits()}
 * 
 * <dt>case
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ChoiceCaseNode}
 * 
 * <dt>choice
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ChoiceNode}
 * 
 * <dt>config
 *   <dd>{@link org.opendaylight.controller.yang.model.api.DataSchemaNode#isConfiguration()}
 * 
 * <dt>contact
 *   <dd>{@link org.opendaylight.controller.yang.model.api.Module#getContact()}
 * 
 * <dt>container
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ContainerSchemaNode}
 * 
 * <dt>default
 *   <dd>
 * 
 * <dt>description
 *   <dd>{@link org.opendaylight.controller.yang.model.api.SchemaNode#getDescription()}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintMetaDefinition#getDescription()}
 * 
 * <dt>enum
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition#getValues()}
 * 
 * <dt>error-app-tag
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintMetaDefinition#getErrorAppTag()}
 * 
 * <dt>error-message
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintMetaDefinition#getErrorMessage()}
 * 
 * <dt>extension
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ExtensionDefinition}
 * 
 * <dt>deviation
 *   <dd>{@link org.opendaylight.controller.yang.model.api.Deviation}
 * 
 * <dt>deviate
 *   <dd>
 * 
 * <dt>feature
 *   <dd>{@link org.opendaylight.controller.yang.model.api.FeatureDefinition}
 * 
 * <dt>fraction-digits
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.DecimalTypeDefinition#getFractionDigits()}
 * 
 * <dt>grouping
 *   <dd>{@link org.opendaylight.controller.yang.model.api.GroupingDefinition}
 * 
 * <dt>identity
 *   <dd>
 * 
 * <dt>if-feature
 *   <dd>
 * 
 * <dt>import
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ModuleImport}
 * 
 * <dt>include
 *   <dd>
 * 
 * <dt>input
 *   <dd>{@link org.opendaylight.controller.yang.model.api.RpcDefinition#getInput()}
 * 
 * <dt>key
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ListSchemaNode#getKeyDefinition()}
 * 
 * <dt>leaf
 *   <dd>{@link org.opendaylight.controller.yang.model.api.LeafSchemaNode}
 * 
 * <dt>leaf-list
 *   <dd>{@link org.opendaylight.controller.yang.model.api.LeafListSchemaNode}
 * 
 * <dt>length
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.LengthConstraint}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.StringTypeDefinition#getLengthStatements()}
 * 
 * <dt>list
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ListSchemaNode}
 * 
 * <dt>mandatory
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintDefinition#isMandatory()}
 * 
 * <dt>max-elements
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintDefinition#getMinElements()}
 * 
 * <dt>min-elements
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintDefinition#getMaxElements()}
 * 
 * <dt>module
 *   <dd>{@link org.opendaylight.controller.yang.model.api.Module}
 * 
 * <dt>must
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintDefinition#getMustConstraints()}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.MustDefinition}
 * 
 * <dt>namespace
 *   <dd>{@link org.opendaylight.controller.yang.model.api.Module#getNamespace()}
 * 
 * <dt>notification
 *   <dd>{@link org.opendaylight.controller.yang.model.api.NotificationDefinition}
 * 
 * <dt>ordered-by
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ListSchemaNode#isUserOrdered()}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.LeafListSchemaNode#isUserOrdered()}
 * 
 * <dt>organization
 *   <dd>{@link org.opendaylight.controller.yang.model.api.Module#getOrganization()}
 * 
 * <dt>output
 *   <dd>{@link org.opendaylight.controller.yang.model.api.RpcDefinition#getOutput()}
 * 
 * <dt>path
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition#getPathStatement()}
 * 
 * <dt>pattern
 *   <dd>{@link org.opendaylight.controller.yang.model.base.type.api.PatternConstraint}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.StringTypeDefinition}
 * 
 * <dt>position
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit#getPosition()}
 * 
 * <dt>prefix
 *   <dd>{@link org.opendaylight.controller.yang.model.api.Module#getPrefix()}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ModuleImport#getPrefix()}
 * 
 * <dt>presence
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ContainerSchemaNode#isPresenceContainer()}
 * 
 * <dt>range
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.RangeConstraint}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition#getRangeStatements()}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.UnsignedIntegerTypeDefinition#getRangeStatements()}
 * 
 * <dt>reference
 *   <dd>{@link org.opendaylight.controller.yang.model.api.SchemaNode#getReference()}
 * 
 * <dt>refine
 *   <dd>
 * 
 * <dt>require-instance
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.InstanceIdentifierTypeDefinition#requireInstance()}
 * 
 * <dt>revision
 *   <dd>{@link org.opendaylight.controller.yang.model.api.Module#getRevision()}
 * 
 * <dt>revision-date
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ModuleImport#getRevision()}
 * 
 * <dt>rpc
 *   <dd>{@link org.opendaylight.controller.yang.model.api.RpcDefinition}
 * 
 * <dt>status
 *   <dd>{@link org.opendaylight.controller.yang.model.api.SchemaNode#getStatus()}
 * 
 * <dt>submodule
 *   <dd>
 * 
 * <dt>type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.TypeDefinition}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.LeafSchemaNode#getType()}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.LeafListSchemaNode#getType()}
 * 
 * <dt>typedef
 *   <dd>{@link org.opendaylight.controller.yang.model.api.TypeDefinition}
 * 
 * <dt>unique
 *   <dd>
 * 
 * <dt>units
 *   <dd>{@link org.opendaylight.controller.yang.model.api.TypeDefinition#getUnits()}
 * 
 * <dt>uses
 *   <dd>{@link org.opendaylight.controller.yang.model.api.UsesNode}
 *   <dd>{@link org.opendaylight.controller.yang.model.api.DataNodeContainere#getUses()}
 * 
 * <dt>value
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair#getValue()}
 * 
 * <dt>when
 *   <dd>{@link org.opendaylight.controller.yang.model.api.ConstraintDefinition#getWhenCondition()}
 * 
 * <dt>yang-version
 * 
 * <dt>yin-element
 *   <dd>
 * 
 * 
 * 
 * 
 * <dt>add
 *   <dd>
 * 
 * <dt>current
 *   <dd>
 * 
 * <dt>delete
 *   <dd>
 * 
 * <dt>deprecated
 *   <dd>
 * 
 * <dt>false
 *   <dd>
 * 
 * <dt>max
 *   <dd>
 * 
 * <dt>min
 *   <dd>
 * 
 * <dt>not-supported
 *   <dd>
 * 
 * <dt>obsolete
 *   <dd>
 * 
 * <dt>replace
 *   <dd>
 * 
 * <dt>system
 *   <dd>
 * 
 * <dt>true
 *   <dd>
 * 
 * <dt>unbounded
 *   <dd>
 * 
 * <dt>user
 *   <dd>
 * </dl>
 * 
 * 
 * <h3>YANG Base Type Mapping</h3>
 * 
 * 
 * <dl>
 * <dt>Integer built-in type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition}
 * 
 * <dt>Unsigned integer built-in type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.UnsignedIntegerTypeDefinition}
 * 
 * <dt>Decimal64 built-ib type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.DecimalTypeDefinition}
 * 
 * <dt>Boolean built-in type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.BooleanTypeDefinition}
 *   
 * <dt>Enumeration built-in type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition}
 *   
 * <dt>Bits Built-In Type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition}
 * 
 * <dt>The binary Built-In Type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.BinaryTypeDefinition}
 *   
 * <dt>The leafref Built-In Type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition}
 * 
 * <dt>The identityref Built-In Type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.IdentityrefTypeDefinition}
 *   
 * <dt>The empty Built-In Type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.EmptyTypeDefinition}
 *   
 * <dt>The union Built-In Type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.UnionTypeDefinition}
 * <dt>The instance-identifier Built-In Type
 *   <dd>{@link org.opendaylight.controller.yang.model.api.type.InstanceIdentifierTypeDefinition}
 * 
 * </dl>
 */
package org.opendaylight.controller.yang.model.api;


/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import java.util.List;

import org.opendaylight.controller.model.parser.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.api.SchemaNodeBuilder;
import org.opendaylight.controller.model.parser.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;


public class LeafListSchemaNodeBuilder implements SchemaNodeBuilder, TypeAwareBuilder, MustAwareBuilder, DataSchemaNodeBuilder {

		private final LeafListSchemaNodeImpl instance;
		private final QName qname;
		private TypeDefinition<?> type;
		private MustDefinitionBuilder mustDefinitionBuilder;

		LeafListSchemaNodeBuilder(QName qname) {
			this.qname = qname;
			instance = new LeafListSchemaNodeImpl(qname);
		}


		@Override
		public DataSchemaNode build() {
			if(mustDefinitionBuilder != null) {
				MustDefinition mustDefinition = mustDefinitionBuilder.build();
				instance.setMustDefinition(mustDefinition);
			}
			return instance;
		}

		@Override
		public QName getQName() {
			return qname;
		}

		@Override
		public void setPath(SchemaPath path) {
			instance.setPath(path);
		}

		@Override
		public void setDescription(String description) {
			instance.setDescription(description);
		}

		@Override
		public void setReference(String reference) {
			instance.setReference(reference);
		}

		@Override
		public void setStatus(Status status) {
			instance.setStatus(status);
		}

		@Override
		public TypeDefinition<?> getType() {
			return type;
		}

		@Override
		public void setType(TypeDefinition<?> type) {
			this.type = type;
			instance.setType(type);
		}

		@Override
		public void setMustDefinitionBuilder(MustDefinitionBuilder mustDefinitionBuilder) {
			this.mustDefinitionBuilder = mustDefinitionBuilder;
		}

		public void setAugmenting(boolean augmenting) {
			instance.setAugmenting(augmenting);
		}
		public void setConfiguration(boolean configuration) {
			instance.setConfiguration(configuration);
		}
		public void setConstraints(ConstraintDefinition constraints) {
			instance.setConstraints(constraints);
		}
		public void setUserOrdered(boolean userOrdered) {
			instance.setUserOrdered(userOrdered);
		}


		private class LeafListSchemaNodeImpl implements LeafListSchemaNode {
			private final QName qname;
			private SchemaPath path;
			private String description;
			private String reference;
			private Status status = Status.CURRENT;

			private boolean augmenting;
			private boolean configuration;
			private ConstraintDefinition constraints;

			private TypeDefinition<?> type;
			private boolean userOrdered;

			private MustDefinition mustDefinition;

			private LeafListSchemaNodeImpl(QName qname) {
				this.qname = qname;
			}


			@Override
			public QName getQName() {
				return qname;
			}

			@Override
			public SchemaPath getPath() {
				return path;
			}
			private void setPath(SchemaPath path) {
				this.path = path;;
			}

			@Override
			public String getDescription() {
				return description;
			}
			private void setDescription(String description) {
				this.description = description;
			}

			@Override
			public String getReference() {
				return reference;
			}
			private void setReference(String reference) {
				this.reference = reference;
			}

			@Override
			public Status getStatus() {
				return status;
			}
			private void setStatus(Status status) {
				this.status = status;
			}

			@Override
			public boolean isAugmenting() {
				return augmenting;
			}
			private void setAugmenting(boolean augmenting) {
				this.augmenting = augmenting;
			}

			@Override
			public boolean isConfiguration() {
				return configuration;
			}
			private void setConfiguration(boolean configuration) {
				this.configuration = configuration;
			}

			@Override
			public ConstraintDefinition getConstraints() {
				return constraints;
			}
			private void setConstraints(ConstraintDefinition constraints) {
				this.constraints = constraints;
			}

			@Override
			public TypeDefinition<?> getType() {
				return type;
			}
			public void setType(TypeDefinition<? extends TypeDefinition<?>> type) {
				this.type = type;
			}

			@Override
			public boolean isUserOrdered() {
				return userOrdered;
			}
			private void setUserOrdered(boolean userOrdered) {
				this.userOrdered = userOrdered;
			}

			@Override
			public MustDefinition getMustDefinition() {
				return mustDefinition;
			}
			private void setMustDefinition(MustDefinition mustDefinition) {
				this.mustDefinition = mustDefinition;
			}

			@Override
			public List<ExtensionDefinition> getExtensionSchemaNodes() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String toString() {
				return LeafListSchemaNodeImpl.class.getSimpleName() +"[qname="+ qname +", type="+ type +"]";
			}
		}

	}
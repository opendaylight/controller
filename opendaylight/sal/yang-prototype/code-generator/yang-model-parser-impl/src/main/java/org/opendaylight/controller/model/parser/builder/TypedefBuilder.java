/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import java.util.List;

import org.opendaylight.controller.model.parser.api.SchemaNodeBuilder;
import org.opendaylight.controller.model.parser.api.TypeAwareBuilder;
import org.opendaylight.controller.model.parser.api.TypeDefinitionBuilder;
import org.opendaylight.controller.model.util.UnknownType;
import org.opendaylight.controller.model.util.YangTypesConverter;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;


public class TypedefBuilder implements TypeDefinitionBuilder, SchemaNodeBuilder, TypeAwareBuilder {

	private final QName qname;
	private SchemaPath schemaPath;
	private TypeDefinition<?> baseType;

	private String description;
	private String reference;
	private Status status;

	TypedefBuilder(QName qname) {
		this.qname = qname;
	}

	@Override
	public TypeDefinition<? extends TypeDefinition<?>> build() {
		final TypeDefinition<?> type = YangTypesConverter.javaTypeForBaseYangType(qname);
		if(type != null) {
			return type;
		} else {
			if(baseType != null) {
				// typedef
				TypeDefinitionImpl instance = new TypeDefinitionImpl(qname);
				instance.setDescription(description);
				instance.setReference(reference);
				instance.setStatus(status);
				instance.setPath(schemaPath);
				instance.setBaseType(baseType);
				return instance;
			} else {
				// type
				final UnknownType.Builder unknownBuilder = new UnknownType.Builder(qname, description, reference);
				unknownBuilder.status(status);
				return unknownBuilder.build();
			}
		}
	}

	@Override
	public QName getQName() {
		return qname;
	}

	@Override
	public void setPath(final SchemaPath schemaPath) {
		this.schemaPath = schemaPath;
	}

	@Override
	public void setDescription(final String description) {
		this.description = description;
	}

	@Override
	public void setReference(final String reference) {
		this.reference = reference;
	}

	@Override
	public void setStatus(final Status status) {
		if(status != null) {
			this.status = status;
		}
	}

	@Override
	public TypeDefinition<?> getType() {
		return baseType;
	}

	@Override
	public void setType(TypeDefinition<?> baseType) {
		this.baseType = baseType;
	}

	@Override
	public TypeDefinition<?> getBaseType() {
		return baseType;
	}



	private static class TypeDefinitionImpl<T extends TypeDefinition<T>> implements TypeDefinition<T> {

		private final QName qname;
		private SchemaPath path;
		private String description;
		private String reference;
		private Status status = Status.CURRENT;
		private T baseType;

		private TypeDefinitionImpl(QName qname) {
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
			this.path = path;
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
		public T getBaseType() {
			return baseType;
		}
		private void setBaseType(T type) {
			this.baseType = type;
		}

		@Override
		public String getUnits() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getDefaultValue() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<ExtensionDefinition> getExtensionSchemaNodes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder(TypeDefinitionImpl.class.getSimpleName());
			sb.append("[");
			sb.append("qname="+ qname);
			sb.append(", path="+ path);
			sb.append(", description="+ description);
			sb.append(", reference="+ reference);
			sb.append(", status="+ status);
			sb.append(", baseType="+ baseType +"]");
			return sb.toString();
		}
	}

}

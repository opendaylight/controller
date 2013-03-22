/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.model.parser.api.AbstractChildNodeBuilder;
import org.opendaylight.controller.model.parser.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.model.parser.api.GroupingBuilder;
import org.opendaylight.controller.model.parser.api.SchemaNodeBuilder;
import org.opendaylight.controller.model.parser.api.TypeDefinitionAwareBuilder;
import org.opendaylight.controller.model.parser.api.TypeDefinitionBuilder;
import org.opendaylight.controller.model.parser.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;


public class NotificationBuilder extends AbstractChildNodeBuilder implements TypeDefinitionAwareBuilder, SchemaNodeBuilder {

	private final NotificationDefinitionImpl instance;
	private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
	private final Set<UsesNodeBuilder> addedUsesNodes = new HashSet<UsesNodeBuilder>();

	NotificationBuilder(QName qname) {
		super(qname);
		instance = new NotificationDefinitionImpl(qname);
	}

	@Override
	public SchemaNode build() {
		// CHILD NODES
		Map<QName, DataSchemaNode> childs = new HashMap<QName, DataSchemaNode>();
		for (DataSchemaNodeBuilder node : childNodes) {
			childs.put(node.getQName(), node.build());
		}
		instance.setChildNodes(childs);

		// GROUPINGS
		Set<GroupingDefinition> groupingDefinitions = new HashSet<GroupingDefinition>();
		for (GroupingBuilder builder : groupings) {
			groupingDefinitions.add(builder.build());
		}
		instance.setGroupings(groupingDefinitions);

		// TYPEDEFS
		Set<TypeDefinition<?>> typedefs = new HashSet<TypeDefinition<?>>();
		for (TypeDefinitionBuilder entry : addedTypedefs) {
			typedefs.add(entry.build());
		}
		instance.setTypeDefinitions(typedefs);

		// USES
		Set<UsesNode> uses = new HashSet<UsesNode>();
		for (UsesNodeBuilder builder : addedUsesNodes) {
			uses.add(builder.build());
		}
		instance.setUses(uses);

		return instance;
	}

	@Override
	public void addTypedef(TypeDefinitionBuilder type) {
		addedTypedefs.add(type);
	}

	@Override
	public void addUsesNode(UsesNodeBuilder usesNodeBuilder) {
		addedUsesNodes.add(usesNodeBuilder);
	}

	@Override
	public void setPath(SchemaPath schemaPath) {
		instance.setPath(schemaPath);
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


	private class NotificationDefinitionImpl implements NotificationDefinition {

		private final QName qname;
		private SchemaPath path;
		private String description;
		private String reference;
		private Status status;

		private Map<QName, DataSchemaNode> childNodes;
		private Set<GroupingDefinition> groupings;
		private Set<TypeDefinition<?>> typeDefinitions;

		private Set<UsesNode> uses;

		private NotificationDefinitionImpl(QName qname) {
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
		public Set<DataSchemaNode> getChildNodes() {
			return new HashSet<DataSchemaNode>(childNodes.values());
		}
		private void setChildNodes(Map<QName, DataSchemaNode> childNodes) {
			this.childNodes = childNodes;
		}

		@Override
		public Set<GroupingDefinition> getGroupings() {
			return groupings;
		}
		private void setGroupings(Set<GroupingDefinition> groupings) {
			this.groupings = groupings;
		}

		@Override
		public Set<UsesNode> getUses() {
			return uses;
		}
		private void setUses(Set<UsesNode> uses) {
			this.uses = uses;
		}

		@Override
		public Set<TypeDefinition<?>> getTypeDefinitions() {
			return typeDefinitions;
		}
		private void setTypeDefinitions(Set<TypeDefinition<?>> typeDefinitions) {
			this.typeDefinitions = typeDefinitions;
		}

		@Override
		public List<ExtensionDefinition> getExtensionSchemaNodes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public DataSchemaNode getDataChildByName(QName name) {
			return childNodes.get(name);
		}

		@Override
		public DataSchemaNode getDataChildByName(String name) {
			DataSchemaNode result = null;
			for (Map.Entry<QName, DataSchemaNode> entry : childNodes.entrySet()) {
				if (entry.getKey().getLocalName().equals(name)) {
					result = entry.getValue();
					break;
				}
			}
			return result;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(
					NotificationDefinitionImpl.class.getSimpleName());
			sb.append("[qname=" + qname + "]");
			return sb.toString();
		}
	}

}

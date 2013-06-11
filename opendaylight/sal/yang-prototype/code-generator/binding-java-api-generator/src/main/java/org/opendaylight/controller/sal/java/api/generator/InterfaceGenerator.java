/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.java.api.generator;

import static org.opendaylight.controller.sal.java.api.generator.Constants.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.binding.model.api.CodeGenerator;
import org.opendaylight.controller.sal.binding.model.api.Constant;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;

public final class InterfaceGenerator implements CodeGenerator {

	private Map<String, LinkedHashMap<String, Integer>> imports;

	private String generateEnums(List<Enumeration> enums) {
		String result = "";
		if (enums != null) {
			EnumGenerator enumGenerator = new EnumGenerator();
			for (Enumeration en : enums) {
				try {
					result = result
							+ (enumGenerator.generateInnerEnumeration(en, TAB).toString() + NL);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	private String generateConstants(List<Constant> constants, String pkgName) {
		String result = "";
		if (constants != null) {
			for (Constant c : constants) {
				result = result
						+ GeneratorUtil
								.createConstant(c, TAB, imports, pkgName) + NL;
			}
			result.concat(NL);
		}
		return result;
	}

	public String generateMethods(List<MethodSignature> methods, String pkgName) {
		String result = "";

		if (methods != null) {
			for (MethodSignature m : methods) {
				result = result
						+ GeneratorUtil.createMethodDeclaration(m, TAB,
								imports, pkgName) + NL;
			}
			result = result + NL;
		}
		return result;
	}

	public Writer generate(Type type) throws IOException {
		Writer writer = new StringWriter();
		if (type instanceof GeneratedType
				&& !(type instanceof GeneratedTransferObject)) {
			GeneratedType genType = (GeneratedType) type;
			imports = GeneratorUtil.createImports(genType);

			final String currentPkg = genType.getPackageName();
			final List<Constant> constants = genType.getConstantDefinitions();
			final List<MethodSignature> methods = genType
					.getMethodDefinitions();
			final List<Enumeration> enums = genType.getEnumDefintions();

			writer.write(GeneratorUtil.createPackageDeclaration(genType
					.getPackageName()));
			writer.write(NL);

			List<String> importLines = GeneratorUtil.createImportLines(imports);
			for (String line : importLines) {
				writer.write(line + NL);
			}
			writer.write(NL);
			writer.write(GeneratorUtil.createIfcDeclaration(genType, "",
					imports));
			writer.write(NL);

			writer.write(generateEnums(enums));
			writer.write(generateConstants(constants, currentPkg));
			writer.write(generateMethods(methods, currentPkg));

			writer.write(RCB);
		}
		return writer;
	}

}

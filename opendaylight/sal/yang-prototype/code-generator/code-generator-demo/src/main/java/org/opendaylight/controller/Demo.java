/**

 *
 * March 2013
 *
 * Copyright (c) 2013 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.controller;

import java.io.File;
import java.util.Map;

import org.opendaylight.controller.model.parser.builder.YangModelBuilder;



public class Demo {

	public static void main(String[] args) throws Exception {

		String yangFilesDir;
		if(args.length > 0) {
			yangFilesDir = args[0];
		} else {
			yangFilesDir = "src/main/resources";
		}

		File resourceDir = new File(yangFilesDir);
		if(!resourceDir.exists()) {
			throw new IllegalArgumentException("Specified resource directory does not exists: "+ resourceDir.getAbsolutePath());
		}

		String[] dirList = resourceDir.list();
		String[] absFiles = new String[dirList.length];

		int i = 0;
		for(String fileName : dirList) {
			File f = new File(fileName);
			absFiles[i] = f.getAbsolutePath();
			i++;
		}

        YangModelBuilder builder = new YangModelBuilder(absFiles);
        Map<String, org.opendaylight.controller.yang.model.api.Module> builtModules = builder.build();

        System.out.println("Modules built: "+ builtModules.size());
	}

}

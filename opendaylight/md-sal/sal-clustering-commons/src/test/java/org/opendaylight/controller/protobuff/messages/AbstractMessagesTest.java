/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.protobuff.messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author: syedbahm Date: 7/31/14
 */
public abstract class AbstractMessagesTest {
  public final String VERSION_COMPATIBILTY_TEST_DATA_PATH = "."
      + File.separator + "src" + File.separator + "test" + File.separator
      + "resources" + File.separator + "version-compatibility-serialized-data";
  private File file;
  private File testDataFile;

  protected AbstractMessagesTest() {
    init();
  }

  protected void init() {
    file = new File(getTestFileName());
    testDataFile =
        new File(VERSION_COMPATIBILTY_TEST_DATA_PATH + File.separator
            + getTestFileName() + "Data");
  }



  abstract public void verifySerialization() throws Exception;


  protected void writeToFile(
      com.google.protobuf.GeneratedMessage.Builder<?> builder) throws Exception {

    FileOutputStream output = new FileOutputStream(file);
    builder.build().writeTo(output);
    output.close();

  }

  protected com.google.protobuf.GeneratedMessage readFromFile(
      com.google.protobuf.Parser<?> parser) throws Exception {
    com.google.protobuf.GeneratedMessage message =
        (com.google.protobuf.GeneratedMessage) parser
            .parseFrom(new FileInputStream(file));

    /*Note: we will delete only the test file -- comment below if you want to capture the
       version-compatibility-serialized-data test data file.The file will be generated at root of the
       sal-protocolbuffer-encoding
       and you need to move it to test/resources/version-compatbility-serialized-data folder renaming the file to include suffix <TestFileName>"Data"
    */
     file.delete();
    return message;
  }

  protected com.google.protobuf.GeneratedMessage readFromTestDataFile(
      com.google.protobuf.Parser<?> parser) throws Exception {
    return (com.google.protobuf.GeneratedMessage) parser
        .parseFrom(new FileInputStream(testDataFile));
  }


  public abstract String getTestFileName();


}

This directory contains one serialized test data file for one of the messages in each .proto file.
These files are utilized as part of the  test cases, mostly to fail the test cases in case some engineer has used
a different version of protocol buffer than what we ship with (Protocol Buffer 2.5.0) to generate the messages source f
file.

1. If you see protocolbuffer version/invalid message exception in the test case
  1. ensure you are using the right version of protocol buffer/protoc compiler i.e protocol

2. If you have knowingly updated an existing .proto message than please update the corresponding version-compatibility-serialized-data
file. You can get the file by commenting out the test file deletion in AbstractMessagesTest look for comments

 /* we will delete only the test file -- comment below if you want to capture the
       version-compatibility-serialized-data test data file.The file will be generated at root of the sal-protocolbuffer-encoding
       and you need to move it to version-compatbility-serialized-data folder renaming the file to include suffix <TestFileName>"Data"
  */

3. If you are creating a new .proto file -- Follow an existing test case e.g. ThreePhaseCommitCohortMessagesTest to
   check how the test case is created and create the corresponding serialized test data file in version-compatibility-serialized-data

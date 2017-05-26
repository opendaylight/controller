#!/bin/bash

#####################################################################################################
#Instructions on generating the protocol buffer Java source files
#
#These instructions are developers who are planning to generate the protocolbuffer java source files.
#
#1. We are using protocol buffer version 2.5.0 - you need to install the exact same version on your box.
#The download link is https://code.google.com/p/protobuf/downloads/list. Download .tar/zip based on
#your OS.
#
#2. Once downloaded the tar/zip and extracted follow the README instructions to compile protoc on your
#machine
#
#3. Create your .proto (IDL) file in resources folder. Give appropriate package name so that the source
#   get generation in proper packages. For more information  check
#   https://developers.google.com/protocol-buffers/docs/javatutorial
#
#   For detailed information https://developers.google.com/protocol-buffers/docs/reference/java-generated
#
#4. To generate the java source files execute in sal-protocolbuffer-encoding execute ./run.sh i.e. this script
# or run command
#       protoc --proto_path=src/main/resources --java_out=src/main/java src/main/resources/*.proto
#
#5. Run mvn clean install & build the .jar
#
#6  !!!WARNING!!!! never edit the source files generated
#
#7. !!!NOTE!!! if you are planning to add new .proto file  option java_package should begin with
#   org.opendaylight.controller.protobuff.messages to properly exclude from sonar.
########################################################################################################

protoc --proto_path=src/main/resources --proto_path=../sal-akka-raft/src/main/resources --java_out=src/main/java src/main/resources/*.proto

echo "Done generating Java source files."

#to remove trailing spaces in the code files
find src/main/java -type f -name '*.java' -exec sed --in-place 's/[[:space:]]\+$//' {} \+

#to remove trailing spaces in the generated code on OSX
find src/main/java -type f -print0 |xargs -0 perl -pi -e 's/ +$//'

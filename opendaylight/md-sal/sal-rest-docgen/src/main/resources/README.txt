This component offers Swagger documentation of the RestConf APIs.

This Swagger documentation can be accessed in two ways:
I. Running server
Open a browser and go to http://<host>:8181/apidoc/explorer/index.html

II. Static documentation generation
By adding a reference to the StaticDocGenerator class in any pom.xml,
static documentation will be generated.  This static documentation will
document all the RestConf APIs for the YANG files in that artifact and
all the YANG files in that artifact's dependencies.

In order to generate static documentation for all resources,
this should be placed in a downstream project.

Below is what you would add to the <plugins> section under <build>.
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>2.8</version>
        <executions>
          <execution>
            <id>unpack-static-documentation</id>
            <goals>
              <goal>unpack-dependencies</goal>
            </goals>
            <phase>generate-resources</phase>
            <configuration>
              <outputDirectory>${project.build.directory}/generated-resources/swagger-api-documentation</outputDirectory>
              <includeArtifactIds>sal-rest-docgen</includeArtifactIds>
              <includes>**/explorer/css/**/*, **/explorer/images/**/*, **/explorer/lib/**/*, **/explorer/static/**/*,</includes>
              <excludeTransitive>true</excludeTransitive>
              <ignorePermissions>false</ignorePermissions>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.opendaylight.yangtools</groupId>
        <artifactId>yang-maven-plugin</artifactId>
        <version>${yangtools.version}</version>
        <dependencies>
          <dependency>
            <groupId>org.opendaylight.yangtools</groupId>
            <artifactId>maven-sal-api-gen-plugin</artifactId>
            <version>${yangtools.version}</version>
            <type>jar</type>
          </dependency>
          <dependency>
            <groupId>org.opendaylight.yangtools</groupId>
            <artifactId>yang-binding</artifactId>
            <version>${yangtools.version}</version>
            <type>jar</type>
          </dependency>
          <dependency>
            <groupId>org.opendaylight.controller</groupId>
            <artifactId>sal-rest-docgen</artifactId>
            <version>${mdsal.version}</version>
            <type>jar</type>
          </dependency>
        </dependencies>
        <executions>
          <execution>
            <goals>
              <goal>generate-sources</goal>
            </goals>
            <configuration>
              <yangFilesRootDir>src</yangFilesRootDir>
              <codeGenerators>
                <generator>
                  <codeGeneratorClass>org.opendaylight.controller.sal.rest.doc.impl.StaticDocGenerator</codeGeneratorClass>
                  <outputBaseDir>${project.build.directory}/generated-resources/swagger-api-documentation/explorer/static</outputBaseDir>
                </generator>
              </codeGenerators>
              <inspectDependencies>true</inspectDependencies>
            </configuration>
          </execution>
        </executions>
      </plugin>

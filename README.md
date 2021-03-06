
## JAVA ANNOTATION PROCESSOR TO GENERATE WIKI DOCUMENTATION FROM JAX-RS ANNOTATION

<a href="http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22jaxrs-wiki-processor%22"><img src="https://img.shields.io/maven-central/v/org.bsc/jaxrs-wiki-processor.svg"></a>&nbsp;<img src="https://img.shields.io/github/forks/bsorrentino/maven-annotation-plugin.jaxrs-wiki-processor.svg">&nbsp;<img src="https://img.shields.io/github/stars/bsorrentino/maven-annotation-plugin.jaxrs-wiki-processor.svg">&nbsp;<a href="https://github.com/bsorrentino/maven-annotation-plugin.jaxrs-wiki-processor/issues"><img src="https://img.shields.io/github/issues/bsorrentino/maven-annotation-plugin.jaxrs-wiki-processor.svg"></a>

**JAXRS-WIKI-PROCESSOR** is the simplest and developer friendly plugin to generates wiki document from RestApis using the **JAX-RS ANNOTATION**.

> Just follow the coding standards, we will take care of the project documentation for you
> By default it will be generated using [Confluence Wiki Notation Guide](http://bsorrentino.github.io/maven-confluence-plugin/Notation%20Guide%20-%20Confluence.html)


### Supported platform

* Java 1.7 and higher

### Procedure

* Setup `${java.home}` variable
* Include processor plugin as given below in the **pom.xml** of the RestAPIs module. This is used to make processor **org.bsc.jaxrs.JAXRSWikiProcessor** available to maven.

 ```xml
  <build>
  <plugin>
  <groupId>org.bsc.maven</groupId>
  <artifactId>maven-processor-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>

      <id>process</id>
      <goals>
          <goal>process</goal>
      </goals>
      <configuration>
          <failOnError>false</failOnError>
          <processors>
            <!-- list of processors to use -->
            <processor>org.bsc.jaxrs.JAXRSWikiProcessor</processor>
          </processors>

          <options>
            <!-- Confluence html sample input template (fileName can be changed) -->
            <templateUri>file:///Users/username/RestApiInputTemplate.txt</templateUri>
            <!-- Confluence wiki format Rest APIs documentation output (fileName can be changed) -->
            <filepath>test.confluence</filepath>
          </options>
      </configuration>
    </execution>
  </executions>
  </plugin>
  </plugins>
  </build>
 ```

* Add below dependency on pom.xml to identify the processor class.

 ```xml
    <dependency>
        <groupId>org.bsc</groupId>
        <artifactId>jaxrs-wiki-processor</artifactId>
        <!-- This can be changed based on current version -->
        <version>2.1</version>
    </dependency>
 ```

* Create sample RestApiInputTemplate.txt for your project based on the documentation requirement. If not default template will be used. More information on the template configuration can be found below.

* Run `mvn clean install` on the module where pom.xml is updated.

* Verify for target folder generated under the module. test.confluence file will be available under path
```{module-name}/target/generated-sources/apt/test.confluence```

* Now `test.confluence` file can be uploaded using
   [confluence-reporting-maven-plugin](https://github.com/bsorrentino/maven-confluence-plugin) to confluence page when ever needed.


### Input Template Sample:

Below is the sample RestApiInputTemplate.txt with all the tags that can be used.

```
{toc}

h1.REST APIs:
\\
<!-- $BeginBlock services --> *${service.class.name}*:
h2. ${service.name}
| Description: | ${service.description} |
| *Since:* |  ${service.since} |
| *Notes:* |  ${service.notes} |
| *Security:* |  ${service.security} |
| URL: |  ${service.path} |
| HTTP METHOD: | ${service.verb} |
| Consumes: | ${service.consumes} |
| Produces: | ${service.produces} |
| Response: | ${service.responsetype} {quote} | ${service.response} | {quote}|
| SuccessCode: | ${service.return} |
| Exceptions: | ${service.exception} |
| Input Parameters: | {quote}
|| name: || type || default ||
<!-- $BeginBlock parameters -->| ${param.name} | ${param.type} | ${param.default} |
<!-- $EndBlock parameters -->
{quote}|
| PS: | {quote} | ${service.see} | {quote}|
\\
----
<!-- $EndBlock services -->
\\
\\
\\
\\
```

### Explanation on tags used in the input template:

#### Tags based on Rest method declarations:

 variable | description
 -------- | -----------
 `${service.class.name}` |
 `${service.name}` |
 `${service.description}` |
 `${service.path}` |
 `${service.verb}` |
 `${service.consumes}` |
 `${service.produces}` |
 `${service.responsetype}` |
 `${service.response}` |
 `${service.exception}`  | if method thows exceptions
 `${param.name}` |
 `${param.type}` |
 `${param.default}` |

#### Tags based on Documentation Annotations:

variable | description
-------- | -----------
 `${service.exception}` | if method documentation comments contains `@exception`
 `${service.return}` | if method documentation comments contains `@return`
 `${service.since}` | if method documentation comments contains `@since`
 `${service.notes}` | if method documentation comments contains `@deprecated`
 `${service.see}`  | if method documentation comments contains `@see`
 `${service.security}` |


### Pros:

Developer can concentrate on development with out worring about documentation.
Any changes in the APIs are taken care automatically in the documentation.
Documentation would be up-to-date.
No complexity in setting this up.
reusable.
simple configuration.
can be included in existing applications as well, NO complexity involved.
This plugin automatically gets the documentation information from the method declared, no need to add any specific documentation comments for this.

### Might be Con:

Currently supported for below annotations...
`javax.ws.rs.GET`, `javax.ws.rs.PUT`, `javax.ws.rs.POST`, `javax.ws.rs.DELETE`
(If needed more can be added based on the requirement)

### Know Issues faced during setup and Guidance on resolving:

#### Error in `tools.jar` dependency or `tools.jar` is not getting loaded by maven.

- Make sure `${java.home}` is setup. Try Using java1.7.x.
If still you are facing issue you can add below maven dependency based on java path in your system.

 ```xml
  <dependency>
      <groupId>jdk</groupId>
      <artifactId>tools</artifactId>
      <version>1.7</version>
      <scope>system</scope>
      <systemPath>/Library/Java/JavaVirtualMachines/jdk1.7.0_67.jdk/Contents/Home/lib/tools.jar</systemPath>
  </dependency>
 ```


#### Processor is not getting executed:

  - Try Changing configuration tags in the plugin based on `maven-processor-plugin` version.
current successful `maven-processor-plugin` version is `3.1.0` as given above in plugin.

  - check for "maven-processor-plugin" in the console. If not found make sure you are running right pom.xml where the plugin is included.

  - check for execution id in the maven-processor-plugin. This id must have process like "<id>process</id>" if you are trying to generate documentation from java source.

#### Below error message in logs

```
source directory [/Users/durgakumari/LatestCode/tpam_new/dhap-third-party-access-manager-service-api/src/test/java] doesn't exist! Processor task will be skipped!
[WARNING] no source file(s) detected! Processor task will be skipped
```

  - It seems you are trying to generate document on compiles classes. IF yes make sure you create test directory under source.
If not, change maven-processor-plugin execution id from "<id>process</id>" to "<id>process-test</id>"

  - Add "<compilerArgument>-proc:none</compilerArgument>" in maven compiler plugin. This make sures class files gets generated if you are running on classes using "process-test".

#### Unknown macro error while uploading to `test.confluence` output file to Confluence.

  - `{sampletext}` might be given in the documentation comments of the API. Try giving space after '{' or before '}' as below...

 ```
  {
    sample
  }
 ```       

> If this doesn't solve the issues, inform us and we will help you :).

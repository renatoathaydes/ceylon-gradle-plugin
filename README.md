# Ceylon Gradle Plugin

This is a Gradle Plugin to make it easy to handle Ceylon projects with Gradle.

Because Ceylon itself manages Ceylon dependencies, this plugin focuses on
managing legacy Java dependencies so that you can get transitive dependencies
resolved by Gradle.

This plugin can be a real lifesaver while the Ceylon ecosystem is still developing and you need 
to rely on Java (and Scala, Groovy, Kotlin)'s libraries!

## Using this plugin

To use this plugin, simply add a Gradle build file to the root of your project
and apply this plugin as shown below:

*build.gradle*
```groovy
buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath "com.athaydes.gradle.ceylon:ceylon-gradle-plugin:1.0"
    }
}

apply plugin: 'com.athaydes.ceylon'
```

You can configure the behaviour of the plugin in the `ceylon` block.

For example:

```groovy
ceylon {
    module = "com.athaydes.maven"
    flatClassPath = true
}
```

The only mandatory field is `module`, which you should set to the name of the Ceylon module.

Other properties are explained in the next sections.

### Tasks

The Ceylon-Gradle Plugin adds the following tasks to your project:

* cleanCeylon - removes output created by the other tasks of this plugin.
* resolveCeylonDependencies - resolves the project's dependencies.
* generateOverridesFile - generates the overrides.xml file.
* importJars - imports transitive dependencies into the `output` repository.
* compileCeylon - compiles the Ceylon module.
* runCeylon - runs the Ceylon module
* testCeylon - runs the tests in the test module

Examples:

#### Check that all dependencies can be resolved by Gradle

```
gradle resolveCeylonDependencies
```

#### Running the Ceylon module with a clean environment

```
gradle cleanCeylon runCeylon
```

#### Compile the Ceylon module and show all INFO log messages

```
gradle compileCeylon --info
```

For even more output, use `--debug`.

> Another useful task that Gradle itself adds by default is `dependencies`, which prints the whole
  dependency tree of your project.

## Configuring the Ceylon build

Most of the configuration of this plugin is done inside the `ceylon` block.

The following properties can be set in the `ceylon` block:

* `ceylonLocation`: (optional) path to the ceylon executable. Set the `CEYLON_HOME` environment variable instead
  to make the build more portable, or do nothing if you use [SDKMAN!](http://sdkman.io/)
  as in that case, the Ceylon location will be found automatically.
* `sourceRoots`: (default: `['source']`) List of directories where the Ceylon source code is located.
* `resourceRoots`: (default: `['resource']`) List of directories where resources are located.
* `output`: (default: `modules`) specifies the output module repository.
* `module`: (**mandatory**) name of the Ceylon module.
* `testModule`: (default: same value as `module`): name of the Ceylon test module.
* `moduleExclusions`: (default: `[]`) name of the modules to remove from the compilation and runtime.
* `overrides`: (default: `'auto-generated/overrides.xml'`) location to store the automatically-generated overrides.xml file.
* `flatClassPath`: (default: `false`) use a flat classpath (like in standard Java), bypassing Ceylon's default module isolation.

An example configuration (using most options above) might look like this:

```groovy
ceylon {
    module = "com.acme.awesome"
    testModule = "test.com.acme.awesome"
    flatClassPath = true
    sourceRoots = ['source', 'src/main/ceylon']
    resourceRoots = ['resource', 'src/main/resources']
    output = 'dist'
    flatClasspath = true
}
```

**See the [project samples](ceylon-gradle-plugin-tests) for working example**

## Handling transitive dependencies

All direct dependencies of your project must be declared in Ceylon `module.ceylon` file.

However, as Ceylon does not automatically resolve transitive Maven dependencies, the Ceylon-Gradle Plugin reads the `module.ceylon`
file and creates an [overrides.xml](http://ceylon-lang.org/documentation/1.2/reference/repository/overrides/) file that
informs Ceylon what those transitive dependencies are.

The dependencies are resolved using Gradle's standard mechanism, so you can use any repository supported by Gradle.

So, for example, supposing you want to use a Java library, say [SparkJava](http://sparkjava.com/),
in your Ceylon project... you would have a `module.ceylon` file similar to this:

```ceylon
native("jvm")
module com.athaydes.sparkweb "1.0.0" {
    import java.base "8";
    shared import "com.sparkjava:spark-core" "2.3";
}
```

Even though Spark has a lot of dependencies itself, you don't need to worry about that as Gradle will figure out
all the transitive dependencies for you.

You can see them if you want, with the `dependencies` task:

```
gradle dependencies
```

Which will print this:

```
ceylonCompile
\--- com.sparkjava:spark-core:2.3
     +--- org.slf4j:slf4j-api:1.7.12
     +--- org.slf4j:slf4j-simple:1.7.12
     |    \--- org.slf4j:slf4j-api:1.7.12
     +--- org.eclipse.jetty:jetty-server:9.3.2.v20150730
     |    +--- javax.servlet:javax.servlet-api:3.1.0
     |    +--- org.eclipse.jetty:jetty-http:9.3.2.v20150730
     |    |    \--- org.eclipse.jetty:jetty-util:9.3.2.v20150730
     |    \--- org.eclipse.jetty:jetty-io:9.3.2.v20150730
     |         \--- org.eclipse.jetty:jetty-util:9.3.2.v20150730
     +--- org.eclipse.jetty:jetty-webapp:9.3.2.v20150730
     |    +--- org.eclipse.jetty:jetty-xml:9.3.2.v20150730
     |    |    \--- org.eclipse.jetty:jetty-util:9.3.2.v20150730
     |    \--- org.eclipse.jetty:jetty-servlet:9.3.2.v20150730
     |         \--- org.eclipse.jetty:jetty-security:9.3.2.v20150730
     |              \--- org.eclipse.jetty:jetty-server:9.3.2.v20150730 (*)
     +--- org.eclipse.jetty.websocket:websocket-server:9.3.2.v20150730
     |    +--- org.eclipse.jetty.websocket:websocket-common:9.3.2.v20150730
     |    |    +--- org.eclipse.jetty.websocket:websocket-api:9.3.2.v20150730
     |    |    +--- org.eclipse.jetty:jetty-util:9.3.2.v20150730
     |    |    \--- org.eclipse.jetty:jetty-io:9.3.2.v20150730 (*)
     |    +--- org.eclipse.jetty.websocket:websocket-client:9.3.2.v20150730
     |    |    +--- org.eclipse.jetty:jetty-util:9.3.2.v20150730
     |    |    +--- org.eclipse.jetty:jetty-io:9.3.2.v20150730 (*)
     |    |    \--- org.eclipse.jetty.websocket:websocket-common:9.3.2.v2015073 (*)
     |    +--- org.eclipse.jetty.websocket:websocket-servlet:9.3.2.v20150730
     |    |    +--- org.eclipse.jetty.websocket:websocket-api:9.3.2.v20150730
     |    |    \--- javax.servlet:javax.servlet-api:3.1.0
     |    +--- org.eclipse.jetty:jetty-servlet:9.3.2.v20150730 (*)
     |    \--- org.eclipse.jetty:jetty-http:9.3.2.v20150730 (*)
     \--- org.eclipse.jetty.websocket:websocket-servlet:9.3.2.v20150730 (*)

ceylonRuntime
No dependencies
```

Luckily, you don't need to figure out all these dependencies yourself!

The auto-generated overrides.xml file for this example will be this one:

```xml
<overrides>
  <artifact groupId='com.sparkjava' artifactId='spark-core' version='2.3' shared='true'>
    <add groupId='org.slf4j' artifactId='slf4j-api' version='1.7.12' shared='true' />
    <add groupId='org.slf4j' artifactId='slf4j-simple' version='1.7.12' shared='true' />
    <add groupId='org.slf4j' artifactId='slf4j-api' version='1.7.12' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-server' version='9.3.2.v20150730' shared='true' />
    <add groupId='javax.servlet' artifactId='javax.servlet-api' version='3.1.0' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-http' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-util' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-io' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-util' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-webapp' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-xml' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-util' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-servlet' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-security' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-server' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-server' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-common' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-api' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-util' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-io' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-client' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-util' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-io' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-common' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-servlet' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-api' version='9.3.2.v20150730' shared='true' />
    <add groupId='javax.servlet' artifactId='javax.servlet-api' version='3.1.0' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-servlet' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty' artifactId='jetty-http' version='9.3.2.v20150730' shared='true' />
    <add groupId='org.eclipse.jetty.websocket' artifactId='websocket-servlet' version='9.3.2.v20150730' shared='true' />
  </artifact>
</overrides>
```

### Excluding transitive dependencies

If you don't want all of transitive dependencies to be added to your project, you can remove them by specifying them
as usual with Gradle and adding exclusions.

For example, continuing the example above, supposing that you don't want the websocket dependencies
of Jetty because you're not going to use websockets, you can declare the dependency on Spark **also** in the Gradle file,
as follows:

```groovy
dependencies {
    ceylonCompile "com.sparkjava:spark-core:2.3", {
        exclude group: 'org.eclipse.jetty.websocket'
    }
}
```

> Notice that you still must declare a dependency on `spark-core` in the `module.ceylon` file even if you add that
  dependency on the Gradle build file.
  
It's important to understand that direct dependencies should be declared in `module.ceylon`, as with any Ceylon project.
The Gradle `dependencies` should be used only to add more information to how you want transitive dependencies to be
handled, as in this example. This avoids tightly coupling your project to Gradle (once you have the overrides.xml file
generated by Gradle, your project does not need anything else from Gradle and can be run by `ceylon` as usual)
and confusion regarding where dependencies should be declared!



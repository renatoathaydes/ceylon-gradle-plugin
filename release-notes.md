1.2.0 - 2016 June 19

* issue #9: tap into Gradle lifecycle tasks such as assemble, build and check
* issue #11: validate Ceylon Location before accepting it
* issue #12: Ceylon module may include literal String for docs

1.1.2 - 2016 Mar 06

* new task: compileCeylonTest makes managing tests compilation and running much better.
* new properties to configure Ceylon test sources/resources locations independently from main resources.
* new task: createJavaRuntime which builds the runtime and bash/bat scripts to run Ceylon modules with the java command.
* new property to allow the declaration of a module entry point, ie. the main runnable function.
* documented Eclipse integration.

1.1.1 - 2016 Feb 28

* ceylon.bat is found automatically in Windows
* run ceylon process with the current environment rather than an empty environment

1.1 - 2016 Feb 08

* added support for multi-modules Gradle projects
* Ceylon projects may depend on Gradle modules written in any JVM language
* made it possible to use Jars from a locally created standard Maven repository instead of importing the Jar into Ceylon.
* use Java flat classpath by default. Do not try to import Jars into Ceylon repo by default.
* renamed 'flatClassPath' to 'flatClasspath'.
* moved location of all output files to under the project buildDirectory, except Ceylon modules
  (to keep the Ceylon default location).
* added property to allow printing Ceylon commands instead of running them

1.0 - 2015 Nov 21

* First Ceylon Gradle Plugin release.
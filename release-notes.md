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
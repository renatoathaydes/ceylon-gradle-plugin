## Ceylon SpringBoot Demo

This is a [Spring Boot](http://projects.spring.io/spring-boot/) Demo written in Ceylon.

The code is at [run.ceylon](source/com/athaydes/springboot/run.ceylon).

To run it:

```
gradle runCeylon
```

Then hit `http://localhost:8080` with your browser. You should see the `Hello World!` message.

> Due to the large number of dependencies Spring Boot requires, running this for the first time will take a long time
  while Gradle downloads all these dependencies and install them into the Ceylon repository.
  Running again later should take just a second or so!

### Logging conflict issue

SpringBoot uses logback by default... this conflicts with Ceylon's own logging library, `org.slf4j.simple`, causing
SpringBoot to complain on startup.

To fix this, the current solution is to remove `org.sfl4j.simple` from the Ceylon classpath. It's ugly but it works.

Find your Ceylon repo directory and type something like this:

```
mv repo/org/slf4j/simple/ repo/org/slf4j/simple_
```

Then run your application again... it should work now.

Remember to undo it later to avoid issues with the Ceylon runtime:

```
mv repo/org/slf4j/simple_ repo/org/slf4j/simple
```

# Ceylon Gradle Plugin

> This project is under development and is not easy to be used yet!

This is a simple plugin to make it easy to handle Ceylon projects with Gradle.

Because Ceylon itself manages Ceylon dependencies, this plugin focuses on
managing legacy Java dependencies so that you can get transitive dependencies
resolved by Gradle.

This plugin is a real lifesaver while the Ceylon ecosystem is still developing and you need 
to rely on Java (and Scala, Groovy, Kotlin)'s libraries!

## Using this plugin

To use this plugin, simply add a gradle build file to the root of your project
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
    modules = "com.athaydes.maven"
    flatClassPath = true
}
```

The only mandatory field is `modules`, which is a list of the Ceylon modules
in the current project. Other properties are explained in the next sections.

TODO ...
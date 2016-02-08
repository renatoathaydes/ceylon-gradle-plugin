## Multi-modules Gradle Project Demo

If you have 2 or more Ceylon projects with inter-dependencies, you must declare the dependency of one project on
another in the Gradle build file.

In this Demo, there are 2 modules:

* `:multi-modules-sample:module1`
* `:multi-modules-sample:module2`

`module2` depends on `module1`.

To build both project, from this directory, run the following command:

```
gradle compileCeylon
```

After that, to run `module2`, run:

```
gradle :multi-modules-sample:module2:runCeylon
```

`module1` is not runnable.

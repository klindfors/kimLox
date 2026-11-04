# kimLox
This is a project of exploring interpreters during my exchange semester. I made my own dialect of Robert Nystrom's Lox language, with the purpose of simulating waterflow in river systems with dams, rainfall and terrain with different levels of water retainment. One year after the course I fixed some bugs, added a visualisation with JavaFX and a Maven wrapper. The version handed in for the course is tagged ht25.

## Run
Requires Java 21+. Run from the repo root (on Windows, use .\mvnw.cmd instead of ./mvnw).

Visualisation with JavaFX:
```
./mvnw javafx:run
```

Text mode:

```
./mvnw compile
java -cp target/classes com.craftinginterpreters.lox.FlowMain lule.water
```

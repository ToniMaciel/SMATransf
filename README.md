# nimrod-transformations

##Generating Testability Transformation Jar File
To generate the jar file, run on terminal: `mvn clean compile assembly:single`. 

As a result, the file `testability-transformations-1.0-jar-with-dependencies.jar` will be created.

##Applying the testability transformations:
To apply the testability transformations, once you have the jar file, run the command: 
`java -cp testabilityJarFileLocalPath org.Transformations targetClassLocalPath`

For example:
`java -cp /home/transformations/testability-transformations-1.0-jar-with-dependencies.jar org.Transformations "/home/toy-project/src/main/java/org/Person.java"`
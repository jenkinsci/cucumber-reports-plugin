# Pipeline samples

These basic projects show how to use the cucumber-reports-plugin in both scripted and declarative jenkins pipelines.
They serve as documentation on how this plugin can be used in pipeline builds.
The projects use java/maven to generate a dummy cucumber report. 

Continue reading if you want to execute these samples.

## Requirements

* A Jenkins Server running at least 2.0, 2.30 or higher recommended
* A JDK installation under global tools, named 'JDK8'
* A maven installation under global tools, named 'M3'
* Cucumber-reports-plugin 3.9.0 or higher installed
* A repository to host the sample code

## Running the sample code

Just take one of the samples, declarative or scripted, and host it in a new repository.
Create a new jenkins pipeline job and select 'pipeline script from SCM' as pipeline definition.
Change settings according to where you hosted these samples. 

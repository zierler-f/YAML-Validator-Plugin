# YAML Validator Plugin

## Introduction

The YAML Validator Plugin was designed to help you find errors in your YAML files, before your program reaches development stages. This way annoying YAML errors during program startup can be prevented.

## Installation

### Gradle 1.x and 2.0

```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.at.zierler:yaml-validator-plugin:1.5.0"
  }
}

apply plugin: "at.zierler.yamlvalidator"
```

### Gradle 2.1 and higher

```groovy
plugins {
  id "at.zierler.yamlvalidator" version "1.5.0"
}
```

## Usage

After including the plugin in your `build.gradle` like above, the YAML check can be run with: `gradle validateYaml`.

### Check task

When the java plugin, or any other plugin which introduces a `check` task, is included the `validateYaml` task will automatically run during the `check` task.

### Automatically run during other tasks

To run the `validateYaml` automatically during a task other than `check`, you can add the following to your `build.gradle`:

    <any-existing-task>.dependsOn validateYaml

### Configuration

The following are all possible configuration options:

<table border="0">
	<tr>
		<th>Name</th>
		<th>Default value</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>searchPaths</td>
		<td>['src/main/resources/']</td>
		<td>Array of all directories which should be searched for YAML files, or direct paths to single YAML files.</td>
	</tr>
	<tr>
		<td>allowDuplicates</td>
		<td>false</td>
		<td>Allow YAML files, which contain a duplicate key when checking.</td>
	</tr>
	<tr>
		<td>searchRecursive</td>
		<td>false</td>
		<td>Search directories defined in `searchPaths` recursively.</td>
	</tr>
</table>

Those are the configuration options, as in an `build.gradle` file, with their default values:

```
yamlValidator {
    searchPaths = ['src/main/resources/']
    allowDuplicates = false
    searchRecursive = false
}
```

### Logging

All messages are logged in logging level INFO and higher by default. To see all outputs please use: `gradle validateYaml --info`.

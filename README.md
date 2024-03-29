***This code has been been merged back into [bazel-eclipse](https://github.com/salesforce/bazel-eclipse).***
No further development activity will happen here.

----

[![](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![](https://img.shields.io/badge/license-EPL%202-blue.svg)](http://www.eclipse.org/legal/epl-v20.html)

B2Eclipse
===========================

This is the set of Eclipse plugins for developing Bazel projects in Eclipse. Currently supports running in a language server mode only (through the [Eclipse JDT LS](https://github.com/eclipse/eclipse.jdt.ls)).

Repository structure
--------------------
- **com.salesforce.b2eclipse.jdt.ls** - the code which does all that Bazel-related stuff for building classpaths before they can be further processed by JDT.
- **com.salesforce.b2eclipse.repository** - here you can find the outcome of the build (packaged plugins).
- **com.salesforce.b2eclipse.tests** - integration tests for the jdt.ls plugin.
- **com.salesforce.b2eclipse.ui** - a plugin for the client (the one which is connected to the language server for code interactions). It's rather a simple connection provider to a language server, not a full-fledged UI plugin.

Properties
----------
The following properties are passed from the client to configure the plugin:
- `java.import.bazel.enabled` - whether Bazel support should be enabled in JDT LS (disabled by default);
- `java.import.bazel.src.path` - location of sources (relative to a package). Defaults to `/src/main/java`;
- `java.import.bazel.test.path` - location of test sources (relative to a package). Defaults to `/src/test/java`.

Assumptions
-----------
- Root package is not allowed (i.e. there should be no BUILD file beside the WORKSPACE);
- A project has to be in a valid state (i.e. no unsatisfied  dependencies are allowed);
- Each package is to have sources;
- Sources of a package are expected to be under the path specified through the following properties: `java.import.bazel.src.path`, `java.import.bazel.test.path` (details in the [Properties](#properties) section).
- Sources of a package cannot reside in the root of it. They should be in a subdir;
- Subpackages are not supported (i.e. package within package).

Future vision
-------------
Will be shipped separately as two independent features: standalone feature and as extension for Eclipse JDT LS.

![Vision diagram](../assets/images/vision-diagram.png?raw=true)

Building from the command line
----------------------------

The following command will install [Apache Maven](https://maven.apache.org/) if necessary, then build the server into the  `/com.salesforce.b2eclipse.repository/target/repository` folder:
```bash
    $ ./mvnw clean verify
````
Note: currently, the build can only run when launched with JDK 8.

Features
--------------
* Code completion
* Code navigation

License
-------
This work is dual-licensed under EPL 2.0 and Apache License 2.0. See corresponding files:
[EPL2.0](LICENSE-EPL2.0.txt), [Apache License 2.0](LICENSE-APACHE2.0.txt).

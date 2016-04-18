## Development of Apache Quarks

*Apache Quarks is an effort undergoing incubation at The Apache Software Foundation (ASF), sponsored by the Incubator PMC. Incubation is required of all newly accepted projects until a further review indicates that the infrastructure, communications, and decision making process have stabilized in a manner consistent with other successful ASF projects. While incubation status is not necessarily a reflection of the completeness or stability of the code, it does indicate that the project has yet to be fully endorsed by the ASF.*

This describes development of Apache Quarks itself, not how to develop Quarks applications.
 * See http://quarks.incubator.apache.org/docs/quarks-getting-started for getting started using Quarks

The Quarks community welcomes contributions, please *Get Involved*!
 * http://quarks.incubator.apache.org/docs/community

### Setup

Once you have forked the repository and created your local clone you need to download
these additional development software tools.

* Java 8 - The development setup assumes Java 8 and Linux. 
* Apache Ant 1.9.4: The build uses Ant, the version it has been tested with is 1.9.4. - https://ant.apache.org/
* JUnit 4.10: Java unit tests are written using JUnit, tested at version 4.10. - http://junit.org/
* Jacoco 0.7.5: The JUnit tests have code coverage enabled by default, using Jacoco, tested with version 0.7.5. - http://www.eclemma.org/jacoco/

The Apache Ant `build.xml` files are setup to assume that the Junit and Jacoco jars are copied into `$HOME/.ant/lib`.
```
> ls $HOME/.ant/lib
jacocoagent.jar  jacocoant.jar  junit-4.10.jar
```

These are optional

* Android SDK - Allows building of Android specific modules. Set the environment variable `ANDROID_SDK_PLATFORM` to
the location of the Android platform so that ${ANDROID_SDK_PLATFORM}/android.jar points to a valid jar.


### Building

The primary build process is using Ant, any pull request is expected to maintain
the build success of `clean, all, test`.

The top-level Ant file `quarks/build.xml` has these main targets:

* `all` (default) : Build all code and Javadoc into `target`. The build will fail on any code error or Javadoc warning or error.
* `clean` : Clean the project
* `test` : Run the JUnit tests, if any test fails the test run stops. Takes around five minutes.
* `reports` : Generate JUnit and Code Coverage reports, use after executing the `test` target.
* `release` : Build a release bundle, that includes subsets of the Quarks jars that run on Java 7 (`target/java7`) and Android (`target/android`).

The build process has been tested on Linux and MacOSX.

To build on Windows probably needs some changes, please get involved and contribute them!

**Continuous Integration**

When a pull request is opened on the GitHub mirror site, the Travis CI service runs a full build.

The latest build status for the project's branches can be seen at: https://travis-ci.org/apache/incubator-quarks/branches

The build setup is contained in `.travis.yml` in the project root directory.
It includes:
* Building the project
* Testing on Java 8 and Java 7
Not all tests may be run, some tests are skipped due to timing issues or if excessive setup is required.

If your test may randomly fail because for example it depends on publicly available test services, 
or is timing dependent, and if timing variances on the Travis CI servers may make it more likely
for your tests to fail, you may disable the test from being executed on Travis CI using the 
following statement:
```
    @Test
    public void testMyMethod() {
        assumeTrue(!Boolean.getBoolean("quarks.build.ci"));
        // your test code comes here
        ...
    }
```

Closing ant reopening a pull request will kick off a new build against the pull request.

### Test reports

Running the reports target produces two reports:
* `reports/junit/index.html` - JUnit test report
* `reports/coverage/index.html` - Code coverage report.

### Code Layout

The code is broken into a number of projects and modules within those projects defined by directories under `quarks`.
Each top level directory is a project and contains one or more modules:

* `api` - The APIs for Quarks. In general there is a strict split between APIs and
implementations to allow multiple implementations of an API, such as for different device types or different approaches.
* `spi` - Common implementation code that may be shared by multiple implementations of an API.
There is no requirement for an API implementation to use the provided spi code.
* `runtime` - Implementations of APIs for executing Quarks applications at runtime.
Initially a single runtime is provided, `etiao` - *EveryThing Is An Oplet* -
A micro-kernel that executes Quarks applications by being a very simple runtime where all
functionality is provided as *oplets*, execution objects that process streaming data.
So a Quarks application becomes a graph of connected oplets, and items such as fan-in or fan-out,
metrics etc. are implemented by inserting additional oplets into the application's graph.
* `providers` - Providers bring the Quarks modules together to allow Quarks applications to
be developed and run.
* `connectors` - Connectors to files, HTTP, MQTT, Kafka, JDBC, etc. Connectors are modular so that deployed
applications need only include the connectors they use, such as only MQTT. Quarks applications
running at the edge are expected to connect to back-end systems through some form of message-hub,
such as an MQTT broker, Apache Kafka, a cloud based IoT service, etc.
* `apps` - Applications for use in an Internet of Things environment.
* `analytics` - Analytics for use by Quarks applications.
* `utils` - Optional utilities for Quarks applications.
* `console` - Development console that allows visualization of the streams within an Quarks application during development.
* `samples` - Sample applications, from Hello World to some sensor simulation applications.
* `android` - Code specific to Android.
* `test` - SVT

### Use of Java 8 features
Quarks primary development environment is Java 8, to take advantage of lambda expressions
since Quarks' primary api is a functional one.

**However** in order to support Android (and Java 7) other features of Java 8 are not used in the core
code. Lambdas are translated into Java 7 compatible classes using retrolambda.

Thus:
* For core code that needs to run on Android:
   * the only Java 8 feature that can be used is lambda expressions.
   * JMX functionality cannot be used.
* For test code that tests core code that runs on Android
   * Java 8 lambda expressions can be used
   * Java 8 default & static interface methods
   * Java 8 new classes and methods cannot be used.
   
In general most code is expected to work on Android (but might not yet) with the exception:
* Functionality aimed at the developer environment, such as console and development provider
* Any JMX related code.

### The ASF / Github Integration

The Quarks code is in ASF resident git repositories:

    https://git-wip-us.apache.org/repos/asf/incubator-quarks.git

The repositories are mirrored on github:

    https://github.com/apache/incubator-quarks

Use of the normal github workflow brings benefits to the team including 
lightweight code reviewing, automatic regression tests, etc 
for both committers and non-committers.  

For a description of the github workflow see:

    https://guides.github.com/introduction/flow/
    https://guides.github.com/activities/hello-world/

In summary:
* fork the incubator-quarks github repository
* clone your fork, use lightweight per-task branches, and commit / push changes to your fork
  * descriptive branch names are good. You can also include a reference
    to the Jira issue.  e.g., mqtt-ssl-quarks-100 for issue QUARKS-100
* when ready, create a pull request.  Committers will get notified.
  * include QUARKS-XXXX (the Jira issue) in the name of your pull request
  * for early preview / feedback, create a pull request with [WIP] in the title.  
    Committers won’t consider it for merging until after [WIP] is removed.

Since the github incubator-quarks repository is a mirror of the ASF repository,
the usual github based merge workflow for committers isn’t supported.

Committers can use one of several ways to ultimately merge the pull request
into the repo at the ASF. One way is described here:

* http://mail-archives.apache.org/mod_mbox/incubator-quarks-dev/201603.mbox/%3C1633289677.553519.1457733763078.JavaMail.yahoo%40mail.yahoo.com%3E

Notes with the above PR merge directions:
  * use an https url unless you have a ssh key setup at github:
    `git remote add mirror https://github.com/apache/incubator-quarks.git`

### Using Eclipse

The repo contains Eclipse project definitions for the top-level directories that
contain code, such as api, runtime, connectors.

Using the plugin Eclipse Git Team Provider allows you to import these projects
into your Eclipse workspace directly from your fork.

1. From the File menu select Import
1. From the Git folder select Projects from Git and click Next
1. Select Clone URI and click Next
1. Under Location enter the URI of your fork (the other fields will be filled in automatically) and click Next
1. If required, enter your passphrase to unlock you ssh key
1. Select the branch, usually master and click Next
1. Set the directory where your local clone will be stored and click Next (the directory quarks under this directory is where you can build and run tests using the Ant targets)
1. Select Import existing Eclipse projects and click Next
1. In the Import Projects window, make sure that the Search for nested projects checkbox is selected. Click Finish to bring in all Quarks projects

The project `_quarks` exists to make the top level artifacts such as 
`build.xml` manageable via Eclipse.  Unfortunately folders for the
other projects (e.g., `api`) also show up in the `_quarks` folder and
are best ignored.

Note. Specifics may change depending on your version of Eclipse or the Eclipse Git Team Provider.


# smack-sint-server-extensions

Additional tests that are server-oriented to add to those in [Smack's Integration Test Framework](https://download.igniterealtime.org/smack/dailybuilds/sinttest-javadoc/org/igniterealtime/smack/inttest/package-summary.html).

## Run tests

These tests require **Java 8 or later** to execute.

The tests from this project can be executed using any of the following methods.

### From a released artifact

This project releases artifacts that are self-contained, executable JAR files. The file name format of these files is:
`smack-sint-server-extensions-<version>-jar-with-dependencies.jar`

When executing the JAR file, the tests are run.

To configure the tests, the [framework properties](https://download.igniterealtime.org/smack/dailybuilds/sinttest-javadoc/org/igniterealtime/smack/inttest/package-summary.html) can be provided as standard Java system properties.

For example, to run PubSub-specific tests against a service named `example.org`, using an admin account to create test users, with security disabled, this command can be invoked:

```bash
java -Dsinttest.adminAccountUsername=admin \
     -Dsinttest.adminAccountPassword=admin \
     -Dsinttest.securityMode=disabled \
     -Dsinttest.enabledTests=PubSubIntegrationTest` \
     smack-sint-server-extensions-1.0.0-jar-with-dependencies.jar
```

### From source code, using an IDE

This is an example Run/Debug configuration (which you can use in Intellij):

- *Main class*: `org.igniterealtime.smack.inttest.SmackIntegrationTestFramework`
- *VM options*: `-Dsinttest.service=example.org -Dsinttest.adminAccountUsername=admin -Dsinttest.adminAccountPassword=admin -Dsinttest.securityMode=disabled -Dsinttest.enabledTests=PubSubIntegrationTest`

Running a configuration like this will make the PubSubIntegrationTest tests run against an XMPP server running on the XMPP domain 'example.org', using an administrator account that has as username/password: admin/admin.

This is the most basic configuration that you can use to run tests against a locally installed Openfire server that is running in 'demoboot' mode.

### From source code, on the command line

To run the tests directly from the source code, edit the pom.xml to match your settings, then run `mvn exec:java`

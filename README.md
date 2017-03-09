# Introduction

Quickstart that shows how to generate a simple greenfield (“Hello World”) Restful Service and run it top of openshift.
It contains a rest endpoint which is used to verify a JDBC connection.

You can perform this task in three different ways:

1. Build and launch using WildFly Swarm.
2. Build and deploy using OpenShift.
3. Build, deploy, and authenticate using OpenShift Online.

# Prerequisites

To get started with this quickstart you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.2.x 
[oc][3] | OpenShift Client Tools | v3.3.x
[git][4] | Git version management | 2.x

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

In order to build and deploy this project, you must have an account on an OpenShift Online (OSO): https://console.dev-preview-int.openshift.com/ instance.

# OpenShift Online

1. Go to [OpenShift Online](https://console.dev-preview-int.openshift.com/console/command-line) to get the token used by the oc client for authentication and project access. 

2. On the oc client, execute the following command to replace MYTOKEN with the one from the Web Console:

    ```
    oc login https://api.dev-preview-int.openshift.com --token=MYTOKEN
    ```

3. Start the PostgreSQL database on Openshift

  ```
  oc new-app -e POSTGRESQL_USER=swarm,POSTGRESQL_PASSWORD=password,POSTGRESQL_DATABASE=fruits openshift/postgresql-92-centos7
  ```

4. Use the Fabric8 Maven Plugin to launch the S2I process on the OpenShift Online machine & start the pod.

    ```
    mvn clean fabric8:deploy -Popenshift  -DskipTests
    ```
    
5. Get the route url.

    ```
    oc get route/swarm-rest-jdbc
    NAME              HOST/PORT                                          PATH      SERVICE                TERMINATION   LABELS
    swarm-rest-jdbc   <HOST_PORT_ADDRESS>             swarm-rest-jdbc:8080
    ```

6. Access the web interface to modify the data in the postgres database.
    ```
    http://<HOST_PORT_ADDRESS>/
    ```

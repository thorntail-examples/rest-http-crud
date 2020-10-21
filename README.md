# Thorntail REST HTTP CRUD Example

## Purpose

This example demonstrates how to persist data to a SQL database and expose them via a simple RESTful API.

## Prerequisites

* Log into an OpenShift cluster of your choice: `oc login ...`.
* Select a project in which the services will be deployed: `oc project ...`.

## Deployment

Run the following commands to configure and deploy the applications.

### Deployment using S2I

```bash
oc apply -f ./.openshiftio/service.database.yaml

oc apply -f ./.openshiftio/application.yaml
oc new-app --template=thorntail-rest-http-crud
```

### Deployment with the JKube Maven Plugin

```bash
oc apply -f ./.openshiftio/service.database.yaml

mvn clean oc:deploy -Popenshift
```

## Test everything

This is completely self-contained and doesn't require the application to be deployed in advance.
Note that this may delete anything and everything in the OpenShift project.

```bash
mvn clean verify -Popenshift,openshift-it
```

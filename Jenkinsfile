node("launchpad-maven") {
  checkout scm
  stage("Provision Database") {
    sh "if ! oc get service my-database | grep my-database; then oc new-app -p POSTGRESQL_USER=luke -p POSTGRESQL_PASSWORD=secret -p POSTGRESQL_DATABASE=my_data -p DATABASE_SERVICE_NAME=my-database --name=my-database --template=postgresql-ephemeral; fi"
  }
  stage("Build and Deploy") {
    sh "mvn fabric8:deploy -Popenshift -DskipTests"
  }
}

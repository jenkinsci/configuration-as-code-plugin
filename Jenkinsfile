node('linux') {

  checkout scm
  infra.runMaven(['clean'])

  infra.runMaven(['help:effective-pom'])
  infra.runMaven(['help:effective-settings'])
}

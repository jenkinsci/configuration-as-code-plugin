node('linux') {

  infra.runMaven('clean install -DskipTests -U', 8);
  infra.runMaven('help:effective-pom', 8);
  infra.runMaven('help:effective-settings', 8);

}

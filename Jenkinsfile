node('linux') {

  runMaven('clean install -DskipTests -U', 8);
  runMaven('help:effective-pom', 8);
  runMaven('help:effective-settings', 8);

}

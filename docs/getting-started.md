---
layout:            jenkins-coco-plugin
organization:      Praqma
repo:              jenkins-coco-plugin
github-issues:     true
javadoc:           false
---

## Getting started

### Plugin development (& so far for demo purpose)
To start a plugin
1. clone the repository `git clone git@github.com:Praqma/jenkins-coco-plugin.git`
2. `cd jenkins-coco-plugin`
3. make sure maven is installed and added to path, e.g. `export PATH=/opt/apache-maven-3.5.0/bin:$PATH`
4. create directory for configuration files `mkdir work/conf`
5. copy example configuration files `cp src/main/resources/examples/* work/conf/`
6. edit the files (explained [here](FIXME))
7. run Jenkins with the plugin `mvn hpi:run`

If you're doing it for the first time, you'll find a password - that is required to configure Jenkins - printed in the terminal.

Once it's done you can access Jenkins via browser `localhost:8080/jenkins`
Provide given password, install suggested plugins & create admin account.

Once you're logged in you can see in *Manage Jenkins* that is is configured according to you're version of configuration files.

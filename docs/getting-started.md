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
1. clone the repository
2. make sure maven is installed and added to path, e.g. `export PATH=/opt/apache-maven-3.5.0/bin:$PATH`
3. run Jenkins with the plugin `mvn hpi:run`

If you're doing it for the first time, you'll find a password - that is required to configure Jenkins - printed in the terminal.

Once it's done you can access Jenkins via browser `localhost:8080/jenkins`

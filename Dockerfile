FROM csanchez/jenkins-kubernetes:1.2

RUN /usr/local/bin/install-plugins.sh job-dsl:1.66 workflow-multibranch:2.9.2

COPY target/configuration-as-code.hpi /usr/share/jenkins/ref/plugins/configuration-as-code.hpi

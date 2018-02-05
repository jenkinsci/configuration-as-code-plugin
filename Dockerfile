FROM jenkins/jenkins:lts-alpine

# kubernetes plugin is optional
RUN /usr/local/bin/install-plugins.sh job-dsl:1.66 workflow-multibranch:2.9.2 workflow-aggregator:2.5 kubernetes:1.2

COPY target/configuration-as-code.hpi /usr/share/jenkins/ref/plugins/configuration-as-code.hpi

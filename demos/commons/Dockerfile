FROM jenkins/jenkins:lts
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    CASC_JENKINS_CONFIG="/casc_configs/config.yaml"
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN  echo 'configuration-as-code:latest' | cat /usr/share/jenkins/ref/plugins.txt - | /usr/local/bin/install-plugins.sh
COPY config.yaml /casc_configs/config.yaml

FROM jenkins/jenkins:lts-alpine

COPY --chown=jenkins target/configuration-as-code.hpi /usr/share/jenkins/ref/plugins/configuration-as-code.jpi
RUN echo $JENKINS_VERSION > /usr/share/jenkins/ref/jenkins.install.UpgradeWizard.state
RUN echo $JENKINS_VERSION > /usr/share/jenkins/ref/jenkins.install.InstallUtil.lastExecVersion

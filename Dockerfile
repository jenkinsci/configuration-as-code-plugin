FROM maven:3.5-jdk-8-alpine AS build
COPY pom.xml /work/pom.xml
WORKDIR /work
RUN mvn dependency:go-offline
COPY . /work
RUN mvn clean install

# --- to be distributed as jenkins/jenkins:lts-alpine-casc
FROM jenkins/jenkins:lts-alpine

COPY --chown=jenkins --from=build /work/target/configuration-as-code.hpi /usr/share/jenkins/ref/plugins/configuration-as-code.jpi
RUN echo $JENKINS_VERSION > /usr/share/jenkins/ref/jenkins.install.UpgradeWizard.state
RUN echo $JENKINS_VERSION > /usr/share/jenkins/ref/jenkins.install.InstallUtil.lastExecVersion

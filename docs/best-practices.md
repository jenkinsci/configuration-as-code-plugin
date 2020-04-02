# How to get started with JCasC using Docker

The first step would be to spin up a jenkins instance using its image

```
docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts
```

Once the container is up, navigate to http://localhost:8080 to open the Jenkins user interface (UI) in your browser. It will ask you for a password—look at the logs from the docker run command, and you should see something like this in your terminal:

```
Jenkins initial setup is required. An admin user has been created and a password generated.
Please use the following password to proceed to installation:
 
63b9bde2015f4aedb75b93ec088e86ca
 
This may also be found at: /var/jenkins_home/secrets/initialAdminPassword
```

First, we need to install the  plugin itself, of course - we can find it in the available plugins list.After the installation, we can see `Configuration-as-Code` available in the `/manage` page.Clicking the link gives us the JCasC configuration screen, where we can provide a path or URL to our configuration path or export the existing configuration.
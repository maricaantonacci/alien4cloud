![Alien4Cloud](https://raw.githubusercontent.com/alien4cloud/alien4cloud.github.io/sources/images/alien4cloud-banner.png)

[Website](http://alien4cloud.github.io) |
[Community](http://alien4cloud.github.io/community/index.html) |
[Roadmap](http://alien4cloud.github.io/roadmap/index.html) |
[Documentation](http://alien4cloud.github.io/#/documentation/2.0.0/index.html) |
[Twitter](https://twitter.com/alien4cloud) |
[Release notes](http://alien4cloud.github.io/#/release_notes/index.html)


ALIEN 4 Cloud stands for Application LIfecycle ENablement for Cloud.

FastConnect started this project in order to help enterprises adopting the cloud for their new and existing applications in an Open way. A4C has an Open-Source model (Apache 2 License) and standardization support in mind.

## Building Alien4Cloud

Alien4Cloud is written in java for the backend and requires a JDK 8 or newer (note that we test it using JDK 8 only for now).

- make sure you have a JDK 8 installed
- make sure you have Maven installed (team is using 3.0.5)
- install Ruby
- install Python
- install Node.js (team is using 6.14.4) to get npm command. Check here http://nodejs.org. Note that you need a recent version of npm (>= 5.5.x) in order to build a4c.
- install bower  
```sh
$ sudo npm install -g bower
```
- install grunt  
```sh
$ sudo npm -g install grunt-cli
```
- install compass  
```sh
$ gem install compass
```
- and grunt-contrib-compass  
```sh
$ npm install grunt-contrib-compass --save-dev
```  

run the folowing command to build the project:  
```sh
$ mvn clean install -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
```
## Running Alien4Cloud

- launch the backend
```sh
$ cd alien4cloud-ui
$ mvn spring-boot:run
```
- launch the frontend
```sh
$ cd alien4cloud-ui
$ grunt serve
```

## OpenID Connect and DEEP orchestrator integration
- In the OpenID Connect server, there must be a client registered with the following callback URL: ```http://<a4c_location>/auth/oidc``` where ```a4c_location``` is the location in which A4C is running. For example: ```localhost:9999``` or ```127.0.0.1:9999``` when testing during development.

- Download and install the Spring Social OIDC project locally
```sh
$ git clone https://github.com/indigo-dc/spring-social-oidc
$ cd spring-social-oidc
$ mvn clean install
```

- Activate the ```oidc-auth``` profile in the ```alien4cloud-config.yml``` file
```yaml
spring:
  profiles:
    active: oidc-auth
```

- Configure the OpenID Connect client parameters in the ```alien4cloud-config.yml``` file
```yaml
oidc:
  iam:
    issuer: <issuer_url>
    client-id: <client_id>
    client-secret: <client_secret>
  roles: <user_roles>
```
where
 
- ```issuer_url``` is the URL to the OIDC authentication provider 
- ```client_id``` is the client identifier configured in the OIDC server
- ```client_secret``` is the secret for the above client identifier
- ```user_roles``` is a comma separated list of the Alien 4 Cloud roles that each user authenticated by OIDC will have in the Alien4Cloud instance. It accepts any role described in [the A4C documentation](https://alien4cloud.github.io/#/documentation/2.1.0/concepts/roles.html): that is ```ADMIN```, ```COMPONENTS_MANAGER```, ```ARCHITECTS``` and ```APPLICATIONS_MANAGER```

- Once Alien4Cloud is running, you should see a button in the UI header with the text OpenID Connect authentication. Clicking on it will trigger the authentication flow.

For the deep orchestrator integration you need to have also the following properties:

```yaml
deep:
  orchestrator:
    keystore:
      location: <cert_keystore_location>
      password: <cert_keystore_password>
```

where

- ```cert_keystore_location``` is the location of a JKS keystore containing the certificate used by the orchestrator endpoint.
- ```cert_keystore_password``` is the password for the above keystore, if any.

## Accessing the OIDC token from inside an Alien4Cloud plug-in or module

- If you are developing a plug in, include the spring-social-oidc dependency in your project

```xml
<dependency>
    <groupId>org.springframework.social</groupId>
    <artifactId>spring-social-oidc</artifactId>
    <version>1.4</version>
</dependency>
```

- Add a reference to the ```ConnectionRepository``` class in the file that you want to get the current access token
```java
@Inject
private ConnectionRepository connRepository;
```

- Access the tokens with the following snippet:
```java
connRepository.getPrimaryConnection(Oidc.class).createData()
```

Inside the returned object, you will have both the access and refresh token for the current session.
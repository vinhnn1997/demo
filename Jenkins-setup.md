# Jenkins deployment setup

## Jenkins agent

Install Java 21, Maven, Docker CLI/daemon, Git and an SSH client on the Jenkins agent. The Jenkins user must be allowed to run Docker commands.

## Credentials

Create these Jenkins credentials:

- `docker-registry`: Username/password or token for the container registry.
- `deployment-ssh`: SSH private key for the deployment user.
- `maven-settings`: Secret file containing a Maven `<server>` entry whose ID is `common-library` and read/deploy credentials for the Maven repository.

The pipeline defaults to `ghcr.io`; set the `REGISTRY` and `IMAGE_REPOSITORY` job parameters when using another registry.
Set `COMMON_MAVEN_REPOSITORY_URL` to the Maven repository URL. Use `PUBLISH_COMMON_LIBRARY` when publishing a new common-library version. Set `BUILD_SERVICE` to one module to test, build, push, and deploy only that service; `all` runs the complete stack build/deployment.

On the first release, publish common-library once before building consumers. The `maven-settings` secret file should contain credentials for server ID `common-library`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
	<servers>
		<server>
			<id>common-library</id>
			<username>repository-user</username>
			<password>repository-token</password>
		</server>
	</servers>
</settings>
```

Each service pins its own common version in `<common-library.version>` in its POM. To release a common change, increment `<version>` in `common-library/pom.xml`, publish it with `PUBLISH_COMMON_LIBRARY`, and update the version property only in services that should consume it. The common artifact is built once and uploaded to the Maven repository; service Docker builds resolve it as a dependency and do not compile common-library source.

## Deployment server

1. Install Docker Engine and Docker Compose plugin.
2. Clone the repository into the configured `DEPLOY_PATH`.
3. Copy `.env.production.example` to `.env.production`.
4. Replace every placeholder secret in `.env.production`.
5. Make sure the deployment user can run Docker.

The Jenkins job parameters must include the deployment server host, user, path, image repository and environment. Production requires a manual approval step.

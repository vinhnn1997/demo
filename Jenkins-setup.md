# Jenkins deployment setup

## Jenkins agent

Install Java 21, Maven, Docker CLI/daemon, Git and an SSH client on the Jenkins agent. The Jenkins user must be allowed to run Docker commands.

## Credentials

Create these Jenkins credentials:

- `docker-registry`: Username/password or token for the container registry.
- `deployment-ssh`: SSH private key for the deployment user.

The pipeline defaults to `ghcr.io`; set the `REGISTRY` and `IMAGE_REPOSITORY` job parameters when using another registry.

## Deployment server

1. Install Docker Engine and Docker Compose plugin.
2. Clone the repository into the configured `DEPLOY_PATH`.
3. Copy `.env.production.example` to `.env.production`.
4. Replace every placeholder secret in `.env.production`.
5. Make sure the deployment user can run Docker.

The Jenkins job parameters must include the deployment server host, user, path, image repository and environment. Production requires a manual approval step.

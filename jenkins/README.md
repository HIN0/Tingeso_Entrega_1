# Local Jenkins (CI/CD)

Runs the pipeline defined in [`../Jenkinsfile`](../Jenkinsfile):
checkout from GitHub → backend tests (JUnit + JaCoCo ≥ 90%) → build and push both images to Docker Hub
(`izkybin/toolrent-backend` and `izkybin/toolrent-frontend`, tagged `:<build number>` and `:latest`).

Jenkins runs in a container and builds the images with the Docker engine of the host (shared socket).

## 1. Start Jenkins

```bash
docker compose -f jenkins/compose.yml up -d --build
```

Open http://localhost:8081. Configuration, jobs and credentials are stored in the `jenkins_home` volume.

## 2. First-time setup

1. Initial admin password:
   ```bash
   docker exec toolrent-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
2. **Select plugins to install → None** (the required plugins are already in the image).
3. Create the admin user and keep the default Jenkins URL.

## 3. Docker Hub credential

Docker Hub → *Account settings → Personal access tokens* → create a token (Read & Write).

Jenkins → *Manage Jenkins → Credentials → System → Global credentials → Add credentials*:

| Field    | Value                          |
|----------|--------------------------------|
| Kind     | Username with password         |
| Username | Docker Hub user (`izkybin`)    |
| Password | the access token               |
| ID       | `dockerhub-id` (used by the Jenkinsfile) |

## 4. Pipeline job

*New Item* → name `toolrent` → **Pipeline** → OK:

- *Pipeline → Definition*: **Pipeline script from SCM**
- *SCM*: Git — Repository URL `https://github.com/HIN0/Tingeso_Entrega_1.git`
  (public repository: no credentials needed)
- *Branch Specifier*: `*/main`
- *Script Path*: `Jenkinsfile`

Save → **Build Now**. Follow the run in *Console Output*.

## 5. Deploy the published images

```bash
docker compose pull
docker compose up -d
```

## Notes

- The container runs as `root` only to access the Docker socket; this setup is meant for local development.
- Stop Jenkins: `docker compose -f jenkins/compose.yml down` (add `-v` to also delete its configuration).

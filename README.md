# Spring Boot Kubernetes and MySQL Example

This Spring Boot application exposes the original greeting endpoint and a small
JDBC-backed messages API. It uses an in-memory H2 database for local development
and switches to MySQL when the `kubernetes` Spring profile is active.

## API

| Method | Path | Operation |
| --- | --- | --- |
| `GET` | `/` | Original greeting |
| `GET` | `/api/messages` | List messages |
| `GET` | `/api/messages/{id}` | Read one message |
| `POST` | `/api/messages` | Create a message |
| `PUT` | `/api/messages/{id}` | Update a message |
| `DELETE` | `/api/messages/{id}` | Delete a message |
| `GET` | `/actuator/health` | Application and database health |

Example requests:

```bash
curl http://localhost:8080/

curl -X POST http://localhost:8080/api/messages \
  -H 'Content-Type: application/json' \
  -d '{"content":"Hello from Kubernetes"}'

curl http://localhost:8080/api/messages

curl -X PUT http://localhost:8080/api/messages/1 \
  -H 'Content-Type: application/json' \
  -d '{"content":"Updated message"}'

curl -i -X DELETE http://localhost:8080/api/messages/1
```

## Build and run locally

Java 17 is required. The default profile uses H2 in MySQL compatibility mode,
so no local database setup is required.

```bash
./mvnw clean package
./mvnw spring-boot:run
```

To run the packaged JAR instead:

```bash
java -jar target/spring-boot-complete-0.0.1-SNAPSHOT.jar
```

## Build and run with the Kubernetes profile

Spring profiles are selected when the application starts, not while the JAR is
compiled. Build the same deployable JAR, then activate the `kubernetes` profile
when starting it. The following example assumes that `mysql-service` is
reachable from the environment where the application runs:

```bash
./mvnw clean package

export SPRING_PROFILES_ACTIVE=kubernetes
export SPRING_DATASOURCE_URL='jdbc:mysql://mysql-service:3306/spring_boot_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='springapp'
export SPRING_DATASOURCE_PASSWORD='replace-me'

java -jar target/spring-boot-complete-0.0.1-SNAPSHOT.jar
```

For development, run the application directly through Maven with the same
profile and datasource environment variables:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=kubernetes
```

Do not use `-Pkubernetes`: that syntax selects a Maven build profile, while
`kubernetes` in this project is a Spring runtime profile. In Kubernetes, the
Deployment sets `SPRING_PROFILES_ACTIVE=kubernetes` and reads the database
credentials from a Secret, as shown below.

## Run locally against MySQL

Create the `spring_boot_db` database and an application user, then override the
standard Spring datasource variables:

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/spring_boot_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='springapp'
export SPRING_DATASOURCE_PASSWORD='replace-me'
./mvnw spring-boot:run
```

`src/main/resources/schema.sql` creates the `messages` table on startup. For a
larger production application, replace this simple initializer with a migration
tool such as Flyway or Liquibase.

## Kubernetes MySQL configuration

Do not connect to a MySQL Pod IP because Pod addresses change. Connect through
the MySQL Service name. If the application and MySQL are in the same namespace,
`mysql-service` is sufficient. From another namespace, use
`mysql-service.<namespace>.svc.cluster.local`.

The `kubernetes` profile defaults to this URL:

```text
jdbc:mysql://mysql-service:3306/spring_boot_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

Create the credentials as a Secret. Replace the example values before applying
it:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysql-app-credentials
type: Opaque
stringData:
  username: springapp
  password: replace-me
```

Add these environment variables and health probes to the Spring Boot
Deployment:

```yaml
spec:
  template:
    spec:
      containers:
        - name: webapp
          image: nexus:5000/spring-boot-complete:latest
          ports:
            - name: http
              containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: kubernetes
            - name: SPRING_DATASOURCE_URL
              value: jdbc:mysql://mysql-service:3306/spring_boot_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: mysql-app-credentials
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-app-credentials
                  key: password
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            initialDelaySeconds: 20
            periodSeconds: 20
```

If MySQL is a sidecar in the same Pod rather than a separate Deployment, use
`127.0.0.1` in `SPRING_DATASOURCE_URL`. A dedicated MySQL workload and Service
is normally easier to scale and maintain.

Apply and verify the workloads:

```bash
kubectl apply -f mysql-secret.yaml
kubectl apply -f mysql-deployment.yaml
kubectl apply -f mysql-service.yaml
kubectl apply -f webapp-deployment.yaml

kubectl rollout status deployment/mysql
kubectl rollout status deployment/webapp
kubectl get pods,svc
kubectl logs deployment/webapp
```

From inside the cluster, confirm the database Service resolves before debugging
the application:

```bash
kubectl exec deployment/webapp -- getent hosts mysql-service
```

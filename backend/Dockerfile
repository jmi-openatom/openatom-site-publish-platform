FROM eclipse-temurin:21-jdk-noble AS builder

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-noble

ARG NODE_VERSION=20.19.4
ARG TARGETARCH

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl xz-utils \
    && case "${TARGETARCH:-amd64}" in \
         amd64) NODE_ARCH="x64" ;; \
         arm64) NODE_ARCH="arm64" ;; \
         *) echo "Unsupported architecture: ${TARGETARCH}" >&2; exit 1 ;; \
       esac \
    && NODE_PACKAGE="node-v${NODE_VERSION}-linux-${NODE_ARCH}.tar.xz" \
    && curl -fsSLO "https://nodejs.org/dist/v${NODE_VERSION}/${NODE_PACKAGE}" \
    && curl -fsSLO "https://nodejs.org/dist/v${NODE_VERSION}/SHASUMS256.txt" \
    && grep " ${NODE_PACKAGE}$" SHASUMS256.txt | sha256sum -c - \
    && tar -xJf "${NODE_PACKAGE}" -C /usr/local --strip-components=1 \
    && rm -f "${NODE_PACKAGE}" SHASUMS256.txt \
    && apt-get purge -y --auto-remove xz-utils \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 sitepublish \
    && useradd --system --uid 10001 --gid sitepublish --create-home sitepublish \
    && mkdir -p /app/data/storage \
    && chown -R sitepublish:sitepublish /app /home/sitepublish

WORKDIR /app

COPY --from=builder /workspace/target/site-publish-platform-*.jar /app/app.jar

ENV SERVER_PORT=8080 \
    SITE_STORAGE_ROOT=/app/data/storage \
    HOME=/home/sitepublish

USER sitepublish

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=12 \
    CMD curl -fsS http://127.0.0.1:8080/api/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

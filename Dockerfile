FROM maven:3.6.1-jdk-11

COPY pom.xml application.yml /usr/src/bug/
COPY src /usr/src/bug/src

WORKDIR /usr/src/bug

RUN mvn clean package;

RUN set -e; \
    mkdir -p storage; \
    chown 1001:0 storage;

USER 1001

ENTRYPOINT []
CMD ["java", "-Xms64m", "-Xmx64m", "-XX:CompressedClassSpaceSize=64m", "-jar", "target/oombug-1.0-SNAPSHOT.jar"]
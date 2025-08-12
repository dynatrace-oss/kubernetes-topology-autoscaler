ARG PROJ_VERSION

FROM gradle:8.14.3-jdk17 AS build
COPY . /app
WORKDIR /app
RUN gradle clean build -PprojVersion=${PROJ_VERSION}

FROM apache/flink:1.18.1-scala_2.12
WORKDIR /opt/flink/lib
# Since we use Flink 1.18, we copy the JAR to lib and not usrlib, see https://issues.apache.org/jira/browse/FLINK-35554
COPY --from=build --chown=flink:flink /app/build/libs/kta-quickstart-flink.jar /opt/flink/lib/kta-quickstart-flink.jar

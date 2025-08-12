ARG PROJ_VERSION

FROM gradle:8.14.3-jdk17 AS build
COPY . /app
WORKDIR /app
RUN gradle clean shadowJar -PprojVersion=${PROJ_VERSION}

FROM openjdk:17-jdk
WORKDIR /app
COPY --from=build /app/build/libs/kta-quickstart-kstreams-all.jar /app/kta-quickstart-kstreams-all.jar
ENTRYPOINT ["java", "-jar", "kta-quickstart-kstreams-all.jar"]

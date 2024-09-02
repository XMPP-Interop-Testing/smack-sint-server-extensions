# If you're editing this file, you might need to make matching changes to the Containerfile too

FROM eclipse-temurin:17-jdk-noble AS pom

WORKDIR /usr/src
COPY pom.xml .

FROM eclipse-temurin:17-jdk-noble AS build

RUN apt-get update -qq
RUN apt-get install -qqy maven

RUN mkdir /tmp/m2_home
ENV M2_HOME=/tmp/m2_home

WORKDIR /usr/src

COPY --from=pom /usr/src/ .
RUN mvn -e -B dependency:go-offline

COPY . .
RUN mvn -e -B package -Dmaven.test.skip

FROM eclipse-temurin:17-jre-noble

COPY --from=build --chmod=0755 /usr/src/entrypoint.sh /sbin/entrypoint.sh
COPY --from=build /usr/src/target/*-jar-with-dependencies.jar /usr/local/sintse/sintse.jar

LABEL author="Dan Caseley" maintainer="dan@caseley.me.uk"
ENTRYPOINT ["/sbin/entrypoint.sh"]

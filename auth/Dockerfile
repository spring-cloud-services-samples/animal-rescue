FROM openjdk:8-alpine AS build
ENV UAA_VERSION 4.28.0
RUN apk add --no-cache git
RUN git clone https://github.com/cloudfoundry/uaa.git /opt/app
#Pin to UAA v4.28.0
WORKDIR /opt/app
RUN git checkout $UAA_VERSION
ADD V4_21_1_1575556384__add_login_user.sql /opt/app/server/src/main/resources/org/cloudfoundry/identity/uaa/db/hsqldb
RUN ./gradlew assemble -x :cloudfoundry-identity-samples:assemble -x javadoc

FROM tomcat:8-alpine
ENV UAA_VERSION 4.28.0
ENV CATALINA_OPTS "-DLOGIN_CONFIG_URL=file:///opt/app/uaa/src/main/resources/required_configuration.yml \
 -Dlogging.config=/opt/app/uaa/src/main/resources/log4j.properties \
 -Djava.util.logging.config.file=/opt/app/uaa/src/main/resources/java-util-logging.properties"
COPY required_configuration.yml /opt/app/uaa/src/main/resources/required_configuration.yml
COPY java-util-logging.properties /opt/app/uaa/src/main/resources/java-util-logging.properties
COPY log4j.properties /opt/app/uaa/src/main/resources/log4j.properties
COPY --from=build /opt/app/uaa/build/libs/cloudfoundry-identity-uaa-$UAA_VERSION.war /usr/local/tomcat/webapps/uaa.war

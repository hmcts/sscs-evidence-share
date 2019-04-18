FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-2.0.1

COPY build/libs/sscs-evidence-share.jar /opt/app/

CMD ["sscs-evidence-share.jar"]

EXPOSE 8091

FROM azul/zulu-openjdk-alpine:15
COPY target/bot-0.0.1-SNAPSHOT.jar bot.jar
ENTRYPOINT ["java","-jar","/bot.jar"]
FROM azul/zulu-openjdk-alpine:15
COPY build/libs/bot.jar bot.jar
ENTRYPOINT ["java","-jar","/bot.jar"]
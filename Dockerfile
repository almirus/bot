FROM azul/zulu-openjdk-alpine:15
COPY build/libs/bot.jar bot.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/bot.jar"]
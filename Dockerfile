FROM openjdk:8-jdk-alpine
COPY build/libs/bot.jar bot.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/bot.jar"]
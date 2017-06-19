FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/apibot.jar /apibot/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/apibot/app.jar"]

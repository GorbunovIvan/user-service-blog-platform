FROM openjdk

WORKDIR /app

COPY build/libs/user-service-blog-platform-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]

# Run:
#   'docker build -t user-service-blog-platform-image .'
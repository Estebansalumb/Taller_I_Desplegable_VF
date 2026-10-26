# Imagen para desplegar la versión web en Render
FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY lib ./lib
COPY src ./src

RUN mkdir -p bin data \
 && javac -encoding UTF-8 -d bin -cp "lib/*" src/modelo/*.java src/datos/*.java src/presentacion/*.java

ENV PORT=10000
EXPOSE 10000

CMD ["java", "-cp", "bin:lib/*", "presentacion.ServidorWeb"]

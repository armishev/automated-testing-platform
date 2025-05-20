# Указываем базовый образ, содержащий Java Runtime Environment (JRE)
FROM openjdk:21-slim

RUN apt-get update && apt-get install -y --no-install-recommends tzdata && \
    ln -sf /usr/share/zoneinfo/Europe/Moscow /etc/localtime && \
    echo "Europe/Moscow" > /etc/timezone && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Создаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем скомпилированный JAR-файл в контейнер
COPY build/libs/stock-market-api-0.0.1-SNAPSHOT.jar /app/

# Открываем нужный порт (например, 8080 для веб-приложений)
EXPOSE 8080

# Указываем команду для запуска JAR-файла
ENTRYPOINT ["java", "-jar", "/app/stock-market-api-0.0.1-SNAPSHOT.jar"]

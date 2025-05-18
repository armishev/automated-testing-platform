package com.armishev.tvm.telegrambot;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${imagerenderer.url}")
    private String imageRendererUrl;

    @Value("${git.repo.url}")
    private String gitRepoUrl;



    public TelegramBot(@Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            Long chatId = update.getMessage().getChatId();
            String response;

            // Обработка команд
            switch (messageText.toLowerCase()) {
                case "/start":
                    response = "Привет! Я бот для автоматизированного тестирования. Введите /help для получения списка команд.";
                    sendText(chatId, response);
                    break;
                case "/help":
                    response = "Доступные команды:\n" +
                            "/start - начать работу с ботом\n" +
                            "/help - список команд\n" +
                            "/test - выполнить тестовую команду";
                    sendText(chatId, response);
                    break;
                case "/test":
                    response = "Тестовая команда выполнена!"; // Здесь можно добавить вызов сервиса или другую логику
                    sendText(chatId, response);
                    break;
                case "/metrics":
                    try {
                        byte[] imageBytes = getScreenshot("http://147.45.150.56:4000/public-dashboards/9191b094754e459688fa1aaeecb77794 "); // Или получаем URL из сообщения
                        sendPhoto(chatId, imageBytes);
                    } catch (Exception e) {
                        logger.error("Ошибка при получении скриншота: {}", e.getMessage());
                        sendText(chatId, "Произошла ошибка при создании скриншота.");
                    }
                    break;
                case "/addAlert":
                    sendText(chatId, "Добавляю тестовый алерт...");
                    try {
                        addAlertToGitRepo();
                        sendText(chatId, "Алерт добавлен и запушен в Git.");
                    } catch (Exception e) {
                        logger.error("Ошибка при добавлении алерта: {}", e.getMessage());
                        sendText(chatId, "Ошибка при добавлении алерта.");
                    }
                    break;

                default:
                    response = "Неизвестная команда. Введите /help для списка доступных команд.";
                    sendText(chatId, response);
                    break;
            }
        }
    }

    // Метод для отправки текстового сообщения
    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
            logger.info("Отправлено сообщение для chatId {}: {}", chatId, text);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    // Метод для получения скриншота от сервиса image-renderer
    private byte[] getScreenshot(String url) {
        RestTemplate restTemplate = new RestTemplate();
        String requestUrl = UriComponentsBuilder
                .fromHttpUrl(imageRendererUrl)
                .path("/api/render")
                .queryParam("url", url)
                .build()
                .toUriString();
        // Формируем URL запроса к image-renderer, например:
        logger.info("requestUrl: {}", requestUrl);
        // Отправляем GET запрос, ожидаем byte[]
        return restTemplate.getForObject(requestUrl, byte[].class);
    }

    // Метод для отправки фотографии в чат
    private void sendPhoto(Long chatId, byte[] imageBytes) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        // Оборачиваем байты в InputStream и задаем имя файла
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(imageBytes), "screenshot.png"));
        try {
            execute(sendPhoto);
            logger.info("Отправлена фотография для chatId {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Ошибка при отправке фотографии: {}", e.getMessage());
        }
    }

    private void addAlertToGitRepo() throws IOException, GitAPIException {
        File localPath = Files.createTempDirectory("brk-repo").toFile();

        Git git = Git.cloneRepository()
                .setURI(gitRepoUrl)
                .setDirectory(localPath)
                .call();

        File rulesFile = new File(localPath, "alert_rules.yml");

        // Добавляем алерт
        List<String> lines = Files.readAllLines(rulesFile.toPath(), StandardCharsets.UTF_8);
        String newAlert = """
        - alert: DynamicAlertFromBot
          expr: vector(1)
          for: 5s
          labels:
            severity: info
          annotations:
            summary: "Добавлено из Telegram"
            description: "Этот алерт добавлен ботом через /addAlert"
        """;

        int insertIndex = lines.size() - 1;
        lines.add(insertIndex, newAlert);
        Files.write(rulesFile.toPath(), lines, StandardCharsets.UTF_8);

        git.add().addFilepattern("alert_rules.yml").call();
        git.commit().setMessage("Add alert via Telegram Bot").call();
        git.push().call();

        // Удаляем временные файлы
        FileUtils.deleteDirectory(localPath);
    }



}

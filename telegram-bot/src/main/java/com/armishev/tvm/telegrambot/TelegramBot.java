package com.armishev.tvm.telegrambot;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jgit.api.Git;
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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

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
                            "/test - выполнить тестовую команду\n" +
                            "/metrics - получить метрики в виде изображения\n" +
                            "/addalert - добавить тестовый алерт в репозиторий";
                    sendText(chatId, response);
                    break;

                case "/test":
                    response = "Тестовая команда выполнена!";
                    sendText(chatId, response);
                    break;

                case "/metrics":
                    try {
                        byte[] imageBytes = getScreenshot("http://147.45.150.56:4000/public-dashboards/9191b094754e459688fa1aaeecb77794");
                        sendPhoto(chatId, imageBytes);
                    } catch (Exception e) {
                        logger.error("Ошибка при получении скриншота: {}", e.getMessage());
                        sendText(chatId, "Произошла ошибка при создании скриншота.");
                    }
                    break;

                case "/addalert":
                    sendText(chatId, "Добавляю тестовый алерт...");
                    try {
                        addAlertToGitRepo(); // реализуй этот метод ниже
                        sendText(chatId, "✅ Тестовый алерт добавлен и отправлен в Git.");
                    } catch (Exception e) {
                        logger.error("Ошибка при добавлении алерта: {}", e.getMessage());
                        sendText(chatId, "❌ Ошибка при добавлении алерта: " + e.getMessage());
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

    public void addAlertToGitRepo() throws Exception {
        // Настройки
        String repoUrl = gitRepoUrl;
        String alertFilePath = "alert_rules.yml";

        // Клонируем временно
        File repoDir = Files.createTempDirectory("alert-repo").toFile();
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir)
                .call();

        // Читаем YAML
        File alertFile = new File(repoDir, alertFilePath);
        Yaml yaml = new Yaml(new SafeConstructor());
        Map<String, Object> data;

        try (InputStream input = new FileInputStream(alertFile)) {
            data = yaml.load(input);
        }

        if (data == null) data = new HashMap<>();
        List<Map<String, Object>> groups = (List<Map<String, Object>>) data.getOrDefault("groups", new ArrayList<>());

        // Ищем или создаём группу
        Map<String, Object> targetGroup = null;
        for (Map<String, Object> group : groups) {
            if ("BotAlerts".equals(group.get("name"))) {
                targetGroup = group;
                break;
            }
        }
        if (targetGroup == null) {
            targetGroup = new LinkedHashMap<>();
            targetGroup.put("name", "BotAlerts");
            targetGroup.put("rules", new ArrayList<>());
            groups.add(targetGroup);
        }

        List<Map<String, Object>> rules = (List<Map<String, Object>>) targetGroup.get("rules");

        // Добавляем новый алерт
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("alert", "DynamicAlertFromBot");
        alert.put("expr", "vector(1)");
        alert.put("for", "5s");

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("severity", "info");
        alert.put("labels", labels);

        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", "Добавлено из Telegram");
        annotations.put("description", "Этот алерт добавлен ботом через /addalert");
        alert.put("annotations", annotations);

        rules.add(alert);
        data.put("groups", groups);

        // Записываем обратно
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Representer representer = new Representer(options);
        yaml = new Yaml(representer, options);

        try (Writer writer = new FileWriter(alertFile)) {
            yaml.dump(data, writer);
        }

        // Git commit & push
        Git git = Git.open(repoDir);
        git.add().addFilepattern(alertFilePath).call();
        git.commit().setMessage("Добавлен алерт из Telegram бота").call();
        git.push().call();

        // Удаляем временный каталог
        deleteDirectory(repoDir);
    }

    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }




}

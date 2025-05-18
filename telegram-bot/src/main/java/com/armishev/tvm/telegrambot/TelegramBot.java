package com.armishev.tvm.telegrambot;

import java.io.BufferedWriter;
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
import java.util.UUID;

import com.armishev.tvm.telegrambot.models.AlertDraft;
import com.armishev.tvm.telegrambot.models.LoadTestDraft;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
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

    @Value("${git.repo.testproject.url}")
    private String gitRepoTestProjectUrl;

    @Value("${git.repo.atp.url}")
    private String gitRepoATPUrl;

    private final Map<Long, AlertDraft> alertSessions = new HashMap<>();
    private final Map<Long, String> customDashboardUrls = new HashMap<>();
    private final Map<Long, LoadTestDraft> loadTestSessions = new HashMap<>();

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

            if (processLoadTestDraft(messageText, chatId)) {
                return;
            }


            if (processAlertDraft(messageText, chatId)) {
                return;
            }

            if (processMetricsCommand(messageText, chatId)) {
                return;
            }

            handleBotCommand(messageText, chatId);
        }
    }

    private boolean processLoadTestDraft(String messageText, Long chatId) {
        if (messageText.equalsIgnoreCase("/loadtest")) {
            loadTestSessions.put(chatId, new LoadTestDraft());
            loadTestSessions.get(chatId).setStep(1);
            sendText(chatId, "📊 Укажи количество потоков (THREADS):");
            return true;
        }

        LoadTestDraft loadDraft = loadTestSessions.get(chatId);
        if (loadDraft != null) {
            if (messageText.isBlank()) {
                sendText(chatId, "❗ Значение не может быть пустым. Введите снова.");
                return true;
            }

            switch (loadDraft.getStep()) {
                case 1:
                    if (!messageText.matches("\\d+")) {
                        sendText(chatId, "❌ Укажи количество потоков числом. Например: 10");
                        return true;
                    }
                    loadDraft.setThreads(messageText);
                    loadDraft.setStep(2);
                    sendText(chatId, "🌐 Укажи протокол (http/https):");
                    break;
                case 2:
                    if (!messageText.equalsIgnoreCase("http") && !messageText.equalsIgnoreCase("https")) {
                        sendText(chatId, "❌ Поддерживаются только 'http' или 'https'.");
                        return true;
                    }
                    loadDraft.setProtocol(messageText.toLowerCase());
                    loadDraft.setStep(3);
                    sendText(chatId, "📡 Укажи домен или IP:");
                    break;
                case 3:
                    if (!messageText.matches("^[\\w.-]+$")) {
                        sendText(chatId, "❌ Неверный формат домена или IP. Пример: example.com или 192.168.0.1");
                        return true;
                    }
                    loadDraft.setDomain(messageText);
                    loadDraft.setStep(4);
                    sendText(chatId, "📌 Укажи порт:");
                    break;
                case 4:
                    if (!messageText.matches("\\d+")) {
                        sendText(chatId, "❌ Порт должен быть числом. Например: 80");
                        return true;
                    }
                    loadDraft.setPort(messageText);
                    loadDraft.setStep(5);
                    sendText(chatId, "📥 Укажи метод запроса (GET, POST и т.д.):");
                    break;
                case 5:
                    if (!messageText.matches("(?i)GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD")) {
                        sendText(chatId, "❌ Метод запроса должен быть одним из: GET, POST, PUT, DELETE, PATCH, " +
                                "OPTIONS, HEAD");
                        return true;
                    }
                    loadDraft.setMethod(messageText.toUpperCase());
                    loadDraft.setStep(6);
                    sendText(chatId, "📍 Укажи путь и query (например, /api/data?id=1):");
                    break;
                case 6:
                    if (!messageText.startsWith("/")) {
                        sendText(chatId, "❌ Путь должен начинаться с /");
                        return true;
                    }
                    loadDraft.setPath(messageText);
                    loadDraft.setStep(7);
                    sendText(chatId, "⚠️ Подтвердить запуск нагрузки? Напиши `да` для подтверждения или `нет` для " +
                            "отмены.");
                    break;
                case 7:
                    if (messageText.equalsIgnoreCase("да")) {
                        try {
                            String command = buildJMeterCommand(loadDraft);
                            writeAndPushTriggerFile(command);
                            sendText(chatId, "✅ Команда отправлена в Git! CI должен её выполнить.");
                        } catch (Exception e) {
                            sendText(chatId, "❌ Ошибка при формировании команды: " + e.getMessage());
                        }
                    } else {
                        sendText(chatId, "❌ Запуск отменён.");
                    }
                    loadTestSessions.remove(chatId);
                    break;
            }
            return true;
        }
        return false;
    }

    private void handleBotCommand(String messageText, Long chatId) {
        String response;
        switch (messageText.toLowerCase()) {
            case "/start":
                response = "Привет! Я бот для автоматизированного тестирования. Введите /help для получения " +
                        "списка команд.";
                sendText(chatId, response);
                break;

            case "/help":
                response = "Доступные команды:\n" +
                        "/start - начать работу с ботом\n" +
                        "/help - список команд\n" +
                        "/test - выполнить тестовую команду\n" +
                        "/metrics - получить метрики в виде изображения\n" +
                        "/setMetrics <url> - установить ссылку для команды /metrics\n" +
                        "/resetMetrics - сбросить ссылку на метрики до значения по умолчанию\n" +
                        "/addAlert - добавить тестовый алерт в репозиторий\n" +
                        "/listAlerts - получить список алертов\n" +
                        "/loadTest - запустить нагрузочный тест";
                sendText(chatId, response);
                break;

            case "/test":
                response = "Тестовая команда выполнена!";
                sendText(chatId, response);
                break;

            case "/metrics":
                try {
                    String url = customDashboardUrls.getOrDefault(chatId, "http://147.45.150" +
                            ".56:4000/public-dashboards/9191b094754e459688fa1aaeecb77794");
                    byte[] imageBytes = getScreenshot(url);
                    sendPhoto(chatId, imageBytes);
                } catch (Exception e) {
                    logger.error("Ошибка при получении скриншота: {}", e.getMessage());
                    sendText(chatId, "Произошла ошибка при создании скриншота.");
                }
                break;

            case "/listalerts":
                sendText(chatId, "📋 Получаю список алертов...");
                try {
                    String list = listAlertsFromGit();
                    sendText(chatId, list);
                } catch (Exception e) {
                    logger.error("Ошибка при получении алертов: {}", e.getMessage());
                    sendText(chatId, "❌ Ошибка при получении списка алертов.");
                }
                break;

            default:
                response = "Неизвестная команда. Введите /help для списка доступных команд.";
                sendText(chatId, response);
                break;
        }
    }

    private boolean processMetricsCommand(String messageText, Long chatId) {
        if (messageText.toLowerCase().startsWith("/setmetrics")) {
            String[] parts = messageText.split("\\s+", 2);
            if (parts.length < 2) {
                sendText(chatId, "❗ Пожалуйста, укажите ссылку после команды. Пример:\n/setmetrics http://example" +
                        ".com/dashboard");
            } else {
                String url = parts[1];
                if (!url.startsWith("http")) {
                    sendText(chatId, "❌ Это не похоже на корректную ссылку. Попробуйте снова.");
                } else {
                    customDashboardUrls.put(chatId, url);
                    sendText(chatId, "✅ Ссылка на дашборд сохранена!");
                }
            }
            return true;
        }

        if (messageText.equalsIgnoreCase("/resetmetrics")) {
            customDashboardUrls.remove(chatId);
            sendText(chatId, "🔄 Ссылка сброшена на значение по умолчанию.");
            return true;
        }
        return false;
    }

    private boolean processAlertDraft(String messageText, Long chatId) {
        if (messageText.equalsIgnoreCase("/addalert")) {
            alertSessions.put(chatId, new AlertDraft(null, null, null, null, null, null, 1));
            sendText(chatId, "🛠️ Введите название алерта:");
            return true;
        }

        AlertDraft draft = alertSessions.get(chatId);
        if (draft != null) {
            if (messageText.isBlank()) {
                sendText(chatId, "❗ Значение не может быть пустым. Пожалуйста, введите снова.");
                return true;
            }
            switch (draft.getStep()) {
                case 1:
                    draft.setAlertName(messageText + "_" + UUID.randomUUID().toString().substring(0, 5));
                    draft.setStep(2);
                    sendText(chatId, "🔍 Введите PromQL выражение (expr):");
                    break;
                case 2:
                    if (!messageText.matches("^[a-zA-Z_]+\\(.*\\)$")) {
                        sendText(chatId, "❌ Похоже, это не PromQL. Пример: rate(http_requests_total[5m])");
                        return true;
                    }
                    draft.setExpr(messageText);
                    draft.setStep(3);
                    sendText(chatId, "⏱️ Введите длительность (например, 30s):");
                    break;
                case 3:
                    if (!messageText.matches("^\\d+[smhd]$")) {
                        sendText(chatId, "❌ Неверный формат. Пример: 30s, 5m, 1h, 1d");
                        return true;
                    }
                    draft.setDuration(messageText);
                    draft.setStep(4);
                    sendText(chatId, "⚠️ Введите уровень severity (info, warning, critical):");
                    break;
                case 4:
                    String sev = messageText.toLowerCase();
                    if (!List.of("info", "warning", "critical").contains(sev)) {
                        sendText(chatId, "❌ Уровень может быть только: info, warning, critical");
                        return true;
                    }
                    draft.setSeverity(messageText);
                    draft.setStep(5);
                    sendText(chatId, "📝 Введите краткое описание (summary):");
                    break;
                case 5:
                    draft.setSummary(messageText);
                    draft.setStep(6);
                    sendText(chatId, "📄 Введите полное описание (description):");
                    break;
                case 6:
                    draft.setDescription(messageText);
                    sendText(chatId, "✅ Формирую алерт...");
                    try {
                        addAlertToGitRepo(draft);
                        sendText(chatId, "✅ Алерт добавлен и запушен в Git.");
                    } catch (Exception e) {
                        sendText(chatId, "❌ Ошибка при добавлении алерта: " + e.getMessage());
                    }
                    alertSessions.remove(chatId);
                    break;
            }
            return true;
        }
        return false;
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

    public void addAlertToGitRepo(AlertDraft draft) throws Exception {
        String repoUrl = gitRepoTestProjectUrl;
        String alertFilePath = "alert_rules.yml";

        File repoDir = Files.createTempDirectory("alert-repo").toFile();
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir)
                .call();

        File alertFile = new File(repoDir, alertFilePath);
        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        Map<String, Object> data;

        try (InputStream input = new FileInputStream(alertFile)) {
            data = yaml.load(input);
        }

        if (data == null) {
            data = new HashMap<>();
        }
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

        // Формируем алерт из draft
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("alert", draft.getAlertName());
        alert.put("expr", draft.getExpr());
        alert.put("for", draft.getDuration());

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("severity", draft.getSeverity());
        alert.put("labels", labels);

        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", draft.getSummary());
        annotations.put("description", draft.getDescription());
        alert.put("annotations", annotations);

        rules.add(alert);
        data.put("groups", groups);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Representer representer = new Representer(options);
        yaml = new Yaml(representer, options);

        try (Writer writer = new FileWriter(alertFile)) {
            yaml.dump(data, writer);
        }

        Git git = Git.open(repoDir);
        git.add().addFilepattern(alertFilePath).call();
        git.commit().setMessage("Добавлен алерт '" + draft.getAlertName() + "' из Telegram бота").call();
        git.push().call();

        deleteDirectory(repoDir);
    }

    public String listAlertsFromGit() throws Exception {
        String repoUrl = gitRepoTestProjectUrl;
        String alertFilePath = "alert_rules.yml";

        File repoDir = Files.createTempDirectory("alert-list").toFile();
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir)
                .call();

        File alertFile = new File(repoDir, alertFilePath);
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> data;

        try (InputStream input = new FileInputStream(alertFile)) {
            data = yaml.load(input);
        }

        StringBuilder result = new StringBuilder("📊 Список алертов:\n\n");

        List<Map<String, Object>> groups = (List<Map<String, Object>>) data.get("groups");
        for (Map<String, Object> group : groups) {
            List<Map<String, Object>> rules = (List<Map<String, Object>>) group.get("rules");
            for (Map<String, Object> rule : rules) {
                result.append("🚨 ").append(rule.get("alert")).append("\n");
                result.append("🔍 ").append(rule.get("expr")).append("\n");
                Map<String, String> labels = (Map<String, String>) rule.get("labels");
                if (labels != null && labels.get("severity") != null) {
                    result.append("⚠️ Уровень: ").append(labels.get("severity")).append("\n");
                }
                Map<String, String> annotations = (Map<String, String>) rule.get("annotations");
                if (annotations != null && annotations.get("summary") != null) {
                    result.append("📝 ").append(annotations.get("summary")).append("\n");
                }
                result.append("\n");
            }
        }

        deleteDirectory(repoDir);
        return result.toString();
    }

    private String buildJMeterCommand(LoadTestDraft draft) {
        return String.format(
                "docker exec jmeter jmeter -n -t /testplan/template.jmx \\\n" +
                        "  -JTHREADS=%s \\\n" +
                        "  -JDOMAIN=%s \\\n" +
                        "  -JPORT=%s \\\n" +
                        "  -JPROTOCOL=%s \\\n" +
                        "  -JPATH=\"%s\" \\\n" +
                        "  -JMETHOD=%s \\\n" +
                        "  -l /testplan/results.jtl \\\n" +
                        "  -j /testplan/jmeter.log",
                draft.getThreads(), draft.getDomain(), draft.getPort(),
                draft.getProtocol(), draft.getPath(), draft.getMethod()
        );
    }

    private void writeAndPushTriggerFile(String command) throws Exception {
        String repoUrl = gitRepoATPUrl;
        String triggerFilePath = "jmeter/trigger-jmeter.yml";

        File repoDir = Files.createTempDirectory("jmeter-trigger").toFile();
        Git.cloneRepository()
                .setURI(repoUrl)
                .setBranch("develop")
                .setDirectory(repoDir)
                .call();

        File triggerFile = new File(repoDir, triggerFilePath);
        triggerFile.getParentFile().mkdirs();

        try (Writer writer = new BufferedWriter(new FileWriter(triggerFile))) {
            writer.write("# Trigger file for JMeter load test via Telegram bot\n");
            writer.write(command + "\n");
        }

        Git git = Git.open(repoDir);
        git.add().addFilepattern(triggerFilePath).call();
        git.commit().setMessage("Добавлен load test через Telegram Bot").call();
        git.push().setRemote("origin").add("develop").call();

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

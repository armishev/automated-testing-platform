package com.armishev.tvm.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
                    break;
                case "/help":
                    response = "Доступные команды:\n" +
                            "/start - начать работу с ботом\n" +
                            "/help - список команд\n" +
                            "/test - выполнить тестовую команду";
                    break;
                case "/test":
                    response = "Тестовая команда выполнена!"; // Здесь можно добавить вызов сервиса или другую логику
                    break;
                default:
                    response = "Неизвестная команда. Введите /help для списка доступных команд.";
                    break;
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(response);

            try {
                execute(message);
                // Логирование успешной отправки сообщения
                logger.info("Отправлено сообщение для chatId {}: {}", chatId, response);
            } catch (TelegramApiException e) {
                logger.error("Ошибка при отправке сообщения для chatId {}: {}", chatId, e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

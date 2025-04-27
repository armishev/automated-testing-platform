package com.armishev.tvm.imagerenderer;

import com.armishev.tvm.imagerenderer.controllers.ImageRendererController;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.codeborne.selenide.Selenide.*;
@Service
public class SelenideTestRunner {

    private static final Logger logger = LoggerFactory.getLogger(SelenideTestRunner.class);

    public byte[] runTest(String url) throws IOException {
        // Настройка Selenide
        Configuration.remote = "http://localhost:4444/wd/hub"; // URL Selenoid
        Configuration.browser = "firefox"; // Браузер
        Configuration.browserSize = "1920x1080"; // Размер окна браузера
        Configuration.headless = true; // Без GUI (опционально)

        // Открытие страницы
        Selenide.open(url);
        Selenide.sleep(2000); // 2 секунды

        // Сохранение скриншота
        screenshot("page_screenshot");


        // Читаем скриншот в байты и возвращаем
        return FileUtils.readFileToByteArray(new File("build/reports/tests/page_screenshot.png"));
    }
}

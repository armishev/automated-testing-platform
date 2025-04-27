package com.armishev.tvm.imagerenderer.controllers;

import com.armishev.tvm.imagerenderer.SelenideTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/api")
public class ImageRendererController {

    private static final Logger logger = LoggerFactory.getLogger(ImageRendererController.class);

    private final SelenideTestRunner testRunner;

    public ImageRendererController(SelenideTestRunner testRunner) {
        this.testRunner = testRunner;
    }

    @GetMapping(value = "/render", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> renderScreenshot(@RequestParam String url) {
        try {
            byte[] screenshot = testRunner.runTest(url);
            return ResponseEntity.ok(screenshot);
        } catch (IOException e) {
            logger.info("!!!");
            return ResponseEntity.internalServerError().body(null);
        }
    }
}


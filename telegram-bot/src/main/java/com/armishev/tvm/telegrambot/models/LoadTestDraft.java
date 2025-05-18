package com.armishev.tvm.telegrambot.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoadTestDraft {
    private String threads;
    private String protocol;
    private String domain;
    private String port;
    private String method;
    private String path;
    private int step;
}

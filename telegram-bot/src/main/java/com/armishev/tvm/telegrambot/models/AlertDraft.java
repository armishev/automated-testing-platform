package com.armishev.tvm.telegrambot.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlertDraft {
    private String alertName;
    private String expr;
    private String duration;
    private String severity;
    private String summary;
    private String description;
    private int step;
}

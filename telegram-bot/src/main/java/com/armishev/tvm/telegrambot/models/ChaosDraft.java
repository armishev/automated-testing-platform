package com.armishev.tvm.telegrambot.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChaosDraft {
    private Boolean latencyActive;
    private Integer latencyRangeStart;
    private Integer latencyRangeEnd;
    private Boolean exceptionsActive;
    private String exceptionClass;
    private Boolean memoryActive;
    private Integer memoryMillisecondsHold;
    private Boolean cpuActive;
    private Integer cpuMillisecondsHold;
    private Boolean killApplicationActive;
    private Integer level;
    private int step;
}

package com.eletra.business.model.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Log {
    private String id;
    private String sentAt;
    private String message;
    private String format;
}
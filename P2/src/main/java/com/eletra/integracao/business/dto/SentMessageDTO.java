package com.eletra.integracao.business.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SentMessageDTO {
    private String username;
    private String createdAt;
    private String sentAt;
    private String message;
}
package com.eletra.integracao.business.dto;

import com.eletra.integracao.business.entities.Log;
import com.eletra.integracao.business.entities.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ReceivedMessageDTO {
    private User user;
    private Log log;
}

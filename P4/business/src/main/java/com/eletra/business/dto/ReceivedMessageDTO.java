package com.eletra.business.dto;

import com.eletra.business.model.entities.Log;
import com.eletra.business.model.entities.User;
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

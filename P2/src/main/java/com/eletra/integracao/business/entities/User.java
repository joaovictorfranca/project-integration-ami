package com.eletra.integracao.business.entities;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class User {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String employeeCode;
    private String position;
    private String cpf;
}
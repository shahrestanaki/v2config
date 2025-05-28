package com.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * @author m.Shahrestanaki @createDate 5/26/2025
 */
@Setter
@Getter
@JsonIgnoreProperties(value = { "operatorFile" })
public class InputDto {
    private String url;
    private String operator;
    private String operatorFile;

    public void setOperator(String operator) {
        this.operator = operator;
        this.operatorFile = operatorFile + ".txt";
    }
}

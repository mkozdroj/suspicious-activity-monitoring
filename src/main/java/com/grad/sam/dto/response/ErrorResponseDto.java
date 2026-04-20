package com.grad.sam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {

    private LocalDateTime timestamp;

    private Integer status;

    private String errorCode;

    private String message;

    private List<String> details;
}

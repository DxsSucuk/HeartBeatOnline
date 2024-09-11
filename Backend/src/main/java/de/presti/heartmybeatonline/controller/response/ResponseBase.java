package de.presti.heartmybeatonline.controller.response;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ResponseBase<T> {
    public boolean success;
    public String message;
    public T data;
}

package de.presti.heartmybeatonline.controller;

import com.google.gson.JsonObject;
import de.presti.heartmybeatonline.controller.response.ResponseBase;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.WebRequest;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    /**
     * Error Attributes in the Application
     */
    private final ErrorAttributes errorAttributes;

    /**
     * Controller for the Error Controller
     * @param errorAttributes Attributes that give more Info about the Error.
     */
    public ErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /**
     * Request mapper for errors.
     * @return {@link String} for Thyme to the HTML Page.
     */
    @GetMapping(value = "/error")
    public ResponseBase<String> error(WebRequest webRequest) {
        ResponseBase<String> response = new ResponseBase<>();
        response.success = false;
        response.message = "Gang you retarded.";
        return response;
    }
}

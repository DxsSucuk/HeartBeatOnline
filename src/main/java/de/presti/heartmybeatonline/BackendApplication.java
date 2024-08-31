package de.presti.heartmybeatonline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(BackendApplication.class, args);
        new Server();
        Server.getInstance().setAuthToken(args[0]);
    }

}

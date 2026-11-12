package bd.bracuinsiders;

import bd.bracuinsiders.config.EditorCommand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewsroomApplication {
    public static void main(String[] args) throws java.sql.SQLException {
        if (args.length == 1 && args[0].equals("--set-editor")) {
            EditorCommand.run();
            return;
        }
        SpringApplication.run(NewsroomApplication.class, args);
    }
}

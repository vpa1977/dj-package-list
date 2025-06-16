package org.debian.checkdepends;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class CheckDependenciesApplication {

    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);

    @Value("${server.port}")
    private int serverPort;

	public static void main(String[] args) throws IOException, InterruptedException {
        // TODO: use Spring Shell
        if (args.length > 0) {
            if (!args[0].equals("maven-check")) {
                System.out.println("Unknown command "+ args[0]);
                System.exit(-1);
            }
            System.out.println("Check project dependencies "+ args[0]);
            if (args.length  < 3) {
                System.out.println("Usage: <project directory> <maven-command>");
                return;
            }
            new MavenDependencyChecker().run(args);
            return;
        }
		SpringApplication.run(CheckDependenciesApplication.class, args);
	}

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            ProcessBuilder pb = new ProcessBuilder("xdg-open", "http://localhost:"+ serverPort);
            var proc = pb.start();
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.warn(e.getMessage());
        }
    }

}

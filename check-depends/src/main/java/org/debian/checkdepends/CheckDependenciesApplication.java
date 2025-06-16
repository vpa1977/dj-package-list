package org.debian.checkdepends;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class CheckDependenciesApplication {

    @Value("${server.port}")
    private int serverPort;

	public static void main(String[] args) {
		SpringApplication.run(CheckDependenciesApplication.class, args);
	}

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder("xdg-open", "http://localhost:"+ serverPort);
        var proc = pb.start();
        proc.waitFor();
    }

}

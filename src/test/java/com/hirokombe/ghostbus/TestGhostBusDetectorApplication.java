package com.hirokombe.ghostbus;

import org.springframework.boot.SpringApplication;

public class TestGhostBusDetectorApplication {

    public static void main(String[] args) {
        SpringApplication.from(GhostBusDetectorApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

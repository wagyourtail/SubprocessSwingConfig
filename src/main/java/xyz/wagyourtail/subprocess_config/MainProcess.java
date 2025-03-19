package xyz.wagyourtail.subprocess_config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import xyz.wagyourtail.subprocess_config.settings.DynamicSettings;
import xyz.wagyourtail.subprocess_config.settings.DynamicSettingsPanel;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainProcess {

    public static String getJava() {
        return ProcessHandle.current().info().command().orElse("java");
    }

    public static String getClasspath() {
        return System.getProperty("java.class.path");
    }

    public static CompletableFuture<?> openConfig(DynamicSettings config) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonWriter w = new JsonWriter(new OutputStreamWriter(baos))) {
            config.serialize(w);
        }
        ProcessBuilder pb = new ProcessBuilder(getJava(), "-cp", getClasspath(), "xyz.wagyourtail.subprocess_config.SubProcess", config.getClass().getCanonicalName(), baos.toString());
        Process p = pb.start();

        return CompletableFuture.runAsync(() -> {
            try (BufferedReader r = p.inputReader()) {
                String line;
                while((line = r.readLine()) != null) {
                    try (JsonReader reader = new JsonReader(new StringReader(line))) {
                        config.deserialize(reader);
                    }
                    System.out.println("Recieved settings: " + line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
//        return CompletableFuture.completedFuture(null);
    }

    public static void main(String[] args) throws Exception {
        ExampleSettings settings = new ExampleSettings();

        while (true) {
            var future = openConfig(settings);

            System.out.println("program opened, current settings: ");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(new OutputStreamWriter(baos))) {
                settings.serialize(w);
            }
            System.out.println(baos.toString());

            System.out.println("Waiting for settings...");

            future.get();

            System.out.println("program closed, current settings: ");
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            try (JsonWriter w = new JsonWriter(new OutputStreamWriter(baos2))) {
                settings.serialize(w);
            }
            System.out.println(baos2.toString());
        }
    }

}

package xyz.wagyourtail.subprocess_config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import xyz.wagyourtail.subprocess_config.settings.DynamicSettings;
import xyz.wagyourtail.subprocess_config.settings.DynamicSettingsPanel;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;

public class SubProcess {

    public static void send(DynamicSettings settings, DynamicSettingsPanel panel) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonWriter w = new JsonWriter(new OutputStreamWriter(baos))) {
            panel.save();
            settings.serialize(w);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(baos.toString());
    }

    public static void main(String[] args) throws Exception {
        DynamicSettings settings = (DynamicSettings) Class.forName(args[0]).newInstance();
        try (JsonReader r = new JsonReader(new StringReader(args[1]))) {
            settings.deserialize(r);
        }

        JFrame window = new JFrame();
        DynamicSettingsPanel panel = new DynamicSettingsPanel(settings);
        window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.Y_AXIS));
        window.add(panel);

        JButton button = new JButton("Save");
        button.addActionListener(e -> send(settings, panel));
        window.add(button);

//        JButton button2 = new JButton("Close");
//        button2.addActionListener(e -> window.dispose());
//        window.add(button2);

        window.pack();
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        window.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                send(settings, panel);
            }
        });

    }

}

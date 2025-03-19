package xyz.wagyourtail.byteedit.decompilers.ui.settings;

import net.lenni0451.commons.swing.GBC;
import org.slf4j.Logger;
import xyz.wagyourtail.byteedit.util.Log;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class DynamicSettingsPanel extends JPanel {

    private static final Logger LOGGER = Log.get();

    public static JFrame createSettingsFrame(final Component parent, final String name, final DynamicSettings config, final BiFunction<JFrame, DynamicSettingsPanel, JPanel> buttons) {
        JFrame frame = new JFrame("Settings");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new GridBagLayout());

        DynamicSettingsPanel settingsPanel = new DynamicSettingsPanel(config);
        int gridy = 0;
        GBC.create(frame).grid(0, gridy++).anchor(GBC.CENTER).add(new JLabel("Settings for " + name));
        GBC.create(frame).grid(0, gridy++).weightx(1).fill(GBC.HORIZONTAL).add(new JSeparator());
        GBC.create(frame).grid(0, gridy++).weight(1, 1).fill(GBC.BOTH).add(() -> {
            JScrollPane scrollPane = new JScrollPane(settingsPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
            return scrollPane;
        });
        GBC.create(frame).grid(0, gridy).weightx(1).fill(GBC.HORIZONTAL).add(() -> buttons.apply(frame, settingsPanel));

        frame.pack();
        frame.setSize(Math.min(500, frame.getWidth()), Math.min(500, frame.getHeight()));
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(parent);
        return frame;
    }


    private final DynamicSettings settings;
    private final List<SettingPanel<?>> settingPanels = new ArrayList<>();

    public DynamicSettingsPanel(DynamicSettings settings) {
        this.settings = settings;
        this.init();
    }

    public void init() {
        this.setLayout(new GridBagLayout());
        int gridy = 0;
        for (DynamicSettings.Setting<?> setting : this.settings.getSettings()) {
            this.add(setting, gridy++);
        }
        GBC.fillVerticalSpace(this);
    }

    public void save() {
        for (SettingPanel<?> settingPanel : this.settingPanels) settingPanel.save();
    }

    public void add(final DynamicSettings.Setting<?> setting, final int gridy) {
        SettingPanel<?> settingPanel = switch (setting.getClass().getSimpleName()) {
            case "BooleanSetting" -> new BooleanSettingPanel((DynamicSettings.BooleanSetting) setting, this, gridy);
            case "PrimitiveSetting" -> new PrimitiveSettingPanel<>((DynamicSettings.PrimitiveSetting<?>) setting, this, gridy);
            case "BoundedIntSetting" -> new BoundedIntSettingPanel((DynamicSettings.BoundedIntSetting) setting, this, gridy);
            case "BoundedDoubleSetting" -> new BoundedDoubleSettingPanel((DynamicSettings.BoundedDoubleSetting) setting, this, gridy);
            case "StringSetting" -> new StringSettingPanel((DynamicSettings.StringSetting) setting, this, gridy);
            case "CharSetting" -> new CharSettingPanel((DynamicSettings.CharSetting) setting, this, gridy);

//            case "ListSetting":
//                this.add(new ListSettingPanel((DynamicSettings.ListSetting<?, ?>) setting));
//                break;
//            case "MapSetting":
//                this.add(new MapSettingPanel((DynamicSettings.MapSetting<?, ?>) setting));
//                break;
            default -> {
                LOGGER.warn("Unknown setting type: {}", setting.getClass().getSimpleName());
                yield null;
            }
        };
        if (settingPanel != null) this.settingPanels.add(settingPanel);
    }

    public static abstract class SettingPanel<T extends DynamicSettings.Setting<?>> {
        protected final T setting;

        public SettingPanel(T setting, JPanel panel, int gridy) {
            this.setting = setting;
            GBC.create(panel).grid(0, gridy).insets(5, 5, 0, 0).anchor(GBC.LINE_START).add(new JLabel(this.setting.getName()));
        }

        public abstract void save();
    }

    public static class BooleanSettingPanel extends SettingPanel<DynamicSettings.BooleanSetting> {
        private final JCheckBox field;

        public BooleanSettingPanel(DynamicSettings.BooleanSetting setting, JPanel panel, int gridy) {
            super(setting, panel, gridy);
            this.field = new JCheckBox();
            this.field.setSelected(this.setting.get());
            this.field.addActionListener(e -> this.setting.set(this.field.isSelected()));
            GBC.create(panel).grid(1, gridy).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.field);
        }

        @Override
        public void save() {
            this.setting.set(this.field.isSelected());
        }
    }

    public static class PrimitiveSettingPanel<T extends Number> extends SettingPanel<DynamicSettings.PrimitiveSetting<T>> implements DocumentListener {
        private final JTextField field;

        public PrimitiveSettingPanel(DynamicSettings.PrimitiveSetting<T> setting, JPanel panel, int gridy) {
            super(setting, panel, gridy);
            this.field = new JTextField(this.setting.get().toString());
            this.field.getDocument().addDocumentListener(this);
            GBC.create(panel).grid(1, gridy).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.field);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            this.update(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            this.update(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            this.update(e);
        }

        @SuppressWarnings("unchecked")
        private void update(DocumentEvent e) {
            try {
                switch (this.setting.type.getSimpleName()) {
                    case "int", "Integer" -> this.setting.set((T) Integer.valueOf(e.getDocument().getText(0, e.getDocument().getLength())));
                    case "long", "Long" -> this.setting.set((T) Long.valueOf(e.getDocument().getText(0, e.getDocument().getLength())));
                    case "float", "Float" -> this.setting.set((T) Float.valueOf(e.getDocument().getText(0, e.getDocument().getLength())));
                    case "double", "Double" -> this.setting.set((T) Double.valueOf(e.getDocument().getText(0, e.getDocument().getLength())));
                    case "byte", "Byte" -> this.setting.set((T) Byte.valueOf(e.getDocument().getText(0, e.getDocument().getLength())));
                    case "short", "Short" -> this.setting.set((T) Short.valueOf(e.getDocument().getText(0, e.getDocument().getLength())));
                    default -> {
                        this.field.setForeground(Color.RED);
                        return;
                    }
                }
                this.field.setForeground(Color.BLACK);
            } catch (NumberFormatException | BadLocationException ex) {
                this.field.setForeground(Color.RED);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void save() {
            try {
                switch (setting.type.getSimpleName()) {
                    case "int", "Integer" -> this.setting.set((T) Integer.valueOf(this.field.getText()));
                    case "long", "Long" -> this.setting.set((T) Long.valueOf(this.field.getText()));
                    case "float", "Float" -> this.setting.set((T) Float.valueOf(this.field.getText()));
                    case "double", "Double" -> this.setting.set((T) Double.valueOf(this.field.getText()));
                    case "byte", "Byte" -> this.setting.set((T) Byte.valueOf(this.field.getText()));
                    case "short", "Short" -> this.setting.set((T) Short.valueOf(this.field.getText()));
                    default -> {
                        this.field.setForeground(Color.RED);
                        return;
                    }
                }
                this.field.setForeground(Color.BLACK);
            } catch (NumberFormatException ex) {
                this.field.setForeground(Color.RED);
            }
        }
    }

    public static class BoundedIntSettingPanel extends SettingPanel<DynamicSettings.BoundedIntSetting> {
        private final JSpinner field;

        public BoundedIntSettingPanel(DynamicSettings.BoundedIntSetting setting, JPanel panel, int gridy) {
            super(setting, panel, gridy);
            this.field = new JSpinner(new SpinnerNumberModel((int) this.setting.get(), this.setting.getMin(), this.setting.getMax(), 1));
            GBC.create(panel).grid(1, gridy).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.field);
        }

        @Override
        public void save() {
            this.setting.set((int) this.field.getValue());
        }
    }

    public static class BoundedDoubleSettingPanel extends SettingPanel<DynamicSettings.BoundedDoubleSetting> {
        private final JSpinner field;

        public BoundedDoubleSettingPanel(DynamicSettings.BoundedDoubleSetting setting, JPanel panel, int gridy) {
            super(setting, panel, gridy);
            this.field = new JSpinner(new SpinnerNumberModel((double) this.setting.get(), this.setting.getMin(), this.setting.getMax(), 0.1));
            GBC.create(panel).grid(1, gridy).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.field);
        }

        @Override
        public void save() {
            this.setting.set((double) this.field.getValue());
        }
    }

    public static class StringSettingPanel extends SettingPanel<DynamicSettings.StringSetting> {
        private final JTextField field;

        public StringSettingPanel(DynamicSettings.StringSetting setting, JPanel panel, int gridy) {
            super(setting, panel, gridy);
            this.field = new JTextField(this.setting.get());
            GBC.create(panel).grid(1, gridy).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.field);
        }

        @Override
        public void save() {
            this.setting.set(this.field.getText());
        }
    }

    public static class CharSettingPanel extends SettingPanel<DynamicSettings.CharSetting> implements DocumentListener {
        private final JTextField field;

        public CharSettingPanel(DynamicSettings.CharSetting setting, JPanel panel, int gridy) {
            super(setting, panel, gridy);
            this.field = new JTextField(String.valueOf(this.setting.get()));
            this.field.getDocument().addDocumentListener(this);
            GBC.create(panel).grid(1, gridy).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.field);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            this.update(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            this.update(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            this.update(e);
        }

        private void update(DocumentEvent e) {
            if (e.getDocument().getLength() == 1) {
                this.field.setForeground(Color.RED);
            } else {
                this.field.setForeground(Color.BLACK);
            }
        }

        @Override
        public void save() {
            this.setting.set(this.field.getText().charAt(0));
        }
    }

}

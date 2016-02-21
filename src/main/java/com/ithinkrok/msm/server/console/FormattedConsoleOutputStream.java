package com.ithinkrok.msm.server.console;

import com.google.common.base.Charsets;
import com.ithinkrok.util.StringUtils;
import jline.console.ConsoleReader;
import org.fusesource.jansi.Ansi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by paul on 21/02/16.
 */
public class FormattedConsoleOutputStream extends ByteArrayOutputStream {

    private final ConsoleReader consoleReader;

    private final Map<String, String> replacements = new HashMap<>();

    private final LogThread logThread;

    public FormattedConsoleOutputStream(ConsoleReader consoleReader) {
        this.consoleReader = consoleReader;

        replacements.put("§0", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff().toString());
        replacements.put("§1", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString());
        replacements.put("§2", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff().toString());
        replacements.put("§3", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff().toString());
        replacements.put("§4", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString());
        replacements.put("§5", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString());
        replacements.put("§6", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff().toString());
        replacements.put("§7", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString());

        replacements.put("§8", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString());
        replacements.put("§9", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString());
        replacements.put("§a", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString());
        replacements.put("§b", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString());
        replacements.put("§c", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold().toString());
        replacements.put("§d", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).bold().toString());
        replacements.put("§e", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString());
        replacements.put("§f", Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString());

        replacements.put("§k", Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW).toString());
        replacements.put("§l", Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE).toString());
        replacements.put("§m", Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON).toString());
        replacements.put("§n", Ansi.ansi().a(Ansi.Attribute.UNDERLINE).toString());
        replacements.put("§o", Ansi.ansi().a(Ansi.Attribute.ITALIC).toString());
        replacements.put("§r", Ansi.ansi().a(Ansi.Attribute.RESET).toString());

        logThread = new LogThread();
        logThread.start();
    }

    @Override
    public void flush() throws IOException {
        String contents = toString(Charsets.UTF_8.name());
        super.reset();
        if (!contents.trim().isEmpty() && !contents.equals("\n") && !contents.equals("\r\n")) {
            logThread.queue(contents);
        }
    }

    private void print(String text) {
        text = StringUtils.convertAmpersandToSelectionCharacter(text);

        for(Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
            text = text.replace(entry.getKey().toUpperCase(), entry.getValue());
        }

        try {
            consoleReader.print(ConsoleReader.RESET_LINE + text + Ansi.ansi().reset().toString());
            consoleReader.drawLine();
            consoleReader.flush();
        } catch (IOException ignored) {
        }
    }

    private class LogThread extends Thread {

        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        public LogThread() {
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                String logLine;

                try {
                    logLine = queue.take();
                } catch (InterruptedException ignored) {
                    continue;
                }

                print(logLine);
            }

            for (String logLine : queue) {
                print(logLine);
            }
        }

        public void queue(String logLine) {
            if (!isInterrupted()) {
                queue.add(logLine);
            }
        }
    }
}

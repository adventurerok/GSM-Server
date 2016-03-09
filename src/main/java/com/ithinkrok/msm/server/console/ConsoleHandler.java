package com.ithinkrok.msm.server.console;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.command.CommandHandler;
import com.ithinkrok.msm.server.command.ConsoleCommandSender;
import com.ithinkrok.msm.server.event.command.ConsoleCommandEvent;
import com.ithinkrok.util.command.CustomCommand;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by paul on 18/02/16.
 */
public class ConsoleHandler {

    private final Logger log = LogManager.getLogger(ConsoleHandler.class);

    private final ConsoleReader reader;
    private final ConsoleCommandSender commandSender;

    public static PrintStream out = System.out;

    private boolean stopped = false;

    public ConsoleHandler(Server server, ConsoleReader reader) {
        this.reader = reader;

        this.commandSender = new ConsoleCommandSender(server);

        FormattedConsoleOutputStream consoleWriter = new FormattedConsoleOutputStream(reader);

        System.setOut(out = new PrintStream(consoleWriter, true));
        System.setErr(new PrintStream(consoleWriter, true));
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public void runConsole() {
        String line;
        try {
            while((line = reader.readLine("> ")) != null && !stopped) {
                if(line.trim().isEmpty()) continue;

                CustomCommand command = new CustomCommand(line);
                ConsoleCommandEvent commandEvent = new ConsoleCommandEvent(commandSender, command);

                CommandHandler commandHandler = commandSender.getServer().getCommandHandler();
                if(!commandHandler.executeCommand(commandEvent)) continue;

                if(!commandEvent.isHandled()) {
                    commandSender.sendMessage("This command does not support the console");
                }
            }
        } catch (IOException e) {
            log.error("Error while reading console line. Future console input disabled", e);
        }
    }
}

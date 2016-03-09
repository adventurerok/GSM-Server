package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.server.event.command.MSMCommandEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Created by paul on 09/03/16.
 */
public interface CommandHandler {

    void registerCommand(ServerCommandInfo command);

    ServerCommandInfo getCommand(String name);

    Collection<ServerCommandInfo> getRegisteredCommands();

    boolean executeCommand(MSMCommandEvent commandEvent);

    default void addTabCompletionItems(String listName, String...items) {
        addTabCompletionItems(listName, Arrays.asList(items));
    }

    void addTabCompletionItems(String listName, Collection<String> items);

    default void removeTabCompletionItems(String listName, String...items) {
        removeTabCompletionItems(listName, Arrays.asList(items));
    }

    void removeTabCompletionItems(String listName, Collection<String> items);

    Set<String> getTabCompletionItems(String listName);

    void setTabCompletionItems(String setName, Set<String> items);

    Set<String> getTabCompletionSetNames();
}

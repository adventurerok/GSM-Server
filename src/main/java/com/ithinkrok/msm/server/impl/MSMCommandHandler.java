package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.server.command.CommandHandler;
import com.ithinkrok.msm.server.command.ServerCommandInfo;
import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.msm.server.event.command.TabCompletionSetModifiedEvent;
import com.ithinkrok.util.event.CustomEventExecutor;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;
import org.apache.commons.lang.Validate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by paul on 09/03/16.
 */
public class MSMCommandHandler implements CommandHandler {


    private final Map<String, ServerCommandInfo> commandMap = new ConcurrentHashMap<>();

    private final Map<String, ServerCommandInfo> commandAliasMap = new ConcurrentHashMap<>();

    /**
     * Maps tab-completion list names to the set of items they contain. A copy of this list is maintained on every
     * client for tab-completion.
     */
    private final Map<String, Set<String>> tabCompletionSets = new ConcurrentHashMap<>();

    private final Collection<CustomListener> tabSetModifiedListeners;

    public MSMCommandHandler(Collection<CustomListener> tabSetModifiedListeners) {
        this.tabSetModifiedListeners = tabSetModifiedListeners;
    }

    @Override
    public void registerCommand(ServerCommandInfo command) {
        commandMap.put(command.getName(), command);

        commandAliasMap.put(command.getName(), command);

        for (String alias : command.getAliases()) {
            commandAliasMap.put(alias, command);
        }
    }

    @Override
    public ServerCommandInfo getCommand(String name) {
        if (name == null) return null;

        return commandAliasMap.get(name);
    }

    @Override
    public Collection<ServerCommandInfo> getRegisteredCommands() {
        return commandMap.values();
    }

    @CustomEventHandler
    public boolean executeCommand(MSMCommandEvent event) {
        event.setHandled(false);

        ServerCommandInfo commandInfo = getCommand(event.getCommand().getCommand());
        if (commandInfo == null) {
            event.getCommandSender().sendMessage("Unknown MSM command: " + event.getCommand().getCommand());
            return false;
        }

        CustomEventExecutor.executeEvent(event, commandInfo.getCommandListener());

        if (!event.isValidCommand()) {
            event.getCommandSender().sendMessage("Usage: " + commandInfo.getUsage());
            return false;
        }

        return true;
    }

    @Override
    public void addTabCompletionItems(String setName, Collection<String> items) {
        Validate.notNull(setName, "setName cannot be null");

        if (items.isEmpty()) return;

        Set<String> set = tabCompletionSets.get(setName);

        if (set == null) {
            set = new HashSet<>();

            tabCompletionSets.putIfAbsent(setName, set);
            set = tabCompletionSets.get(setName);
        }

        if(!set.addAll(items)) return;

        TabCompletionSetModifiedEvent event =
                new TabCompletionSetModifiedEvent(setName, TabCompletionSetModifiedEvent.ModifyMode.ADD, items);

        CustomEventExecutor.executeEvent(event, tabSetModifiedListeners);
    }

    @Override
    public void removeTabCompletionItems(String setName, Collection<String> items) {
        Validate.notNull(setName, "setName cannot be null");

        if (items.isEmpty()) return;

        Set<String> set = tabCompletionSets.get(setName);

        if (set == null) return;

        if(!set.removeAll(items)) return;

        if(set.isEmpty()) {
            tabCompletionSets.remove(setName);
        }

        TabCompletionSetModifiedEvent event =
                new TabCompletionSetModifiedEvent(setName, TabCompletionSetModifiedEvent.ModifyMode.REMOVE, items);

        CustomEventExecutor.executeEvent(event, tabSetModifiedListeners);
    }

    @Override
    public Set<String> getTabCompletionItems(String setName) {
        Validate.notNull(setName, "setName cannot be null");

        Set<String> result = tabCompletionSets.get(setName);

        return result != null ? new HashSet<>(result) : Collections.emptySet();
    }

    @Override
    public void setTabCompletionItems(String setName, Set<String> items) {
        Validate.notNull(setName, "setName cannot be null");
        Validate.notNull(items, "items cannot be null");

        Set<String> itemsCopy = new HashSet<>(items);
        tabCompletionSets.put(setName, itemsCopy);

        TabCompletionSetModifiedEvent event =
                new TabCompletionSetModifiedEvent(setName, TabCompletionSetModifiedEvent.ModifyMode.SET, itemsCopy);

        CustomEventExecutor.executeEvent(event, tabSetModifiedListeners);
    }

    @Override
    public Set<String> getTabCompletionSetNames() {
        return tabCompletionSets.keySet();
    }
}

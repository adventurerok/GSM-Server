package com.ithinkrok.msm.server.event.command;

import com.ithinkrok.util.event.CustomEvent;

import java.util.Collection;

/**
 * Created by paul on 09/03/16.
 */
public class TabCompletionSetModifiedEvent implements CustomEvent {

    private final String setName;
    private final Collection<String> modification;
    private final ModifyMode modifyMode;

    public TabCompletionSetModifiedEvent(String setName, ModifyMode modifyMode, Collection<String> modification) {
        this.setName = setName;
        this.modifyMode = modifyMode;
        this.modification = modification;
    }

    public String getSetName() {
        return setName;
    }

    public Collection<String> getModification() {
        return modification;
    }

    public ModifyMode getModifyMode() {
        return modifyMode;
    }

    public enum ModifyMode {
        ADD,
        REMOVE,
        SET
    }
}

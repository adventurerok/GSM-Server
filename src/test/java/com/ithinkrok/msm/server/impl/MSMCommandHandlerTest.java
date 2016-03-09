package com.ithinkrok.msm.server.impl;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by paul on 09/03/16.
 */
@RunWith(DataProviderRunner.class)
public class MSMCommandHandlerTest {

    public MSMCommandHandler sut = new MSMCommandHandler(Collections.emptyList());

    @Test
    @DataProvider({"players,game-modes", "medics,spies"})
    public void addAndRemoveTabCompletionItemsShouldAffectGet(String modifySetName, String leaveSetName) {

        //Ensure the sets are empty at the start
        Set<String> modifySet = sut.getTabCompletionItems(modifySetName);
        assertThat(modifySet).isEmpty();

        Set<String> leaveSet = sut.getTabCompletionItems(leaveSetName);
        assertThat(leaveSet).isEmpty();

        String item1 = "PLAYER_1";
        String item2 = "PLAYER_2";

        //Add the test items, and verify the set contains them
        sut.addTabCompletionItems(modifySetName, item1, item2);

        modifySet = sut.getTabCompletionItems(modifySetName);
        assertThat(modifySet).containsOnly(item1, item2);

        //Ensure the other set isn't modified
        leaveSet = sut.getTabCompletionItems(leaveSetName);
        assertThat(leaveSet).isEmpty();

        //Remove a test item, and verify the set no longer contains it
        sut.removeTabCompletionItems(modifySetName, item2);

        modifySet = sut.getTabCompletionItems(modifySetName);
        assertThat(modifySet).containsOnly(item1);

        //Ensure the other set isn't modified
        leaveSet = sut.getTabCompletionItems(leaveSetName);
        assertThat(leaveSet).isEmpty();
    }

    @Test(expected = IllegalArgumentException.class)
    public void disallowNullSetNames() {
        sut.getTabCompletionItems(null);
    }

}
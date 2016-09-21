package com.ithinkrok.msm.server.external;

import com.ithinkrok.msm.server.Server;

/**
 * Created by paul on 21/09/16.
 */
public interface External {

    /**
     * @return The GSM Server object this external is connected to
     */
    Server getConnectedTo();

}

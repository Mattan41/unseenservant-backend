package org.kruskopf.backend.config;

import org.crac.Core;
import org.springframework.stereotype.Component;
import org.crac.Context;
import org.crac.Resource;

@Component
class CRaCHandler implements Resource {
    public CRaCHandler() {
        Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        System.out.println("CRaC: Preparing for checkpoint");
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        System.out.println("CRaC: Restored from checkpoint");
    }
}
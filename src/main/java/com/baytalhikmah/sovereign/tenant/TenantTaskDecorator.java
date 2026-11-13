package com.baytalhikmah.sovereign.tenant;

import org.springframework.core.task.TaskDecorator;

/** Carries the tenant of the thread that hands over a task to the worker thread that runs it. */
public class TenantTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        String tenant = TenantContext.get();
        return () -> TenantContext.runAs(tenant, task);
    }

}

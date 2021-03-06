/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.workbench.pr.backend.server;

import java.util.function.Function;

import org.jbpm.workbench.pr.model.TimerInstanceSummary;
import org.kie.server.api.model.admin.TimerInstance;

public class TimerInstanceSummaryMapper implements Function<TimerInstance, TimerInstanceSummary> {

    @Override
    public TimerInstanceSummary apply(TimerInstance timerInstance) {
        if (timerInstance == null) {
            return null;
        }

        TimerInstanceSummary summary = new TimerInstanceSummary();

        summary.setId(timerInstance.getId());
        summary.setTimerId(timerInstance.getTimerId());
        summary.setName(timerInstance.getTimerName());
        summary.setProcessInstanceId(timerInstance.getProcessInstanceId());
        summary.setActivationTime(timerInstance.getActivationTime());
        summary.setDelay(timerInstance.getDelay());
        summary.setLastFireTime(timerInstance.getLastFireTime());
        summary.setNextFireTime(timerInstance.getNextFireTime());
        summary.setPeriod(timerInstance.getPeriod());
        summary.setRepeatLimit(timerInstance.getRepeatLimit());

        return summary;
    }
}

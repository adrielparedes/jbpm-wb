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

package org.jbpm.workbench.es.client.editors.requestlist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.dashbuilder.dataset.DataSet;
import org.dashbuilder.dataset.client.DataSetReadyCallback;
import org.dashbuilder.dataset.filter.ColumnFilter;
import org.dashbuilder.dataset.filter.CoreFunctionFilter;
import org.dashbuilder.dataset.filter.CoreFunctionType;
import org.jboss.errai.common.client.api.Caller;
import org.jbpm.workbench.common.client.PerspectiveIds;
import org.jbpm.workbench.common.client.filters.active.ActiveFilterItem;
import org.jbpm.workbench.common.client.list.AbstractMultiGridPresenter;
import org.jbpm.workbench.common.client.list.MultiGridView;
import org.jbpm.workbench.common.client.menu.PrimaryActionMenuBuilder;
import org.jbpm.workbench.common.client.menu.RefreshMenuBuilder;
import org.jbpm.workbench.df.client.filter.FilterSettings;
import org.jbpm.workbench.df.client.list.DataSetQueryHelper;
import org.jbpm.workbench.es.client.editors.events.JobSelectedEvent;
import org.jbpm.workbench.es.client.editors.quicknewjob.NewJobPresenter;
import org.jbpm.workbench.es.client.i18n.Constants;
import org.jbpm.workbench.es.client.util.JobStatusConverter;
import org.jbpm.workbench.es.model.RequestSummary;
import org.jbpm.workbench.es.model.events.RequestChangedEvent;
import org.jbpm.workbench.es.service.ExecutorService;
import org.jbpm.workbench.es.util.RequestStatus;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.mvp.Command;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.Menus;

import static org.dashbuilder.dataset.filter.FilterFactory.equalsTo;
import static org.jbpm.workbench.common.client.util.DataSetUtils.*;
import static org.jbpm.workbench.es.model.RequestDataSetConstants.*;

@Dependent
@WorkbenchScreen(identifier = PerspectiveIds.JOB_LIST_SCREEN)
public class RequestListPresenter extends AbstractMultiGridPresenter<RequestSummary, RequestListPresenter.RequestListView> {

    private final org.jbpm.workbench.common.client.resources.i18n.Constants commonConstants = org.jbpm.workbench.common.client.resources.i18n.Constants.INSTANCE;

    private Constants constants = Constants.INSTANCE;

    private NewJobPresenter newJobPresenter;

    private Command newJobCommand;

    @Inject
    private Caller<ExecutorService> executorServices;

    @Inject
    private Event<RequestChangedEvent> requestChangedEvent;

    @Inject
    private Event<JobSelectedEvent> jobSelectedEvent;

    public RequestListPresenter() {
        super();
    }

    public RequestListPresenter(RequestListViewImpl view,
                                Caller<ExecutorService> executorServices,
                                DataSetQueryHelper dataSetQueryHelper,
                                Event<RequestChangedEvent> requestChangedEvent,
                                Event<JobSelectedEvent> jobSelectedEvent,
                                PlaceManager placeManager) {
        this.view = view;
        this.executorServices = executorServices;
        this.dataSetQueryHelper = dataSetQueryHelper;
        this.requestChangedEvent = requestChangedEvent;
        this.jobSelectedEvent = jobSelectedEvent;
        this.placeManager = placeManager;
    }

    @Inject
    protected void setNewJobPresenter(NewJobPresenter newJobPresenter) {
        this.newJobPresenter = newJobPresenter;
    }

    @Inject
    public void setFilterSettingsManager(final JobListFilterSettingsManager filterSettingsManager) {
        super.setFilterSettingsManager(filterSettingsManager);
    }

    @Override
    public void createListBreadcrumb() {
        setupListBreadcrumb(placeManager,
                            commonConstants.Manage_Jobs());
    }

    public void setupDetailBreadcrumb(String detailLabel) {
        setupDetailBreadcrumb(placeManager,
                              commonConstants.Manage_Jobs(),
                              detailLabel,
                              PerspectiveIds.JOB_DETAILS_SCREEN);
    }

    @Override
    protected DataSetReadyCallback getDataSetReadyCallback(final Integer startRange,
                                                           final FilterSettings tableSettings) {
        return errorHandlerBuilder.get().withUUID(tableSettings.getUUID()).withDataSetCallback(
                dataSet -> {
                    if (dataSet != null && dataSetQueryHelper.getCurrentTableSettings().getKey().equals(tableSettings.getKey())) {
                        List<RequestSummary> myRequestSumaryFromDataSet = new ArrayList<RequestSummary>();

                        for (int i = 0; i < dataSet.getRowCount(); i++) {
                            myRequestSumaryFromDataSet.add(getRequestSummary(dataSet,
                                                                             i));
                        }
                        boolean lastPageExactCount = false;
                        if (dataSet.getRowCount() < view.getListGrid().getPageSize()) {
                            lastPageExactCount = true;
                        }
                        updateDataOnCallback(myRequestSumaryFromDataSet,
                                             startRange,
                                             startRange + myRequestSumaryFromDataSet.size(),
                                             lastPageExactCount);
                    }
                })
                .withEmptyResultsCallback(() -> setEmptyResults());
    }

    protected RequestSummary getRequestSummary(final DataSet dataSet,
                                               final Integer index) {
        return new RequestSummary(
                getColumnLongValue(dataSet,
                                   COLUMN_ID,
                                   index),
                getColumnDateValue(dataSet,
                                   COLUMN_TIMESTAMP,
                                   index),
                RequestStatus.valueOf(getColumnStringValue(dataSet,
                                                           COLUMN_STATUS,
                                                           index)),
                getColumnStringValue(dataSet,
                                     COLUMN_COMMANDNAME,
                                     index),
                getColumnStringValue(dataSet,
                                     COLUMN_MESSAGE,
                                     index),
                getColumnStringValue(dataSet,
                                     COLUMN_BUSINESSKEY,
                                     index),
                getColumnIntValue(dataSet,
                                  COLUMN_RETRIES,
                                  index),
                getColumnIntValue(dataSet,
                                  COLUMN_EXECUTIONS,
                                  index),
                getColumnStringValue(dataSet,
                                     COLUMN_PROCESS_NAME,
                                     index),
                getColumnLongValue(dataSet,
                                   COLUMN_PROCESS_INSTANCE_ID,
                                   index),
                getColumnStringValue(dataSet,
                                     COLUMN_PROCESS_INSTANCE_DESCRIPTION,
                                     index),
                getColumnStringValue(dataSet,
                                     COLUMN_JOB_DEPLOYMENT_ID,
                                     index)
        );
    }

    public void cancelRequest(final String deploymentId,
                              final Long requestId) {
        executorServices.call(
                (Void nothing) -> {
                    view.displayNotification(constants.RequestCanceled(requestId));
                    requestChangedEvent.fire(new RequestChangedEvent(requestId));
                }).cancelRequest(getSelectedServerTemplate(),
                                 deploymentId,
                                 requestId);
    }

    public void requeueRequest(final String deploymentId,
                               final Long requestId) {
        executorServices.call(
                (Void nothing) -> {
                    view.displayNotification(constants.RequestRequeued(requestId));
                    requestChangedEvent.fire(new RequestChangedEvent(requestId));
                }).requeueRequest(getSelectedServerTemplate(),
                                  deploymentId,
                                  requestId);
    }

    protected Command getNewJobCommand() {
        if (newJobCommand == null) {
            newJobCommand = () -> {
                final String selectedServerTemplate = getSelectedServerTemplate();
                if (selectedServerTemplate == null || selectedServerTemplate.trim().isEmpty()) {
                    view.displayNotification(constants.SelectServerTemplate());
                } else {
                    newJobPresenter.openNewJobDialog(selectedServerTemplate);
                }
            };
        }
        return newJobCommand;
    }

    @WorkbenchMenu
    public void getMenus(final Consumer<Menus> menusConsumer) {
        menusConsumer.accept(MenuFactory
                                     .newTopLevelCustomMenu(new RefreshMenuBuilder(this)).endMenu()
                                     .newTopLevelCustomMenu(new PrimaryActionMenuBuilder(constants.New_Job(),
                                                                                         getNewJobCommand())).endMenu()
                                     .build());
    }

    @Override
    public void selectSummaryItem(final RequestSummary job) {
        if (job.getStatus() != null) {
            setupDetailBreadcrumb(constants.JobBreadcrumb(job.getId()));
            placeManager.goTo(PerspectiveIds.JOB_DETAILS_SCREEN);
            jobSelectedEvent.fire(new JobSelectedEvent(getSelectedServerTemplate(),
                                                       job.getDeploymentId(),
                                                       job.getJobId()));
        }
    }

    public void requestCreated(@Observes RequestChangedEvent event) {
        refreshGrid();
    }

    @Override
    public void setupActiveSearchFilters() {
        final Optional<String> processInstIdSearch = getSearchParameter(PerspectiveIds.SEARCH_PARAMETER_PROCESS_INSTANCE_ID);
        if (processInstIdSearch.isPresent()) {
            final String processInstId = processInstIdSearch.get();
            addActiveFilter(
                    equalsTo(COLUMN_PROCESS_INSTANCE_ID,
                             Integer.valueOf(processInstId)),
                    constants.Process_Instance_Id(),
                    processInstId,
                    Integer.valueOf(processInstId),
                    v -> removeActiveFilter(equalsTo(COLUMN_PROCESS_INSTANCE_ID,
                                                     v))
            );
        } else {
            final Optional<String> jobSearch = getSearchParameter(PerspectiveIds.SEARCH_PARAMETER_JOB_ID);
            if (jobSearch.isPresent()) {
                final String jobId = jobSearch.get();
                addActiveFilter(equalsTo(COLUMN_ID,
                                         Integer.valueOf(jobId)),
                                constants.JobId(),
                                jobId,
                                Integer.valueOf(jobId),
                                v -> removeActiveFilter(equalsTo(COLUMN_ID, v))
                );
            }
        }
    }

    @Override
    public boolean existActiveSearchFilters() {
        final Optional<String> processInstIdSearch = getSearchParameter(PerspectiveIds.SEARCH_PARAMETER_PROCESS_INSTANCE_ID);
        if (processInstIdSearch.isPresent()) {
            return true;
        }
        final Optional<String> jobSearch = getSearchParameter(PerspectiveIds.SEARCH_PARAMETER_JOB_ID);
        if (jobSearch.isPresent()) {
            return true;
        }
        return false;
    }

    @Override
    public ActiveFilterItem getActiveFilterFromColumnFilter(ColumnFilter columnFilter) {
        if (columnFilter instanceof CoreFunctionFilter) {
            CoreFunctionFilter coreFunctionFilter = (CoreFunctionFilter) columnFilter;
            if (columnFilter.getColumnId().equals(COLUMN_STATUS) &&
                    (coreFunctionFilter.getType() == CoreFunctionType.IN ||
                            coreFunctionFilter.getType() == CoreFunctionType.EQUALS_TO)) {
                return new ActiveFilterItem<>(constants.Status(),
                                              getStatusColumnFilterDescription(columnFilter),
                                              null,
                                              coreFunctionFilter.getParameters(),
                                              v -> removeActiveFilter(columnFilter));
            }
        }
        return super.getActiveFilterFromColumnFilter(columnFilter);
    }

    public String getStatusColumnFilterDescription(ColumnFilter columnFilter) {
        List<Object> parameters = ((CoreFunctionFilter) columnFilter).getParameters();
        final List<String> labels =
                parameters.stream().map(s -> JobStatusConverter.getStatesStrMapping().get(s.toString())).collect(Collectors.toList());
        return constants.Status() + ": " + String.join(", ", labels);
    }

    public void openProcessInstanceView(final String processInstanceId) {
        navigateToPerspective(PerspectiveIds.PROCESS_INSTANCES,
                              PerspectiveIds.SEARCH_PARAMETER_PROCESS_INSTANCE_ID,
                              processInstanceId);
    }

    public Predicate<RequestSummary> getCancelActionCondition() {
        return getActionConditionFromStatusList(new RequestStatus[]{
                RequestStatus.QUEUED,
                RequestStatus.RETRYING,
                RequestStatus.RUNNING
        });
    }

    public Predicate<RequestSummary> getRequeueActionCondition() {
        return getActionConditionFromStatusList(new RequestStatus[]{
                RequestStatus.ERROR,
                RequestStatus.RUNNING
        });
    }

    public Predicate<RequestSummary> getViewProcessActionCondition() {
        return job -> (job.getProcessInstanceId() != null);
    }

    private Predicate<RequestSummary> getActionConditionFromStatusList(RequestStatus[] statusList) {
        return value -> Arrays.stream(statusList).anyMatch(
                s -> s.equals(value.getStatus()));
    }

    public void bulkCancel(List<RequestSummary> jobsSelected) {
        if (jobsSelected == null || jobsSelected.isEmpty()) {
            return;
        }
        for (RequestSummary selected : jobsSelected) {
            if (!getCancelActionCondition().test(selected)) {
                view.displayNotification(constants.Job_Can_Not_Be_Cancelled(selected.getJobId()));
                continue;
            } else {
                cancelRequest(selected.getDeploymentId(),
                              selected.getJobId());
            }
        }
    }

    public void bulkRequeue(List<RequestSummary> jobsSelected) {
        if (jobsSelected == null || jobsSelected.isEmpty()) {
            return;
        }
        for (RequestSummary selected : jobsSelected) {
            if (!getRequeueActionCondition().test(selected)) {
                view.displayNotification(constants.Job_Can_Not_Be_Requeued(selected.getJobId()));
                continue;
            } else {
                requeueRequest(selected.getDeploymentId(),
                               selected.getJobId());
            }
        }
    }

    public interface RequestListView extends MultiGridView<RequestSummary, RequestListPresenter> {

    }
}
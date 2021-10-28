package com.km.zhc.activiti.demo.util;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BpmnUtil {

    private static Logger logger = Logger.getLogger(BpmnUtil.class);

    /** 根据流程id查询流程节点列表信息
     * 流程id 就是 bpmn 文件中的 process 的 id
     * */
    public static List<UserTask> findBpmnInfo(String flowKeyId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取RepositoryService
        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(flowKeyId);
        List<Process> processes = bpmnModel.getProcesses();
        //通过Process获取UserTask信息
        List<UserTask> userTaskList = processes.get(0).findFlowElementsOfType(UserTask.class);
        return userTaskList;
    }

    public static List<FlowElement> getFlowElements(String flowKeyId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取RepositoryService
        RepositoryService repositoryService = processEngine.getRepositoryService();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(flowKeyId);
        List<Process> processes = bpmnModel.getProcesses();
        Collection<FlowElement> flowElements = processes.get(0).getFlowElements();
        List<FlowElement> flowElementsList = new ArrayList<>();
        flowElementsList.addAll(flowElements);
        return flowElementsList;
    }
}

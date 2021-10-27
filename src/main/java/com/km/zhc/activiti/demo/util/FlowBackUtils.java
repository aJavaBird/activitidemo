package com.km.zhc.activiti.demo.util;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程非正常操作（撤回、驳回等）工具类
 * 本类有参考： https://www.cnblogs.com/haoleiliu/p/8652452.html
 * */
public class FlowBackUtils {

    private static Logger logger = Logger.getLogger(FlowBackUtils.class);

    // 创建ProcessEngine
    private static ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    /**
     * 根据任务id，退回指定的节点
     * 比如：关于回退，我们在进行流程的操作的时候难免会手残，开始了一段让人心惊胆战的错误操作，此时第一时间是想着如何挽回自己得出错误，撤回该流程
     * @params taskId 你想tiao回到那一任务节点Id
     */
    public static void taskRollback(String taskId){
        // 根据要跳转的任务ID获取其任务
        HistoricTaskInstance hisTask = processEngine.getHistoryService()
                .createHistoricTaskInstanceQuery().taskId(taskId)
                .singleResult();
        // 进而获取流程实例
        ProcessInstance instance = processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(hisTask.getProcessInstanceId())
                .singleResult();
        //取得流程定义
        ProcessDefinitionEntity definition = (ProcessDefinitionEntity) processEngine.getRepositoryService().getProcessDefinition(hisTask.getProcessDefinitionId());
        //获取历史任务的Activity
        ActivityImpl hisActivity = definition.findActivity(hisTask.getTaskDefinitionKey());
        //实现跳转
        taskRollbackByProcessInstanceIdAndActivityId(instance.getId(),hisActivity.getId());
    }

    /**
     * 根据流程示例id和节点id，退回指定的节点
     * */
    public static void taskRollbackByProcessInstanceIdAndActivityId(String processInstanceId,String activityId){
        //实现跳转
        processEngine.getManagementService().executeCommand(new JumpCmd(processInstanceId, activityId));
    }

    /**
     * 任务驳回（退回上一个节点）
     * @params taskId 即当前任务id
     * 此方法测试失败，建议使用 taskRollback 取而代之
     */
    public static void rejectFlow(String taskId) {
        Task curTask = processEngine.getTaskService().createTaskQuery()
                .taskId(taskId).singleResult();
        if(curTask==null){
            logger.error("taskId="+taskId+" 对应的task为空，不进行处理！");
            return;
        }
        //获取任务ID 然后查询到当前任务节点
        curTask.getProcessDefinitionId();
        ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) processEngine.getRepositoryService().
                createProcessDefinitionQuery().processDefinitionId(curTask.getProcessDefinitionId()).singleResult();
        // 取得上一步活动的节点流向
        ActivityImpl curActivity = processDefinitionEntity.findActivity(curTask.getTaskDefinitionKey());
        List<PvmTransition> incomingTransitions = curActivity.getIncomingTransitions();

        //清空指定节点所有流向并暂时先将所有流向变量暂时存储在一个新的集合（主要是为后来恢复正常流程走向做准备）
        List<PvmTransition> pvmTransitionList = new ArrayList<>();

        List<PvmTransition> outgoingTransitions = curActivity.getOutgoingTransitions();

        for (PvmTransition pvmTransition: outgoingTransitions) {
            pvmTransitionList.add(pvmTransition);
        }
        outgoingTransitions.clear();

        //创建新的流向并且设置新的流向的目标节点 （将该节点的流程走向都设置为上一节点的流程走向，目的是相当于形成一个回路）
        List<TransitionImpl> newTransitionList = new ArrayList<TransitionImpl>();
        for (PvmTransition pvmTransition : incomingTransitions) {
            PvmActivity source = pvmTransition.getSource();
            ActivityImpl inActivity = processDefinitionEntity.findActivity(source.getId());
            TransitionImpl newOutgoingTransition = curActivity.createOutgoingTransition();
            newOutgoingTransition.setDestination(inActivity);
            newTransitionList.add(newOutgoingTransition);
        }

        //完成任务（流程走向上一节点）
        TaskService taskService = processEngine.getTaskService();
        List<Task> taskList = processEngine.getTaskService().createTaskQuery().processInstanceId(curTask.getProcessInstanceId()).list();
        for (Task task : taskList) {
            taskService.complete(task.getId());
            processEngine.getHistoryService().deleteHistoricTaskInstance(task.getId());
        }

        // 恢复方向（实现驳回功能后恢复原来正常的方向）
        for (TransitionImpl transitionImpl : newTransitionList) {
            curActivity.getOutgoingTransitions().remove(transitionImpl);
        }

        for (PvmTransition pvmTransition : pvmTransitionList) {
            outgoingTransitions.add(pvmTransition);
        }

    }
}

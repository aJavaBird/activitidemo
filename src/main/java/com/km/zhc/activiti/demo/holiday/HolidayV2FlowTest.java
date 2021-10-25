package com.km.zhc.activiti.demo.holiday;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 请假流程测试
 * 请假流程如下：
 * 1、组内审批，小组长和政委同时审批，二者都过了之后，才行下一步；
 * 2、部门经理审批；
 * 3、如果请假天数大于3天，则需要分管领导审批，之后是人事审批；
 * 4、如果请假天数小于等于3天，则直接到人事审批
 * 本例使用了排他网关，完善了 HolidayFlowTest 中的遗留问题（申请天数大于3天<比如4天>时的流程问题）
 * */
public class HolidayV2FlowTest {
    private static Logger logger = Logger.getLogger(HolidayV2FlowTest.class);
    /** 需要先画bpmn流程图，idea需要先安装插件actiBPM，画流程图步骤如下（参考 note -> 如何画流程图）：
     * 1、New -> BPMN File
     * 2、画流程图，点击创建的bpmn文件，简单流程图用到的组件就只有3个：StartEvent、UserTask、EndEvent
     * 3、本例使用的是 src/main/resources/diagram/holidayV2.bpmn，节点仅仅是设置了 Assignee，每个节点都是变量，如${assignee0}，而连线也只设置了condition，如 ${days>3}
     * 4、生成png文件，将文件 holiday.bpmn 复制一封，重命名为 holiday.xml，然后使用BPMN Designer 导出png文件
     * 5、xml文件仅仅只是一个临时文件，程序中真正用的文件是 holiday.bpmn 和 holiday.png
     * */
    public static void main(String[] args) {
        String flowKey = "holidayV2"; // 这个key是 act_re_procdef 表中的key，同时也是 bpmn 文件中的 process 的 id
        // 以下每个方法为独立的一个步骤，理论上说，只能有一个方法运行，其他方法都应该注释了，否则不好看执行效果
        // 1、部署流程，需呀部署流程后，才能进行后续操作
//        doDeployment();
        // 2、zhangsan 申请流程，流程启动
//        startProcess(flowKey,"lisi");
        String approver = "renshi01"; // 审批人
//        // 3、查询待办
        findPersonalTaskList(flowKey,approver);
        // 4、完成待办 （3、4 两步是需要循环操作的）
//        String taskId = "47503"; // 这个任务id是从上一步中查询得到的
//        completTask(flowKey,approver,taskId);
        // 5、查询历史信息
        /**  查询 act_hi_actinst 表，条件：根据 processInstanceId 查询
         * 也可以查询 act_hi_taskinst 表，但是这个表查询的数据不包含网管节点，仅为任务节点 */
        String processInstanceId = "42501"; // 这个其实就是 act_hi_procinst 表 的 ID_
        logger.info("历史信息如下：");
        findHistoryInfo(processInstanceId); // 也可以使用 processDefinitionId 查询，但是需要改一下代码
    }

    /** 手动部署
     * 开发中更推荐自动部署，即使用 SpringProcessEngineConfiguration，配置 deploymentResources
     * */
    public static void doDeployment(){
        /**
         * SELECT * FROM act_re_deployment; -- 流程定义部署表，记录流程部署信息
         * SELECT * FROM act_re_procdef; -- 流程定义表，记录流程定义信息
         * SELECT * FROM act_ge_bytearray; -- 资源表
         * */
        // 1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 2、得到RepositoryService实例
        RepositoryService repositoryService = processEngine.getRepositoryService();
        // 3、使用RepositoryService进行部署
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("diagram/holidayV2.bpmn") // 添加bpmn资源
                .addClasspathResource("diagram/holidayV2.png") // 添加png资源
                .name("请假单v2").deploy();
        // 4、输出部署信息
        logger.info("流程部署id：" + deployment.getId());
        logger.info("流程部署名称：" + deployment.getName());
    }

    /** 启动流程 */
    public static void startProcess(String flowKey,String applyUserName){
        /**
         * select * from act_hi_actinst; -- 流程实例执行历史
         * select * from act_hi_identitylink; -- 流程的参与用户历史信息
         * select * from act_hi_procinst; -- 流程实例历史信息
         * select * from act_hi_taskinst; -- 流程任务历史信息
         * select * from act_ru_execution; -- 流程执行信息
         * select * from act_ru_identitylink; -- 流程的参与用户信息
         * select * from act_ru_task; -- 任务信息
         * select * from act_ru_variable; -- 变量信息
         * */
        // 1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        // 流程定义key
        String key = flowKey;
        // 创建变量集合
        Map<String, Object> map = new HashMap<>();
        // 创建请假pojo对象
        HolidayInfo holidayInfo = new HolidayInfo();
        holidayInfo.setDays(3).setFromDate("2021-10-08").setToDate("2021-10-10").setRemark("zhc请假测试");
        //定义流程变量，把出差pojo对象放入map
        map.put("holidayInfo",holidayInfo);
        map.put("applyMan",applyUserName);
        // 这些变量都是在流程图bpmn图中配置的，这里需要设置实际值
        // 以下几个变量可以是根据 applyUserName 动态算出来的
        map.put("zuzhang","zuzhang01"); // applyUserName 的小组长
        map.put("zhengwei","zhengwei01"); // applyUserName 的政委
        map.put("bumenjingli","bumenjingli01,bumenjingli02"); // applyUserName 的部门经理
        map.put("renshi","renshi01"); // applyUserName 的人事负责人
        map.put("fenguan","fenguan01,fenguan02"); // applyUserName 的分管领导
        // 这里可以不写的，但需要改一下流程图中的condition，改成 ${holidayInfo.days>3}
        map.put("days",holidayInfo.getDays());
        // 启动流程实例，并设置流程变量的值（把map传入）
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key, map);
        // 输出
        logger.info("流程实例名称="+processInstance.getName());
        logger.info("流程定义id=="+processInstance.getProcessDefinitionId());
    }

    /**　完成流程 */
    public static void completTask(String flowKey,String userName,String taskId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取taskService
        TaskService taskService = processEngine.getTaskService();
        // 根据流程key 和 任务的负责人 查询任务
        // 返回一个任务对象
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(flowKey) //流程Key
                .taskCandidateUser(userName) //要查询的候选人
                .taskId(taskId)
                .singleResult();
        if(task==null){
            task = taskService.createTaskQuery()
                    .processDefinitionKey(flowKey) //流程Key
                    .taskAssignee(userName) //要查询的负责人
                    .taskId(taskId)
                    .singleResult();
        }else{
            // 指定的是候选人时，因为是多个，所以需要多一个拾取任务的操作，任务拾取后，才能去完成任务
            taskService.claim(taskId, userName); // 候选人需要拾取任务，这样才能变成自己的个人任务
            logger.info(userName+" 拾取流程“"+flowKey+"”的任务: "+task.getId());
        }
        if(task!=null){
            // 完成任务,参数：任务id
            taskService.complete(task.getId());
            logger.info(userName+" 完成流程“"+flowKey+"”的任务: "+task.getId());
        }else{
            logger.info(userName+" 的流程“"+flowKey+"”的任务为空，未进行任何操作");
        }
    }

    /** 查询待办 */
    public static void findPersonalTaskList(String flowKey,String userName) {
        // 任务负责人
        String assignee = userName;
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 创建TaskService
        TaskService taskService = processEngine.getTaskService();
        // 根据流程key 和 任务负责人 查询任务
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey(flowKey) //流程Key
                .taskAssignee(assignee)//只查询该任务负责人的任务
                .list();
        if(list==null){
            list = new ArrayList<>();
        }
        // 根据流程key 和 候选人 查询任务
        List<Task> list2 = taskService.createTaskQuery()
                .processDefinitionKey(flowKey) // 流程Key
                .taskCandidateUser(userName)// 根据候选人查询
                .list();
        if(list2!=null && !list2.isEmpty()){
            list.addAll(list2);
        }
        if(list==null || list.isEmpty()){
            logger.info("[flowKey="+flowKey+",userName="+userName+"] 查询结果为空");
            return;
        }
        for (Task task : list) {
            logger.info("流程实例id：" + task.getProcessInstanceId()+"; 任务id：" + task.getId()
                    +"; 任务负责人：" + task.getAssignee()+"; 流程示例id：" + task.getProcessInstanceId()
                    +"; 任务名称：" + task.getName());
        }
    }

    /** 查询历史信息 */
    public static void findHistoryInfo(String processInstanceId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取HistoryService
        HistoryService historyService = processEngine.getHistoryService();
        // 获取 actinst表的查询对象
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();
        // 查询 actinst表，条件：根据 InstanceId 查询
        // instanceQuery.processInstanceId("2501");
        // 查询 actinst表，条件：根据 DefinitionId 查询
//        instanceQuery.processDefinitionId(processDefinitionId);
        instanceQuery.processInstanceId(processInstanceId);
        // 增加排序操作,orderByHistoricActivityInstanceStartTime 根据开始时间排序 asc 升序
        instanceQuery.orderByHistoricActivityInstanceStartTime().asc();
        // 查询所有内容
        List<HistoricActivityInstance> activityInstanceList = instanceQuery.list();
        // 输出
        for (HistoricActivityInstance hi : activityInstanceList) {
            logger.info("activityId="+hi.getActivityId()+",activityName="+hi.getActivityName()
                    +",processDefinitionId="+hi.getProcessDefinitionId()
                    +",processInstanceId="+hi.getProcessInstanceId()+",assignee="+hi.getAssignee()
                    +",taskId="+hi.getTaskId()+",durationInMillis="+hi.getDurationInMillis());
        }
    }


}

package com.km.zhc.activiti.demo.holiday;

import com.km.zhc.activiti.demo.util.FlowBackUtils;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.log4j.Logger;

import java.util.*;

/** 请假流程测试（依然使用了holidayV4的请假流程，但是流程图重新画了，不同意的线都去掉了，改成使用程序控制，不同意则退回到申请人重走流程）
 * 请假流程如下：
 * 1、填写申请/调整
 * 2、组长审批
 * 3、项目经理审批
 * 4、如果请假天数不大于3天，则直接人事审批；如果请假天数大于3天，则需要部门领导先审批，然后才是人事审批；
 * 5、财务审批
 * 其中，第2、3、4、5步中，任意一步选择了不同意，都是退回到第1步，申请人需要调整申请，重走流程
 *
 * 本例使用的是 src/main/resources/diagram/holidayV4.bpmn
 * 本例完善了 HolidayV3FlowTest 中的退回上一步漏洞，参考 testRejectFlow 方法
 * 本例的流程图也更简洁了，另外流程图bpmn设置了名称
 *
 * */
public class HolidayV4FlowTest {
    private static Logger logger = Logger.getLogger(HolidayV4FlowTest.class);
    private static final String USER_TASK_TYPE = "userTask";
    /**
     * 本例使用的是 src/main/resources/diagram/holidayV3.bpmn
     * 本例包含2个子例，实现一体化测试，对应3个方法：
     * testDisagree：测试领导意见为不同意（请假5天），不同意则执行退回到申请人操作（即撤回）
     * testRejectFlow：测试驳回（退回上一步）流程（请假2天）
     * */
    public static void main(String[] args) {
        String flowKey = "holidayV4"; // 这个key是 act_re_procdef 表中的key，同时也是 bpmn 文件中的 process 的 id

        // 1、部署流程，需呀部署流程后，才能进行后续操作
//        doDeployment();

        int times = 1;

//        testDisagree(flowKey,times); // 测试领导意见为不同意（请假5天）
//        findHistoryInfo("80001");

        times = 2;
//        testRejectFlow(flowKey,times); // 测试驳回（退回上一步）流程（请假2天）
        findHistoryInfo("85001");

    }


    /** 测试领导意见为不同意 */
    public static void testDisagree(String flowKey,int times){
        // 创建请假pojo对象
        HolidayInfo holidayInfo = new HolidayInfo().setDays(5).setFromDate("2021-10-08").setToDate("2021-10-10").setRemark("zhc请假测试");
        String applyUser = "zhangsan",zuzhang="tongzuzhang", xiangmujingli="zhangjingli"
                , renshi="lirenshi,weirenshi", bumenjingli="jizong", caiwu="zhuocaiwu,jicaiwu";
        Map<String,Object> variableMap = generateVariableMap(flowKey,times,applyUser
                ,zuzhang, xiangmujingli, renshi, bumenjingli, caiwu,holidayInfo);
        String businessKey = variableMap.get("businessKey")+"";
        logger.info("此例的businessKey = " + businessKey);

        // 1.1、zhangsan 申请流程，流程启动
        String processInstanceId = startProcess(flowKey,variableMap);
        logger.info("此例的processInstanceId = " + processInstanceId);
        // 1.2、zhangsan 完成表单申请填写
        completeTaskByBusinessKey(flowKey,businessKey,null);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 组长同意，因为默认就是同意（已去掉排他网关），所以不需要变量来控制，直接完成就行
        // 根据businessKey完成流程 zuzhang
        completeTaskByBusinessKey(flowKey,businessKey,null);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 项目经理同意，因为默认就是同意（已去掉排他网关），所以不需要变量来控制，直接完成就行
        // 根据businessKey完成流程 xiangmujingli
        completeTaskByBusinessKey(flowKey,businessKey,null);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 部门领导不同意，因为默认就是同意（已去掉排他网关），所以不同意需要进行撤回操作
        // 退回到发起人，重走流程
        cancleFlow(processInstanceId);

        logger.info("最后查看一下待办信息：");
        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        logger.info("历史信息如下：");
        // 查询历史信息
        findHistoryInfo(processInstanceId);

        logger.info("注意：本例中的所有操作，建议业务上新建一张业务表来记录审批人、审批意见等内容，并在通过或不通过时记录到业务表中");
    }

    /** 测试发起者驳回（退回上一步）流程（实现直线条退回上一步，平行审批因为每一条线可能会有多个审批，故未做兼容） */
    public static void testRejectFlow(String flowKey,int times){
        // 创建请假pojo对象
        HolidayInfo holidayInfo = new HolidayInfo().setDays(2).setFromDate("2021-10-08").setToDate("2021-10-10").setRemark("zhc请假测试");
        String applyUser = "zhangsan",zuzhang="tongzuzhang", xiangmujingli="zhangjingli"
                , renshi="lirenshi,weirenshi", bumenjingli="jizong", caiwu="zhuocaiwu,jicaiwu";
        Map<String,Object> variableMap = generateVariableMap(flowKey,times,applyUser
                ,zuzhang, xiangmujingli, renshi, bumenjingli, caiwu,holidayInfo);
        String businessKey = variableMap.get("businessKey")+"";
        logger.info("此例的businessKey = " + businessKey);

        // 1.1、zhangsan 申请流程，流程启动
        String processInstanceId = startProcess(flowKey,variableMap);
        logger.info("此例的processInstanceId = " + processInstanceId);
        // 1.2、zhangsan 完成表单申请填写
        completeTaskByBusinessKey(flowKey,businessKey,null);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 组长同意，因为默认就是同意（已去掉排他网关），所以不需要变量来控制，直接完成就行
        // 根据businessKey完成流程 zuzhang
        completeTaskByBusinessKey(flowKey,businessKey,null);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 项目经理同意，因为默认就是同意（已去掉排他网关），所以不需要变量来控制，直接完成就行
        // 根据businessKey完成流程 xiangmujingli
        completeTaskByBusinessKey(flowKey,businessKey,null);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 人事同意，因为默认就是同意（已去掉排他网关），所以不需要变量来控制，直接完成就行
        // 根据businessKey完成流程 renshi
        completeTaskByBusinessKey(flowKey,businessKey,null);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 财务驳回（退回上一步）流程
        rejectFlow(processInstanceId);

        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        // 再次驳回（退回上一步）流程
        rejectFlow(processInstanceId);

        logger.info("最后查看一下待办信息：");
        // 查询待办
        findNeedDealByBusinessKey(flowKey,businessKey);
        logger.info("历史信息如下：");
        // 查询历史信息
        findHistoryInfo(processInstanceId);
    }

    /**
     * 生成 VariableMap，启动流程时使用（仅针 holidayV3 这个流程）
     * @Param flowKey 流程key，bpmn 文件中的 process 的 id
     * @Param times 次数，递增（调用者自己控制），仅仅是为了让businessKey唯一而使用的
     * @Param applyUser 申请人
     * @Param zuzhang 组长
     * @Param xiangmujingli 项目经理
     * @Param renshi 人事（候选人，多个）
     * @Param bumenjingli 部门经理
     * @Param caiwu 财务（候选人，多个）
     * @Param holidayInfo 请假信息
     * */
    public static Map<String,Object> generateVariableMap(String flowKey,int times,String applyUser
            ,String zuzhang,String xiangmujingli,String renshi,String bumenjingli,String caiwu
            ,HolidayInfo holidayInfo){
        // 创建变量集合
        Map<String, Object> map = new HashMap<>();
        //定义流程变量，把出差pojo对象放入map
        map.put("holidayInfo",holidayInfo);
        map.put("applyUser",applyUser);
        // 这些变量都是在流程图bpmn图中配置的，这里需要设置实际值
        // 以下几个变量可以是根据 applyUserName 动态算出来的
        map.put("zuzhang",zuzhang);
        map.put("xiangmujingli",xiangmujingli);
        map.put("renshi",renshi);
        map.put("bumenjingli",bumenjingli);
        map.put("caiwu",caiwu);
        String businessKey = flowKey+":"+applyUser+":"+times;
        map.put("businessKey",businessKey);
        // 这里可以不写的，但需要改一下流程图中的condition，改成 ${holidayInfo.days>3}
        map.put("days",holidayInfo.getDays());
        return map;
    }

    /** 启动流程 */
    public static String startProcess(String flowKey,Map<String,Object> variableMap){
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
         // 设置一个businessKey
        String businessKey = variableMap.get("businessKey")+"";
        // 启动流程实例，并设置流程变量的值（把map传入）
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(flowKey,businessKey, variableMap);
        // 输出
        logger.info("流程实例名称="+processInstance.getName());
        logger.info("流程定义id=="+processInstance.getProcessDefinitionId());
        return processInstance.getId();
    }

    /**　根据businessKey完成流程(newVariables为需要更改或者设置的变量) */
    public static void completeTaskByBusinessKey(String flowKey,String businessKey,Map<String,Object> newVariables){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取taskService
        TaskService taskService = processEngine.getTaskService();
        // 根据流程key 和 任务的负责人 查询任务
        // 返回一个任务对象
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(flowKey) //流程Key
                .processInstanceBusinessKey(businessKey)
                .singleResult();
        if(task!=null){
            // 设置变量（最常见的就是是否同意）
            if(newVariables!=null && !newVariables.isEmpty()){
                for(String varKey:newVariables.keySet()){
                    processEngine.getRuntimeService().setVariable(task.getProcessInstanceId()
                            ,varKey,newVariables.get(varKey));
                }
            }
            String assignee = task.getAssignee();
            if(task.getAssignee()==null || task.getAssignee().length()==0){
                // 候选人列表
                List<IdentityLink> identityLinkList = taskService.getIdentityLinksForTask(task.getId());
                String assignUserId = identityLinkList.get(0).getUserId(); // 默认由第一个候选人完成任务
                taskService.claim(task.getId(), assignUserId); // 候选人需要拾取任务，这样才能变成自己的个人任务
                logger.info(assignUserId+" 拾取流程“"+businessKey+"”的任务: "+task.getId());
                assignee = assignUserId;
            }
            // 完成任务,参数：任务id
            taskService.complete(task.getId());
            logger.info(assignee+" 完成流程“"+businessKey+"”任务: "+task.getId());
        }else{
            logger.info("流程“"+businessKey+"”任务为空，未进行任何操作");
        }
    }

    /**　发起者撤销（取消）流程，撤回后的流程将回到流程的第一步 */
    public static void cancleFlow(String processInstanceId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        HistoricActivityInstance hisInstance = getHistoryFirstTask(processInstanceId);

        if(hisInstance!=null){
            String taskId = hisInstance.getTaskId();
            FlowBackUtils.taskRollback(taskId);
            logger.info("已撤回流程[processInstanceId="+processInstanceId+"]，撤回前任务: "+taskId);
        }else{
            logger.info("流程[processInstanceId="+processInstanceId+"]任务为空，未进行任何操作");
        }
    }

    /**　审批者驳回流程（退回上一步），此方法存在漏洞，多次退回上一步会出现问题 */
    public static void rejectFlow(String processInstanceId){
//        HistoricActivityInstance hisInstance = getHistorySencondToLastTask(processInstanceId);
        HistoricActivityInstance hisInstance = getHistorySencondToLastTaskV2(processInstanceId);
        if(hisInstance!=null){
            String taskId = hisInstance.getTaskId();
            FlowBackUtils.taskRollback(taskId);
            logger.info("已退回上一步流程[processInstanceId="+processInstanceId+"]，退回前任务: "+taskId);
        }else{
            logger.info("流程[processInstanceId="+processInstanceId+"]可退回上一步任务为空，未进行任何操作");
        }
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

    /** 查询待办（通过 businessKey 查询） */
    public static void findNeedDealByBusinessKey(String flowKey,String businessKey) {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 创建TaskService
        TaskService taskService = processEngine.getTaskService();
        // 根据流程key 和 任务负责人 查询任务
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey(flowKey) //流程Key
                .processInstanceBusinessKey(businessKey)
                .list();
        if(list==null || list.isEmpty()){
            list = new ArrayList<>();
            logger.info("[flowKey="+flowKey+",businessKey="+businessKey+"]待办任务为空");
        }else{
            for (Task task : list) {
                if(task.getAssignee()==null || task.getAssignee().length()==0){
                    // 候选人列表
                    List<IdentityLink> identityLinkList = taskService.getIdentityLinksForTask(task.getId());
                    StringBuilder usersSb = new StringBuilder("[");
                    if(identityLinkList==null || identityLinkList.isEmpty()){
                        usersSb.append("]");
                    }else{
                        usersSb.append(identityLinkList.get(0).getUserId());
                        for(int i=1,l=identityLinkList.size();i<l;i++){
                            usersSb.append(",").append(identityLinkList.get(i).getUserId());
                        }
                        usersSb.append("]");
                    }
                    logger.info("流程实例id：" + task.getProcessInstanceId()+"; 任务id：" + task.getId()
                            +"; 候选负责人：" + usersSb+"; 流程示例id：" + task.getProcessInstanceId()
                            +"; 任务名称：" + task.getName());
                }else{
                    logger.info("流程实例id：" + task.getProcessInstanceId()+"; 任务id：" + task.getId()
                            +"; 任务负责人：" + task.getAssignee()+"; 流程示例id：" + task.getProcessInstanceId()
                            +"; 任务名称：" + task.getName());
                }
            }
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
                    +",activityType="+hi.getActivityType()
                    +",processInstanceId="+hi.getProcessInstanceId()+",assignee="+hi.getAssignee()
                    +",taskId="+hi.getTaskId()+",id="+hi.getId()
                    +",durationInMillis="+hi.getDurationInMillis());
            // durationInMillis（耗时） 不为空，则表示任务已完成
        }
    }

    /** 查询历史信息中的第一个用户任务 */
    public static HistoricActivityInstance getHistoryFirstTask(String processInstanceId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取HistoryService
        HistoryService historyService = processEngine.getHistoryService();
        // 获取 actinst表的查询对象
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();
        instanceQuery.processInstanceId(processInstanceId);
        // 增加排序操作,orderByHistoricActivityInstanceStartTime 根据开始时间排序 asc 升序
        instanceQuery.orderByHistoricActivityInstanceStartTime().asc();
        // 查询所有内容
        List<HistoricActivityInstance> activityInstanceList = instanceQuery.list();
        if(activityInstanceList!=null && !activityInstanceList.isEmpty()){
            for (HistoricActivityInstance hi : activityInstanceList) {
                if(USER_TASK_TYPE.equals(hi.getActivityType())){
                    return hi;
                }
            }
        }
        return null;
    }

    /** 查询历史信息中的倒数第2个用户任务 */
    public static HistoricActivityInstance getHistorySencondToLastTask(String processInstanceId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取HistoryService
        HistoryService historyService = processEngine.getHistoryService();
        // 获取 actinst表的查询对象
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();
        instanceQuery.processInstanceId(processInstanceId);
        // 增加排序操作,orderByHistoricActivityInstanceStartTime 根据开始时间排序 desc 降序
        instanceQuery.orderByHistoricActivityInstanceStartTime().desc();
        int index=0;
        // 查询所有内容
        List<HistoricActivityInstance> activityInstanceList = instanceQuery.list();
        if(activityInstanceList!=null && !activityInstanceList.isEmpty()){
            for (HistoricActivityInstance hi : activityInstanceList) {
                if(USER_TASK_TYPE.equals(hi.getActivityType())){
                    index++;
                }
                if(index==2){
                    return hi;
                }
            }
        }
        return null;
    }

    /** 查询历史信息中的倒数第2个用户任务（这里并没有考虑平行审批的情况，因为平行审批会比较复杂，因为平行中的一条线上可以有多级审批） */
    public static HistoricActivityInstance getHistorySencondToLastTaskV2(String processInstanceId){
        // 获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        // 获取HistoryService
        HistoryService historyService = processEngine.getHistoryService();
        // 获取 actinst表的查询对象
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();
        instanceQuery.processInstanceId(processInstanceId);
        // 增加排序操作,orderByHistoricActivityInstanceStartTime 根据开始时间排序 desc 降序
        instanceQuery.orderByHistoricActivityInstanceStartTime().asc();
        // 查询所有内容
        List<HistoricActivityInstance> activityInstanceList = instanceQuery.list();

        if(activityInstanceList!=null && !activityInstanceList.isEmpty()){
            // 第一个节点
            HistoricActivityInstance firstHisIns = null;
            // 最后一个节点
            HistoricActivityInstance lastHisIns = null;
            // 有效的历史
            List<HistoricActivityInstance> activityInstanceEffectiveList = new ArrayList<>();
            Set<String> effctiveActivities = new HashSet<>();
            for (int i=0,l=activityInstanceList.size();i<l;i++) {
                HistoricActivityInstance hi = activityInstanceList.get(i);
                if(USER_TASK_TYPE.equals(hi.getActivityType())){
                    lastHisIns = hi;
                    if(firstHisIns==null){
                        firstHisIns = hi;
                        activityInstanceEffectiveList.add(hi);
                        effctiveActivities.add(hi.getActivityId());
                    }else{
                        if(hi.getActivityId().equals(firstHisIns.getActivityId())){
                            activityInstanceEffectiveList.clear();
                            effctiveActivities.clear();
                        }
                        if(effctiveActivities.contains(hi.getActivityId())){
                            continue;
                        }
                        activityInstanceEffectiveList.add(hi);
                        effctiveActivities.add(hi.getActivityId());
                    }
                }
            }
            if(activityInstanceEffectiveList.size()>2){
                for(int i=0,l=activityInstanceEffectiveList.size();i<l;i++){
                    if(activityInstanceEffectiveList.get(i).getActivityId().equals(lastHisIns.getActivityId()) && i>=1){
                        return activityInstanceEffectiveList.get(i-1);
                    }
                }
            }
        }
        return null;
    }

    public static String getActivityId(String taskId){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
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
        return hisActivity.getId();
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
                .addClasspathResource("diagram/holidayV4.bpmn") // 添加bpmn资源
                .addClasspathResource("diagram/holidayV4.png") // 添加png资源
                .name("请假单v4").deploy();
        // 4、输出部署信息
        logger.info("流程部署id：" + deployment.getId());
        logger.info("流程部署名称：" + deployment.getName());
    }

}

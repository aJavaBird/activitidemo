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
 * 1、填写申请/调整
 * 2、组长审批
 * 3、项目经理审批
 * 4、如果请假天数不大于3天，则直接人事审批；如果请假天数大于3天，则需要部门领导先审批，然后才是人事审批；
 * 5、财务审批
 * 其中，第2、3、4、5步中，任意一步选择了不同意，都是退回到第1步，申请人需要调整申请，重走流程
 *
 * 本例使用的是 src/main/resources/diagram/holidayV3.bpmn
 * 本类中仅处理流程部署，流程运行示例，请参考 HolidayV3FlowTest
 * */
public class HolidayV3FlowDeployTest {
    private static Logger logger = Logger.getLogger(HolidayV3FlowDeployTest.class);

    public static void main(String[] args) {
        String flowKey = "holidayV3"; // 这个key是 act_re_procdef 表中的key，同时也是 bpmn 文件中的 process 的 id
        // 以下每个方法为独立的一个步骤，理论上说，只能有一个方法运行，其他方法都应该注释了，否则不好看执行效果
        // 1、部署流程，需呀部署流程后，才能进行后续操作
        doDeployment();
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
                .addClasspathResource("diagram/holidayV3.bpmn") // 添加bpmn资源
                .addClasspathResource("diagram/holidayV3.png") // 添加png资源
                .name("请假单v3").deploy();
        // 4、输出部署信息
        logger.info("流程部署id：" + deployment.getId());
        logger.info("流程部署名称：" + deployment.getName());
    }

}

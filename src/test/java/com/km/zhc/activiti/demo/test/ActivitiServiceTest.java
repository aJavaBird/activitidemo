package com.km.zhc.activiti.demo.test;

import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.BeansConfigurationHelper;
import org.apache.log4j.Logger;

/** activiti 的 service 测试，获取 service */
public class ActivitiServiceTest {
    private static Logger logger = Logger.getLogger(ActivitiServiceTest.class);
    public static void main(String[] args) {
        //创建ProcessEngineConfiguration对象
        ProcessEngineConfiguration configuration = createProcessEngineConfigurationFromResource("activiti.cfg.xml","processEngineConfiguration");
        //创建ProcessEngine对象
        ProcessEngine processEngine = configuration.buildProcessEngine();

        /*** RepositoryService 是 Activiti的资源管理接口，提供了管理和控制流程发布包和流程定义的操作。
         * 使用工作流建模工具设计的业务流程图需要使用此Service将流程定义文件的内容部署到计算机中。
         * 除了流程部署定义以外还可以做如下的操作：
         * 1、查询引擎中的发布包和流程定义。
         * 2、暂停或激活发布包以及对应全部和特定流程定义。暂停意味着它们不能再在执行任务操作了，激活是对应的反向操作。
         * 3、获取多种资源，像包含在发布包中的文件获引擎自动生成的流程图。
         * 4、获取流程定义的POJO，可以用解析流程，而不必通过XML。 */
        RepositoryService repositoryService = processEngine.getRepositoryService();
        /** RuntimeService是Activiti的流程运行管理接口，可以从这个接口中获取很多关于流程执行相关的信息 */
        RuntimeService runtimeService = processEngine.getRuntimeService();
        /** TaskService是Activiti的任务管理接口，可以从这个接口中获取任务的信息。 */
        TaskService taskService = processEngine.getTaskService();
        /** HistoryService是Activiti的历史管理类，可以查询历史信息，
         * 执行流程时，引擎会包含很多数据（根据配置），比如流程实例启动时间，任务的参与者，完成任务的时间，每个流程实例的执行路径，等等。 */
        HistoryService historyService = processEngine.getHistoryService();
        /** ManagementService是Activiti的引擎管理接口，提供了对Activiti流程引擎的管理和维护功能，
         * 这些功能不在工作流驱动的应用程序中使用，主要用于Activiti系统的日常维护。 */
        ManagementService managementService = processEngine.getManagementService();

        logger.info("repositoryService = " + repositoryService);
        logger.info("runtimeService = " + runtimeService);
        logger.info("taskService = " + taskService);
        logger.info("historyService = " + historyService);
        logger.info("managementService = " + managementService);
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource, String beanName) {
        return BeansConfigurationHelper.parseProcessEngineConfigurationFromResource(resource, beanName);
    }
}

package com.km.zhc.activiti.demo.test;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.BeansConfigurationHelper;
import org.apache.log4j.Logger;

/**
 * activiti 初始化测试
 * 创建ProcessEngineConfiguration，通过ProcessEngineConfiguration创建ProcessEngine，
 * 在创建ProcessEngine的同时会自动创建数据库。
 * */
public class ActivitiInitTest {

    private static Logger logger = Logger.getLogger(ActivitiInitTest.class);

    public static void main(String[] args) {
        //创建ProcessEngineConfiguration对象
        ProcessEngineConfiguration configuration = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("activiti.cfg.xml");
        //创建ProcessEngine对象
        ProcessEngine processEngine = configuration.buildProcessEngine();
        logger.info("processEngine = " + processEngine);
    }

    // 上面的方法createProcessEngineConfigurationFromResource
    // 在执行activiti-cfg.xml中找固定的名称processEngineConfiguration，
    // 也可以使用重载方法调用，这时就可以不用限定processEngineConfiguration名称。
    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource, String beanName) {
        return BeansConfigurationHelper.parseProcessEngineConfigurationFromResource(resource, beanName);
    }
}

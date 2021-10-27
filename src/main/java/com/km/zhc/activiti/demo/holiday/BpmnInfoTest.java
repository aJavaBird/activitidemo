package com.km.zhc.activiti.demo.holiday;

import com.km.zhc.activiti.demo.util.BpmnUtil;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.List;

/** 根据流程id查询流程节点列表信息
 * 流程id 就是 bpmn 文件中的 process 的 id
 * */
public class BpmnInfoTest {
    private static Logger logger = Logger.getLogger(BpmnInfoTest.class);
    public static void main(String[] args) {
        /**
         * 查询 流程定义表，取出 ID_
         * SELECT * FROM act_re_procdef; -- 流程定义表
         * */
        String flowKeyId = "holidayV2:1:25004";

        // 从bpmn文件 查询流程信息
        List<UserTask> userTaskList = BpmnUtil.findBpmnInfo(flowKeyId);
        // 输出
        for (UserTask task : userTaskList) {
            JSONObject jsonObj = new JSONObject(task);
            logger.info("taskInfo: "+jsonObj.toString());
        }
    }
}

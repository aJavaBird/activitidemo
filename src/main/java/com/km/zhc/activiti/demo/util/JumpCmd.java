package com.km.zhc.activiti.demo.util;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;

/**
 *   因为activiti的具体服务执行是运用可命令模式
 *   所以我们在进行动态任务节点跳转的时候要实现该接口
 */
public class JumpCmd implements Command<ExecutionEntity> {

    private String processInstanceId;
    private String activityId;
    public String reasonDelete = "deleted";

    public JumpCmd(String processInstanceId, String activityId) {
        this.processInstanceId = processInstanceId;
        this.activityId = activityId;
    }

    public JumpCmd(String processInstanceId, String activityId,String reasonDelete) {
        this(processInstanceId,activityId);
        this.reasonDelete = reasonDelete;
    }

    @Override
    public ExecutionEntity execute(CommandContext commandContext) {
        ExecutionEntity executionEntity = commandContext.getExecutionEntityManager().findExecutionById(processInstanceId);
        executionEntity.destroyScope(reasonDelete); //删除的原因
        ProcessDefinitionImpl processDefinition = executionEntity.getProcessDefinition();
        ActivityImpl activity = processDefinition.findActivity(activityId);
        executionEntity.executeActivity(activity);
        return executionEntity;
    }

}
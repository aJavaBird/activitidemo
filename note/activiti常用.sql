SELECT * FROM act_re_deployment; -- 流程定义部署表，记录流程部署信息
SELECT * FROM act_re_procdef; -- 流程定义表，记录流程定义信息
SELECT * FROM act_ge_bytearray; -- 资源表

select * from act_hi_actinst order by convert(id_,signed); -- 流程实例执行历史
select * from act_hi_identitylink order by convert(id_,signed); -- 流程的参与用户历史信息
select * from act_hi_procinst order by convert(id_,signed); -- 流程实例历史信息
select * from act_hi_taskinst order by convert(id_,signed); -- 流程任务历史信息
select * from act_ru_execution order by convert(id_,signed); -- 流程执行信息
select * from act_ru_identitylink order by convert(id_,signed); -- 流程的参与用户信息
select * from act_ru_task order by convert(id_,signed); -- 任务信息
SELECT * FROM act_ru_variable order by convert(id_,signed); -- 变量信息

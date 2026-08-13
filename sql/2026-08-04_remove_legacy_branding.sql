START TRANSACTION;

DELETE FROM sys_job
WHERE job_id IN (1, 2, 3)
  AND job_name LIKE '系统默认%';

UPDATE sys_dept
SET leader = '管理员',
    email = 'admin@datacenter.local'
WHERE dept_id = 100;

COMMIT;

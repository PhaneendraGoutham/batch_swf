SELECT *  FROM swf_dept_all sd
                LEFT OUTER JOIN
                (SELECT se.emp_id,
                        se.emp_cname,
                        se.org_id,
                        se.org_dept_id,
                        so.job_level
                 FROM   swf_emps_all se,
                        swf_office_duty_all so
                 WHERE  so.position_code LIKE 'M%'
                 AND    SYSDATE BETWEEN se.take_office_date AND NVL(se.leave_office_date, SYSDATE)
                 AND    se.duty_id = so.duty_id
                 UNION ALL
                 SELECT se.emp_id,
                        se.emp_cname,
                        se.org_id,
                        TO_NUMBER(SUBSTR(TO_CHAR(saa.dept_id), 0, LENGTH(TO_CHAR(saa.dept_id)) - 2)) org_dept_id,
                        so.job_level
                 FROM   swf_auth_all saa,
                        swf_emps_all se,
                        swf_office_duty_all so
                 WHERE  1 = 1
                 AND    se.emp_id = saa.emp_id
                 AND    SYSDATE BETWEEN se.take_office_date AND NVL(se.leave_office_date, SYSDATE)
                 AND    SYSDATE BETWEEN saa.start_time AND saa.end_time
                 AND    se.duty_id = so.duty_id
                 AND    so.position_code LIKE 'M%') sea ON(sd.dept_id = TO_NUMBER(DECODE(
                                                                             sea.org_id,
                                                                             175, sea.org_dept_id || '10',
                                                                             82, sea.org_dept_id || '20',
                                                                             217, sea.org_dept_id || '30',
                                                                             sea.org_dept_id
                                                                          )))
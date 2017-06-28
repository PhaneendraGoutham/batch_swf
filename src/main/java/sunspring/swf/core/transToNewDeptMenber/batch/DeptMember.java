/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sunspring.swf.core.transToNewDeptMenber.batch;


import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;





@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableBatchProcessing
public class DeptMember {

	@Autowired
	private JobBuilderFactory jobs;

	@Autowired
	private StepBuilderFactory steps;

	@Bean
	@ConfigurationProperties(prefix = "spring.ds_target")
	public DataSource dataSource(){		
		return DataSourceBuilder.create().build();
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource ds){		
		return new JdbcTemplate(ds);
	}
	
	@Bean
	public BatchConfigurer configurer(DataSource ds,PlatformTransactionManager txm,JobRepository rep){
		BatchConfigurer conf=new MyBatchConfig(ds,txm,rep);
		return conf;
	}
	
	
	@Bean
    ItemReader<DataObj> JdbcCursorlItemReader(DataSource dataSource) {
        JdbcCursorItemReader<DataObj> databaseReader = new JdbcCursorItemReader<DataObj>();
        databaseReader.setDataSource(dataSource);
        String sql="SELECT sd.DEPT_ID deptId,sea.EMP_ID emplId,sea.member_type memberType,sea.job_level "
        		+" FROM swf.swf_dept_all sd"
                +" INNER JOIN "
               +"(SELECT se.emp_id,"
                      +"se.emp_cname,"
                       +"se.org_id,"
                       +"se.org_dept_id,"
                       +"so.job_level,"
                       +"'S' member_type"
                +" FROM swf.swf_emps_all se,"
                       +"swf.swf_office_duty_all so"
               +" WHERE  so.position_code LIKE 'M%'"
                +" AND SYSDATE BETWEEN se.take_office_date AND NVL(se.leave_office_date, SYSDATE)"
                +" AND se.duty_id = so.duty_id"
                +" UNION ALL "
                +" SELECT se.emp_id,"
                       +"se.emp_cname,"
                       +"se.org_id,"
                       +"TO_NUMBER(SUBSTR(TO_CHAR(saa.dept_id), 0, LENGTH(TO_CHAR(saa.dept_id)) - 2)) org_dept_id,"
                       +"so.job_level,"
                       +"'P' member_type"
                +" FROM   swf.swf_auth_all saa,"
                       +"swf.swf_emps_all se,"
                       +"swf.swf_office_duty_all so"
               +" WHERE  1 = 1 "
                +" AND se.emp_id = saa.emp_id"
                +" AND SYSDATE BETWEEN se.take_office_date AND NVL(se.leave_office_date, SYSDATE)"
                +" AND SYSDATE BETWEEN saa.start_time AND saa.end_time"
                +" AND se.duty_id = so.duty_id"
                +" AND so.position_code LIKE 'M%') sea ON (sd.dept_id = TO_NUMBER(DECODE("
                                                                            +"sea.org_id,"
                                                                            +"175, sea.org_dept_id || '10',"
                                                                            +"82, sea.org_dept_id || '20',"
                                                                            +"217, sea.org_dept_id || '30',"
                                                                            +"sea.org_dept_id"
                                                                         +")))";
        databaseReader.setSql(sql);
        databaseReader.setRowMapper(new BeanPropertyRowMapper<DataObj>(DataObj.class));
        return databaseReader;
    }

	@Bean
	public ItemProcessor<DataObj, DataObj> processor(){
		return new ItemProcessor<DataObj, DataObj>(){
			
			private JobExecution jobExecution;
			private Map<BigDecimal, MemberJobLevel> deptMgrCheck;
			
			
			@BeforeStep
		    public void beforeStep(StepExecution stepExecution) {
		        jobExecution = stepExecution.getJobExecution();
		        ExecutionContext ect =jobExecution.getExecutionContext();
		        if(ect.containsKey("deptMgrCheck")){
		        	deptMgrCheck=(Map<BigDecimal, MemberJobLevel>) ect.get("deptMgrCheck");
		        }else{
		        	deptMgrCheck=new HashMap<BigDecimal,MemberJobLevel>();
		        	ect.put("deptMgrCheck", deptMgrCheck);
		        }
			}
			
			@Override
			public DataObj process(DataObj item) throws Exception {
				if(item.getMemberType().equals("S"))
				if(deptMgrCheck.containsKey(item.getDeptId())){
					MemberJobLevel empl=deptMgrCheck.get(item.getDeptId());
					if(item.getJobLevel().longValue()>empl.getJobLevel().longValue()){
						empl.setEmplId(item.getEmplId());
						empl.setJobLevel(item.getJobLevel());
						deptMgrCheck.put(item.getDeptId(), empl);
					}
				}else{
					MemberJobLevel empl=new MemberJobLevel();
					empl.setEmplId(item.getEmplId());
					empl.setJobLevel(item.getJobLevel());
					deptMgrCheck.put(item.getDeptId(), empl);
				}
				return item;
			}
			
		};
	}
	
		@Bean
		public JdbcBatchItemWriter<DataObj> writer(DataSource ds) {
			JdbcBatchItemWriter<DataObj> writer = new JdbcBatchItemWriter<DataObj>();
			writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<DataObj>());
			writer.setSql("MERGE INTO SWF.SWF_DEPT_MEMBER dm USING (SELECT :deptId a, :emplId b, :memberType c, :jobLevel d FROM DUAL) s "
					+ "ON (dm.DEPT_ID=:deptId AND dm.EMP_ID=:emplId) WHEN MATCHED THEN UPDATE SET dm.MEMBER_TYPE=s.c,dm.JOB_LEVEL=s.d "
					+ "WHEN NOT MATCHED THEN INSERT VALUES (s.a,s.b,s.c,s.d)");
			writer.setDataSource(ds);
			return writer;
		}
	
		
		@Bean
		public Tasklet tasklet(JdbcTemplate jt) {
			final JdbcTemplate jdbcTemplate =jt;
			return new Tasklet() {
				private JdbcTemplate template=jdbcTemplate;
				private Map<BigDecimal, MemberJobLevel> deptMgrCheck;
			
				
				@Override
				public RepeatStatus execute(StepContribution contribution,
						ChunkContext context) {					
					Map<String,Object> ect=context.getStepContext().getJobExecutionContext();
					if(ect.containsKey("deptMgrCheck")){
			        	deptMgrCheck=(Map<BigDecimal, MemberJobLevel>) ect.get("deptMgrCheck");
			        }
										
					String sql="SELECT dm.DEPT_ID,dm.EMP_ID FROM SWF.SWF_DEPT_MEMBER dm LEFT OUTER JOIN ("
							+"SELECT sd.DEPT_ID,sea.emp_id,sea.member_type "
+" FROM swf.swf_dept_all sd"
                +" INNER JOIN "
                +"(SELECT se.emp_id,"
                        +"se.emp_cname,"
                        +"se.org_id,"
                        +"se.org_dept_id,"
                        +"so.job_level,"
                        +"'M' member_type"
                 +" FROM swf.swf_emps_all se,"
                        +"swf.swf_office_duty_all so"
                 +" WHERE  so.position_code LIKE 'M%'"
                 +" AND SYSDATE BETWEEN se.take_office_date AND NVL(se.leave_office_date, SYSDATE)"
                 +" AND se.duty_id = so.duty_id"
                 +" UNION ALL "
                 +" SELECT se.emp_id,"
                        +"se.emp_cname,"
                        +"se.org_id,"
                        +"TO_NUMBER(SUBSTR(TO_CHAR(saa.dept_id), 0, LENGTH(TO_CHAR(saa.dept_id)) - 2)) org_dept_id,"
                        +"so.job_level,"
                        +"'P' member_type"
                 +" FROM swf.swf_auth_all saa,"
                        +"swf.swf_emps_all se,"
                        +"swf.swf_office_duty_all so"
                 +" WHERE  1 = 1 "
                 +" AND se.emp_id = saa.emp_id"
                 +" AND SYSDATE BETWEEN se.take_office_date AND NVL(se.leave_office_date, SYSDATE)"
                 +" AND SYSDATE BETWEEN saa.start_time AND saa.end_time"
                 +" AND se.duty_id = so.duty_id"
                 +" AND so.position_code LIKE 'M%') sea ON(sd.dept_id = TO_NUMBER(DECODE("
                                                                             +"sea.org_id,"
                                                                             +"175, sea.org_dept_id || '10',"
                                                                             +"82, sea.org_dept_id || '20',"
                                                                             +"217, sea.org_dept_id || '30',"
                                                                             +"sea.org_dept_id"
                                                                         +" )))"
                 +") sub ON dm.DEPT_ID=sub.DEPT_ID AND dm.EMP_ID=sub.EMP_ID WHERE sub.DEPT_ID is NULL";
					List<DataObj> list=template.query(sql,new RowMapper<DataObj>(){
						@Override
						public DataObj mapRow(ResultSet rs, int rowNum) throws SQLException {
							DataObj obj=new DataObj();
							obj.setDeptId(rs.getBigDecimal("DEPT_ID"));
							obj.setEmplId(rs.getBigDecimal("EMP_ID"));
							return obj ;
						}												
					});
					for(DataObj o:list){
						template.update("DELETE FROM SWF.SWF_DEPT_MEMBER dm"
								+ " WHERE dm.DEPT_ID=? AND dm.EMP_ID=?",o.getDeptId(),o.getEmplId());
					}		
					if(deptMgrCheck!=null){
						for(BigDecimal k:deptMgrCheck.keySet()){
							MemberJobLevel empl=deptMgrCheck.get(k);
							template.update("UPDATE SWF.SWF_DEPT_MEMBER dm SET dm.MEMBER_TYPE='M'"
								+ " WHERE dm.DEPT_ID=? AND dm.EMP_ID=? AND dm.MEMBER_TYPE='S'",k,empl.getEmplId());
						}
					}
					
					return RepeatStatus.FINISHED;
				}
			};
		}	
		
	@Bean(name="jobRepository")
	public JobRepository getJobRepository(DataSource ds,PlatformTransactionManager txm) throws Exception {
		JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
		factoryBean.setDataSource(ds);
		factoryBean.setDatabaseType(DatabaseType.ORACLE.name());
		factoryBean.setTransactionManager(txm);
		factoryBean.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
		factoryBean.setTablePrefix("SADM.BATCH_");
		try {
			factoryBean.afterPropertiesSet();
			return factoryBean.getObject();
		} catch (Exception e) {

			throw new BatchConfigurationException(e);
		}
	}
	
	@Bean(name="transactionManager")
	public PlatformTransactionManager transactionManager(DataSource ds){
		DataSourceTransactionManager txa=new DataSourceTransactionManager(ds);
		return txa;
	}
	
	@Bean
	public Job job(@Qualifier("dept_member_step") Step step1,
			@Qualifier("dept_member_step2") Step step2) throws Exception {
		return this.jobs.get("dept_member_job").start(step1).next(step2).build();
	}

	@Bean(name="dept_member_step")
	protected Step step1(ItemReader<DataObj> reader,ItemProcessor<DataObj, DataObj> processor,ItemWriter<DataObj> writer) throws Exception {
		return this.steps.get("dept_member_step")
				.<DataObj,DataObj>chunk(50)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.build();
	}
	@Bean(name="dept_member_step2")
	protected Step step2(Tasklet task) throws Exception{
		return this.steps.get("dept_member_step2")
				.tasklet(task)
				.build();
	}
	
	public static void main(String[] args) throws Exception {
		System.exit(SpringApplication.exit(SpringApplication.run(
				DeptMember.class, args)));
	}
}

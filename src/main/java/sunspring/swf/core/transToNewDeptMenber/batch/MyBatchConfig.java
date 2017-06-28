package sunspring.swf.core.transToNewDeptMenber.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

public class MyBatchConfig implements BatchConfigurer {
	
	private JobRepository rep;
	private PlatformTransactionManager txm;
	private DataSource ds;

	public MyBatchConfig(DataSource ds,PlatformTransactionManager txm,JobRepository rep){
		this.ds=ds;
		this.txm=txm;
		this.rep=rep;
	}
	
	@Override
	public JobRepository getJobRepository() throws Exception {
		
		return rep;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() throws Exception {
		return txm;
	}

	@Override
	public JobLauncher getJobLauncher() throws Exception {
		return createJobLauncher();
	}

	@Override
	public JobExplorer getJobExplorer() throws Exception {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(ds);
		jobExplorerFactoryBean.afterPropertiesSet();
		return  jobExplorerFactoryBean.getObject();
	}
	
	protected JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(rep);
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}
}

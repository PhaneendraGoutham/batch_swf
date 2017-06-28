package sunspring.swf.core.transToNewDeptMenber.batch;

import java.io.Serializable;
import java.math.BigDecimal;

public class MemberJobLevel implements Serializable {
	
	private static final long serialVersionUID = -1156452486437476866L;
	
	private BigDecimal emplId;
	private BigDecimal jobLevel;
	
	public BigDecimal getEmplId() {
		return emplId;
	}
	public void setEmplId(BigDecimal emplId) {
		this.emplId = emplId;
	}
	public BigDecimal getJobLevel() {
		return jobLevel;
	}
	public void setJobLevel(BigDecimal jobLevel) {
		this.jobLevel = jobLevel;
	}
}

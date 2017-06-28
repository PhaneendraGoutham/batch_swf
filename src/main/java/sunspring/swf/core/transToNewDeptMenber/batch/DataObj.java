package sunspring.swf.core.transToNewDeptMenber.batch;

import java.math.BigDecimal;

public class DataObj {
	
	private BigDecimal emplId;
	private BigDecimal deptId;
	private String memberType;
	private BigDecimal jobLevel;
	

	public BigDecimal getEmplId() {
		return emplId;
	}
	public void setEmplId(BigDecimal emplId) {
		this.emplId = emplId;
	}
	public BigDecimal getDeptId() {
		return deptId;
	}
	public void setDeptId(BigDecimal deptId) {
		this.deptId = deptId;
	}
	public String getMemberType() {
		return memberType;
	}
	public void setMemberType(String memberType) {
		this.memberType = memberType;
	}
	public BigDecimal getJobLevel() {
		return jobLevel;
	}
	public void setJobLevel(BigDecimal jobLevel) {
		this.jobLevel = jobLevel;
	}
}

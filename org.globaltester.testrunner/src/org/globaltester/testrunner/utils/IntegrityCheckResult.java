package org.globaltester.testrunner.utils;

/**
 * Result of a single integrity check of a single IContainer
 * 
 * @author amay
 *
 */
public class IntegrityCheckResult {

	public enum IntegrityCheckStatus {
		VALID, INVALID, UNCHECKED;
	}

	IntegrityCheckStatus status = IntegrityCheckStatus.UNCHECKED;

	long expectedChecksum, calculatedChecksum;
	
	public IntegrityCheckResult(IntegrityCheckStatus status, long expectedChecksum, long calculatedChecksum) {
		super();
		this.status = status;
		this.expectedChecksum = expectedChecksum;
		this.calculatedChecksum = calculatedChecksum;
	}

	public IntegrityCheckResult(IntegrityCheckStatus status) {
		this(status, -1, -1);
	}

	public long getExpectedChecksum() {
		return expectedChecksum;
	}

		public long getCalculatedChecksum() {
		return calculatedChecksum;
	}

		public IntegrityCheckStatus getStatus() {
			return status;
		}

}
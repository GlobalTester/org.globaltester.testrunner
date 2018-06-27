package org.globaltester.testrunner.utils;

import java.util.Collection;

/**
 * Result of a single integrity check of a single IContainer
 * 
 * @author amay
 *
 */
public class IntegrityCheckResult {

	public enum IntegrityCheckStatus {
		VALID, INVALID, UNCHECKED;
		
		@Override
		public String toString() {
			switch (this) {
			case VALID:
				return "consistent";
			case INVALID:
				return "inconsistent";
			case UNCHECKED:
				return "(partially) unchecked";
			default:
				return "unknown";
			}
		}
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

		public static IntegrityCheckResult combineCheckStatus(Collection<IntegrityCheckResult> collection) {
			IntegrityCheckStatus combinedStatus = IntegrityCheckStatus.VALID;
			for (IntegrityCheckResult curResult : collection) {
				switch (curResult.getStatus()) {
				case VALID:
					break;
				case INVALID:
					return new IntegrityCheckResult(IntegrityCheckStatus.INVALID);
				case UNCHECKED:
					combinedStatus = IntegrityCheckStatus.UNCHECKED;
					break;

				}
			}
			return new IntegrityCheckResult(combinedStatus);
		}

}
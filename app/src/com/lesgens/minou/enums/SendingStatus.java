package com.lesgens.minou.enums;

public enum SendingStatus {
	PENDING(0),
	SENT(1),
	FAILED(2),
	READ(3),
	RECEIVED(4);

	private final int intValue;

	SendingStatus(int intValue) {
		this.intValue = intValue;
	}

	public int getIntValue(){
		return intValue;
	}

	public static SendingStatus fromInt(int status) {
		if (status == 0) return PENDING;
		else if (status == 1) return SENT;
		else if (status == 2) return FAILED;
		else if (status == 3) return READ;
		else if (status == 4) return RECEIVED;
		return SENT;
	}
}

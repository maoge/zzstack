package com.zzstack.paas.underlying.utils;

import java.util.UUID;

public class UUIDUtils {
	
	public static String genUUID() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
	
}

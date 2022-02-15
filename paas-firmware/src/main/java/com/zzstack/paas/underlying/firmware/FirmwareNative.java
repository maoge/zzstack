package com.zzstack.paas.underlying.firmware;

import java.util.concurrent.atomic.AtomicBoolean;

import com.zzstack.paas.underlying.firmware.utils.JniTools;


public class FirmwareNative {
	
	// generate jni c header files
	// $HOME/work/tools/java/jdk1.8.0_202/bin/javah -classpath /home/ultravirs/work/wks/java/firmware/bin -jni com.smsbase.firmware.FirmwareNative
    
	public static AtomicBoolean LOAD_FALG = new AtomicBoolean(false);

	public native String cpuid();
	
	public native String encrypt(String text);
	
	public native void checkFirmware(byte[] lisence);

	public FirmwareNative() {
		super();
		
		JniTools.loadFirmwareJniFile(this.getClass());
		
		if (!LOAD_FALG.get()) {
			String currPath = System.getProperty("user.dir");
			String fileName = JniTools.getFirmwareJniFileName();
			String jniFile = String.format("%s/%s", currPath, fileName);
			System.load(jniFile);
			LOAD_FALG.set(true);
		}
	}

}

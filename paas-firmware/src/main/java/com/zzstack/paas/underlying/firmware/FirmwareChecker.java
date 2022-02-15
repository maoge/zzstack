package com.zzstack.paas.underlying.firmware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirmwareChecker {
	
	private ScheduledExecutorService exec;
    private Runnable runner;
    
    private static final long CHECK_INTERVAL = 60;
    private static final String LISENCE = "system.license";
    
    private static volatile FirmwareChecker instance;
    
	private FirmwareChecker() throws IOException {
		byte[] lisence = getLisence();
		FirmwareNative firmware = new FirmwareNative();
		
		runner = new CheckRunner(firmware, lisence);
		
		exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(runner, 10, CHECK_INTERVAL, TimeUnit.SECONDS);
	}
	
	private byte[] getLisence() throws IOException {
		String file = String.format("conf/%s", LISENCE);
		return Files.readAllBytes(Paths.get(file));
	}
	
	public static FirmwareChecker get() {
		if (instance != null)
			return instance;
		
		synchronized (FirmwareNative.class) {
			if (instance == null) {
				try {
					instance = new FirmwareChecker();
				} catch (IOException e) {
					System.exit(-1);
				}
			}
		}
		
		return instance;
	}
	
	private static class CheckRunner implements Runnable {
	    private static Logger logger = LoggerFactory.getLogger(CheckRunner.class);
	    
		private FirmwareNative firmware;
		private byte[] encrypt;
		
		public CheckRunner(FirmwareNative firmware, byte[] encrypt) {
			this.firmware = firmware;
			this.encrypt = encrypt;
		}

		@Override
		public void run() {
			firmware.checkFirmware(encrypt);
			logger.info("checkFirmware ok!");
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
//		FirmwareNative firmware = new FirmwareNative();
//		
//		System.out.println(firmware.cpuid());  // 0x49656e696c65746e
//		
//		JSONObject json = new JSONObject();
//		json.put("cpu_id", "0x49656e696c65746e");
//		json.put("valid_date", "2021-07-30");
//		String s = json.toJSONString();
//		String decrypt = firmware.encrypt(s);
//		System.out.println(decrypt);
		
		FirmwareChecker.get();
	}

}

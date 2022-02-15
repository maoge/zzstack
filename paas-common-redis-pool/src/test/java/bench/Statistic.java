package bench;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Statistic {
	private AtomicLong maxTPS;
	private AtomicLong[] normalCntVec;
	private ScheduledExecutorService statRunnerExec;
	private Runnable statRunner;
	private ScheduledExecutorService statPrintExec;
	private Runnable statPrinter;
	private long lastTotalCnt;
	private long begTS;
	private long lastTS;
	private long lastTPS;
	private long avgTPS;
	
	private static final long STAT_INTERVAL = 1000L;
	private static final long PRINT_INTERVAL = 5000L;

	public Statistic(AtomicLong maxTPS, AtomicLong[] normalCntVec) {
		this.maxTPS = maxTPS;
		this.normalCntVec = normalCntVec;
		this.lastTotalCnt = 0L;

		this.begTS = System.currentTimeMillis();
		this.lastTS = this.begTS;

		this.statRunner = new StatRunner(this);
		this.statRunnerExec = Executors.newSingleThreadScheduledExecutor();
		this.statRunnerExec.scheduleAtFixedRate(this.statRunner, STAT_INTERVAL, STAT_INTERVAL,
				TimeUnit.MILLISECONDS);

		this.statPrinter = new StatPrinter(this);
		this.statPrintExec = Executors.newSingleThreadScheduledExecutor();
		this.statPrintExec.scheduleAtFixedRate(this.statPrinter, PRINT_INTERVAL, PRINT_INTERVAL,
				TimeUnit.MILLISECONDS);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Statistic.this.statRunnerExec.shutdown();
				Statistic.this.statPrintExec.shutdown();
			}
		});
	}

	public void StopRunning() {
		if (!this.statRunnerExec.isShutdown()) {
			this.statRunnerExec.shutdown();
		}

		if (!this.statPrintExec.isShutdown()) {
			this.statPrintExec.shutdown();
		}

		computeStatInfo();
		printStatInfo();
	}

	private long getCurrTotal() {
		long currTotalCnt = 0L;

		for (AtomicLong ai : this.normalCntVec) {
			currTotalCnt += ai.get();
		}

		return currTotalCnt;
	}

	public void computeStatInfo() {
		long currTotalCnt = getCurrTotal();
		long currTS = System.currentTimeMillis();

		long diff = currTotalCnt - this.lastTotalCnt;

		if (currTS > this.lastTS) {
			this.lastTPS = diff * 1000L / (currTS - this.lastTS);
			this.avgTPS = currTotalCnt * 1000L / (currTS - this.begTS);

			if (this.lastTPS > this.maxTPS.get()) {
				this.maxTPS.set(this.lastTPS);
			}

			this.lastTS = currTS;
			this.lastTotalCnt = currTotalCnt;
		}
	}

	public void printStatInfo() {
		String print = String
				.format("Statistic runs for:%d seconds, total processed:%d, Last TPS:%d, Avg TPS:%d, Max TPS:%d",
						new Object[] {
								Long.valueOf((this.lastTS - this.begTS) / 1000L),
								Long.valueOf(this.lastTotalCnt),
								Long.valueOf(this.lastTPS),
								Long.valueOf(this.avgTPS),
								Long.valueOf(this.maxTPS.get()) });
		System.out.println("--------------------------------------------------------------------------------------------------");
		System.out.println(print);
		System.out.println("--------------------------------------------------------------------------------------------------");
	}

	private static class StatRunner implements Runnable {
		private Statistic statistic;

		public StatRunner(Statistic statistic) {
			this.statistic = statistic;
		}

		public void run() {
			this.statistic.computeStatInfo();
		}
	}

	private static class StatPrinter implements Runnable {
		private Statistic statistic;

		public StatPrinter(Statistic statistic) {
			this.statistic = statistic;
		}

		public void run() {
			this.statistic.printStatInfo();
		}
	}
}

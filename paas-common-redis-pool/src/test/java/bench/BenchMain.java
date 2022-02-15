package bench;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.redis.MultiRedissonClient;
import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;
import com.zzstack.paas.underlying.redis.node.RedissonClientHolder;
import com.zzstack.paas.underlying.utils.PropertiesUtils;

public class BenchMain {
    
    private static Logger logger = LoggerFactory.getLogger(BenchMain.class);

	private static AtomicLong[] normalCntVec;
	private static AtomicLong[] errorCntVec;
	private static AtomicLong maxTPS;

	private static final String QUEUE_FMT = "Q_%02d";
	
	public static int  THREAD_CNT;
	public static long TOTAL_TIME;
	public static int  TYPE;
	public static final int TYPE_PUSH = 1;
	public static final int TYPE_POP = 2;
	

	public static void main(String[] args) {
		String confName = "test";
		PropertiesUtils props = PropertiesUtils.getInstance(confName);
		
		THREAD_CNT = props.getInt("threadCnt");
		TOTAL_TIME = props.getInt("totalTime");
		TYPE       = props.getInt("type");

		bench();
	}

	private static void bench() {
		normalCntVec = new AtomicLong[THREAD_CNT];
		errorCntVec = new AtomicLong[THREAD_CNT];
		for (int i = 0; i < THREAD_CNT; i++) {
			normalCntVec[i] = new AtomicLong(0L);
			errorCntVec[i] = new AtomicLong(0L);
		}
		maxTPS = new AtomicLong(0L);

		Statistic stat = new Statistic(maxTPS, normalCntVec);

		Vector<Bencher> theadVec = new Vector<Bencher>(THREAD_CNT);
		int idx = 0;

		long start = System.currentTimeMillis();
		long totalDiff = 0L;

		for (; idx < THREAD_CNT; idx++) {
			String threadName = String.format("%s.%02d", new Object[] { "bencher", Integer.valueOf(idx) });
			Bencher bencher = new Bencher(threadName, normalCntVec[idx], errorCntVec[idx]);
			Thread thread = new Thread(bencher);
			thread.start();

			theadVec.add(bencher);
		}

		while (totalDiff < TOTAL_TIME) {
			long curr = System.currentTimeMillis();
			totalDiff = (curr - start) / 1000L;

			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
			    logger.error(e.getMessage(), e);
			}
		}

		for (Bencher bencher : theadVec) {
			bencher.StopRunning();
		}

		try {
			Thread.sleep(3000L);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}

		stat.StopRunning();
	}

	private static class Bencher implements Runnable {
		private AtomicLong normalCnt;
		private AtomicLong errorCnt;
		private boolean bRunning;

		public Bencher(String threadName, AtomicLong normalCnt, AtomicLong errorCnt) {
			this.normalCnt = null;
			this.errorCnt = null;

			this.bRunning = true;

			this.normalCnt = normalCnt;
			this.errorCnt = errorCnt;
		}
		
		public void run() {
		    WeightedRRLoadBalancer balancer = MultiRedissonClient.get("redis-cluster-queue");
		    
		    byte[] msgBytes = "aaaaaaaaabbbbbbbbbcccccccc".getBytes();
		    
			try {
			    int cnt = 0;
				while (this.bRunning) {
					try {
					    RedissonClientHolder redissonClientHolder = (RedissonClientHolder) balancer.select();
					    // System.out.println(redissonClientHolder.id());
					    RedissonClient redissonClient = redissonClientHolder.getRedissonClient();
						
					    String identifier = String.format(QUEUE_FMT, cnt++ % 100);
					    RDeque<byte[]> deque = redissonClient.getDeque(identifier);
					    
					    if (TYPE == TYPE_PUSH) {
					        deque.addFirst(msgBytes);
					    } else {
					        // byte[] bytes = deque.pollLast();
					        deque.pollLast();
					    }
					    
					    normalCnt.incrementAndGet();
					    
					} catch (Exception e) {
						errorCnt.incrementAndGet();
						logger.error(e.getMessage(), e);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {

			}
		}
		
		public void StopRunning() {
			this.bRunning = false;
		}
	}

}

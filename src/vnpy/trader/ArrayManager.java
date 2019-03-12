package vnpy.trader;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import javax.security.auth.x500.X500Principal;


public class ArrayManager {
//    K线序列管理工具，负责：
//    1. K线时间序列的维护
//    2. 常用技术指标的计算
	private int count; // 缓存计数
	private int size; // 缓存大小
	private boolean inited; // True if count>=size

	private Deque<Double> openArray; // OHLC
	private Deque<Double> highArray;
	private Deque<Double> lowArray;
	private Deque<Double> closeArray;
	private Deque<Integer> volumeArray;
	
	public ArrayManager() {
		this(100);
	}
	
	public ArrayManager(int size) {
		this.count = 0;                      // 缓存计数
		this.size = size;                    // 缓存大小
		this.inited = false;                 // True if count>=size
		        
		this.openArray = new ArrayDeque<Double>(size);     // OHLC
		this.highArray = new ArrayDeque<Double>(size);
		this.lowArray = new ArrayDeque<Double>(size);
		this.closeArray = new ArrayDeque<Double>(size);
		this.volumeArray = new ArrayDeque<Integer>(size);
		for (int i = 0; i < size; i++) {
			this.openArray.addLast(0.);
			this.highArray.addLast(0.);
			this.lowArray.addLast(0.);
			this.closeArray.addLast(0.);
			this.volumeArray.addLast(0);
		}
	} 
	
	public void updateBar(VtBarData bar) {
        // 更新K线
        this.count += 1;
        if (!this.inited && this.count >= this.size) {
        	this.inited = true;
        }
        
        openArray.addLast(bar.getOpen());
        openArray.pollFirst();
        highArray.addLast(bar.getHigh());
        highArray.pollFirst();
        lowArray.addLast(bar.getLow());
        lowArray.pollFirst();
        closeArray.addLast(bar.getClose());
        closeArray.pollFirst();
        volumeArray.addLast(bar.getVolume());
        volumeArray.pollFirst();
	}

	// 肯特纳通道
    public double[] keltner(int n, double dev) {
        double mid = this.sma(n);
        double atr = this.atr(n);
        
        double up = mid + atr * dev;
        double down = mid - atr * dev;
        //System.out.println("up="+up+" down="+down);
        return new double[] {up, down};
    }

    // 简单均线
    public double sma(int n) {
    	double total = this.closeArray.stream().skip(this.size-n).reduce(0., (sum, i)->{sum+=i; return sum;});
    	//System.out.println("sma="+total/n);
    	return total/n;
    }
    
    // ATR指标
    // TR : MAX(MAX((HIGH-LOW),ABS(REF(CLOSE,1)-HIGH)),ABS(REF(CLOSE,1)-LOW));    
    // ATR : MA(TR,N)
    public double atr(int n) {
    	Double[] high = this.highArray.toArray(new Double[this.size]);
    	Double[] low = this.lowArray.toArray(new Double[this.size]);
    	Double[] close = this.closeArray.toArray(new Double[this.size]);
    	Double[] tr = new Double[this.size];
    	tr[0] = high[0]-low[0];
    	for (int i = 1; i < this.size; i++) {
			tr[i] = Math.max(Math.max((high[i]-low[i]), Math.abs(close[i-1]-high[i])), Math.abs(close[i-1]-low[i]));
		}
    	
    	//double alpha = 1 - Math.exp(Math.log(0.5) / n);
    	
    	//double atr = Arrays.asList(tr).stream().reduce(0., (acc, i)->{return ewma(acc, i, alpha);});
    	
    	double total = Arrays.asList(tr).stream().skip(this.size-n).reduce(0., (sum, i)->{sum+=i; return sum;});
    	//System.out.println("close="+close[this.size-1]+" atr="+total/n);
    	//System.out.println("trsum="+total);
    	return total/n;
    }
    
    private double ewma(double pastEwma, double newVal, double alpha) {
    	//return pastEwma * (1-alpha) + newVal * alpha;
    	return pastEwma + alpha * (newVal - pastEwma);
    }
    
	///////////////////////Getter Setter///////////////////////////////
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public boolean isInited() {
		return inited;
	}

	public void setInited(boolean inited) {
		this.inited = inited;
	}

	public Deque<Double> getOpenArray() {
		return openArray;
	}

	public void setOpenArray(Deque<Double> openArray) {
		this.openArray = openArray;
	}

	public Deque<Double> getHighArray() {
		return highArray;
	}

	public void setHighArray(Deque<Double> highArray) {
		this.highArray = highArray;
	}

	public Deque<Double> getLowArray() {
		return lowArray;
	}

	public void setLowArray(Deque<Double> lowArray) {
		this.lowArray = lowArray;
	}

	public Deque<Double> getCloseArray() {
		return closeArray;
	}

	public void setCloseArray(Deque<Double> closeArray) {
		this.closeArray = closeArray;
	}

	public Deque<Integer> getVolumeArray() {
		return volumeArray;
	}

	public void setVolumeArray(Deque<Integer> volumeArray) {
		this.volumeArray = volumeArray;
	}
	
	public static void main(String[] args) {
		ArrayDeque<Integer> test = new ArrayDeque<Integer>();
		test.add(1);
		test.add(2);
		test.add(3);
		test.add(4);
		test.add(5);
//		test.add(6);
//		test.add(7);
//		test.add(8);
//		test.add(9);
//		test.add(10);
		
		int total = test.stream().skip(0).reduce(0, (sum, i)->{sum+=i; return sum;});
		System.out.println(total);
	}
}

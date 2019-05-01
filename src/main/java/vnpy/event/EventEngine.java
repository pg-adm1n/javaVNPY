package vnpy.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import vnpy.trader.VtLogData;
import vnpy.utils.Method;

public class EventEngine {

	private LinkedBlockingQueue<Event> __queue; // 事件队列
	private boolean __active; // 事件引擎开关

	private Thread __thread; // 事件处理线程
	private Thread __timer; // 计时器，用于触发计时器事件
	private boolean __timerActive; // 计时器工作状态
	private int __timerSleep; // 计时器触发间隔（默认1秒）

	private Map<String, List<Method>> __handlers; // 这里的__handlers是一个字典，用来保存对应的事件调用关系
	private List<Method> __generalHandlers; // __generalHandlers是一个列表，用来保存通用回调函数（所有事件均调用）

	// 初始化事件引擎
	public EventEngine() {
		// 事件队列
		this.__queue = new LinkedBlockingQueue<Event>();

		// 事件引擎开关
		this.__active = false;

		// 事件处理线程
		this.__thread = new Thread() {
			@Override
			public void run() {
				__run();
			}
		};

		// 计时器，用于触发计时器事件
		this.__timer = new Thread() {
			@Override
			public void run() {
				__runTimer();
			}
		};
		this.__timerActive = false; // 计时器工作状态
		this.__timerSleep = 1000; // 计时器触发间隔（默认1秒）

		// 这里的__handlers是一个字典，用来保存对应的事件调用关系
		// 其中每个键对应的值是一个列表，列表中保存了对该事件进行监听的函数功能
		this.__handlers = new ConcurrentHashMap<String, List<Method>>();

		// __generalHandlers是一个列表，用来保存通用回调函数（所有事件均调用）
		this.__generalHandlers = new CopyOnWriteArrayList<Method>();
	}

	// 引擎运行
	private synchronized void __run() {
		while (this.__active) {
			try {
				Event event = this.__queue.poll(1000, TimeUnit.MILLISECONDS);
				if (event == null) {
					continue;
				}
				this.__process(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 处理事件
	private void __process(Event event) {
		// 检查是否存在对该事件进行监听的处理函数
		if (this.__handlers.containsKey(event.getType_())) {
			// 若存在，则按顺序将事件传递给处理函数执行
			for (Method handler : this.__handlers.get(event.getType_())) {
				handler.invoke(event);
			}
		}

		// 调用通用处理函数进行处理
		if (this.__generalHandlers != null) {
			for (Method handler : this.__generalHandlers) {
				handler.invoke(event);
			}
		}
	}

	// 运行在计时器线程中的循环函数
	private void __runTimer() {
		while (this.__timerActive) {
			// 创建计时器事件
			Event event = new Event(EventType.EVENT_TIMER);

			// 向队列中存入计时器事件
			this.put(event);

			// 等待
			try {
				Thread.sleep(this.__timerSleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// 向事件队列中存入事件
	public void put(Event event) {
		this.__queue.offer(event);
	}

	// 注册事件处理函数监听
	public void register(String type_, Method handler) {
		// 尝试获取该事件类型对应的处理函数列表，若无defaultDict会自动创建新的list
		List<Method> handlerList = this.__handlers.getOrDefault(type_, new CopyOnWriteArrayList<Method>());

		// 若要注册的处理器不在该事件的处理器列表中，则注册该事件
		if (!handlerList.contains(handler)) {
			handlerList.add(handler); 
		}
		this.__handlers.putIfAbsent(type_, handlerList);
	}

	// 注销事件处理函数监听
	public void unregister(String type_, Method handler) {
		// 尝试获取该事件类型对应的处理函数列表，若无则忽略该次注销请求
		List<Method> handlerList = this.__handlers.get(type_);

		// 如果该函数存在于列表中，则移除
		if (handlerList.contains(handler)) {
			handlerList.remove(handler);
		}

		// 如果函数列表为空，则从引擎中移除该事件类型
		if (handlerList == null || handlerList.size() == 0) {
			this.__handlers.remove(type_);
		}
	}

	// 注册通用事件处理函数监听
	public void registerGeneralHandler(Method handler) {
		if (!this.__generalHandlers.contains(handler)) {
			this.__generalHandlers.add(handler);
		}
	}

	// 注销通用事件处理函数监听
	public void unregisterGeneralHandler(Method handler) {
		if (this.__generalHandlers.contains(handler)) {
			this.__generalHandlers.remove(handler);
		}
	}

	public void start() {
		start(true);
	}

	// 引擎启动
	// timer：是否要启动计时器
	public void start(boolean timer) {
		// 将引擎设为启动
		this.__active = true;

		// 启动事件处理线程
		this.__thread.start();

		// 启动计时器，计时器事件间隔默认设定为1秒
		if (timer) {
			this.__timerActive = true;
			this.__timer.start();
		}
	}

	// 停止引擎
	public void stop() {
		// 将引擎设为停止
		this.__active = false;

		// 停止计时器
		this.__timerActive = false;
		try {
			this.__timer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// 等待事件处理线程退出
		try {
			this.__thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void simpletest(Event event) {
		System.out.println("处理每秒触发的计时器事件：" + LocalDateTime.now());
	}
	
	public static void main(String[] args) {
		EventEngine ee = new EventEngine();
		Method simpletest = new Method(ee, "simpletest", Event.class);
		ee.registerGeneralHandler(simpletest);
		ee.start(false);
		
	}
}

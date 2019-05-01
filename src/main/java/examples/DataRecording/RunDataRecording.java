package examples.DataRecording;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import vnpy.event.Event;
import vnpy.event.EventEngine;
import vnpy.event.EventType;
import vnpy.trader.LogEngine;
import vnpy.trader.MainEngine;
import vnpy.trader.VtErrorData;
import vnpy.trader.app.dataRecorder.DrEngine;
import vnpy.trader.gateway.ctpGateway.CtpGateway;
import vnpy.utils.AppException;
import vnpy.utils.Method;

public class RunDataRecording {

    // 父进程运行函数
    private static void runParentProcess() {
        // 创建日志引擎
        LogEngine le = LogEngine.getInstance();
        le.setLogLevel(LogEngine.LEVEL_INFO);
        le.addConsoleHandler();
        le.info("启动行情记录守护父进程");

        LocalTime DAY_START = LocalTime.of(8, 57); // 日盘启动和停止时间
        LocalTime DAY_END = LocalTime.of(15, 18);
        LocalTime NIGHT_START = LocalTime.of(20, 57); // 夜盘启动和停止时间
        LocalTime NIGHT_END = LocalTime.of(2, 33);

        Thread p = null; // 子进程句柄

        while (true) {
            LocalTime currentTime = LocalTime.now();
            boolean recording = false;

            // 判断当前处于的时间段
            if ((currentTime.compareTo(DAY_START) >= 0 && currentTime.compareTo(DAY_END) <= 0)
                || (currentTime.compareTo(NIGHT_START) >= 0 || currentTime.compareTo(NIGHT_END) <= 0)) {
                recording = true;
            }

            // 过滤周末时间段：周六全天，周五夜盘，周日日盘
            if ((DayOfWeek.SUNDAY.equals(LocalDate.now().getDayOfWeek()))
                || (DayOfWeek.SATURDAY.equals(LocalDate.now().getDayOfWeek())
                && currentTime.compareTo(NIGHT_END) > 0)
                || (DayOfWeek.MONDAY.equals(LocalDate.now().getDayOfWeek())
                && currentTime.compareTo(DAY_START) < 0)) {
                recording = false;
            }

            // 测试代码
            recording = true;

            // 记录时间则需要启动子进程
            if (recording && p == null) {
                le.info("启动子进程");
                p = new Thread() {
                    @Override
                    public void run() {
                        runChildProcess();
                    }
                };
                p.start();
                le.info("子进程启动成功");
            }

            // 非记录时间则退出子进程
            if (!recording && p != null) {
                le.info("关闭子进程");
                p.interrupt();
                try {
                    p.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    le.exception("join 出错", e);
                    throw new AppException("join 出错");
                }
                p = null;
                le.info("子进程关闭成功");
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                le.exception("sleep 出错", e);
                throw new AppException("sleep 出错");
            }
        }

    }

    // 子进程运行函数
    private static void runChildProcess() {
        System.out.println("--------------------");

        // 创建日志引擎
        LogEngine le = LogEngine.getInstance();
        le.setLogLevel(LogEngine.LEVEL_INFO);
        le.addConsoleHandler();
        le.info("启动行情记录运行子进程");

        EventEngine ee = new EventEngine();
        le.info("事件引擎创建成功");

        MainEngine me = new MainEngine(ee);
        me.addGateway(CtpGateway.class);
        me.addApp(DrEngine.class);
        le.info("主引擎创建成功");

        ee.register(EventType.EVENT_LOG, new Method(le, "processLogEvent", Event.class));
        ee.register(EventType.EVENT_ERROR, new Method(RunDataRecording.class, "processErrorEvent", Event.class));
        le.info("注册日志事件监听");

        me.connect("CTP");
        le.info("连接CTP接口");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 处理错误事件
    // 错误信息在每次登陆后，会将当日所有已产生的均推送一遍，所以不适合写入日志
    private static void processErrorEvent(Event event) {
        VtErrorData error = (VtErrorData) event.getDict_().get("data");
        System.out.println(String.format("错误代码：%s，错误信息：%s", error.getErrorID(), error.getErrorMsg()));
    }

    public static void main(String[] args) {
        // runParentProcess();
        runChildProcess();
    }
}

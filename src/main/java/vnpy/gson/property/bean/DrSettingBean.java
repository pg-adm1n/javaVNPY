package vnpy.gson.property.bean;

import java.util.List;
import java.util.Map;

public class DrSettingBean {
	private boolean working;
	private String marketCloseTime;
	private List<List<String>> tick;
	private List<List<String>> bar;
	private Map<String, String> active;
	public boolean isWorking() {
		return working;
	}
	public void setWorking(boolean working) {
		this.working = working;
	}
	public String getMarketCloseTime() {
		return marketCloseTime;
	}
	public void setMarketCloseTime(String marketCloseTime) {
		this.marketCloseTime = marketCloseTime;
	}
	public List<List<String>> getTick() {
		return tick;
	}
	public void setTick(List<List<String>> tick) {
		this.tick = tick;
	}
	public List<List<String>> getBar() {
		return bar;
	}
	public void setBar(List<List<String>> bar) {
		this.bar = bar;
	}
	public Map<String, String> getActive() {
		return active;
	}
	public void setActive(Map<String, String> active) {
		this.active = active;
	}
}

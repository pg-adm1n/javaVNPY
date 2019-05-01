package vnpy.trader;

import vnpy.event.EventEngine;

public abstract class VtAppModule {
	private String appName;
	public MainEngine mainEngine;
	public EventEngine eventEngine;

	public VtAppModule(MainEngine mainEngine, EventEngine eventEngine) {
		this.mainEngine = mainEngine;
		this.eventEngine = eventEngine;
	}
	
	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}
}

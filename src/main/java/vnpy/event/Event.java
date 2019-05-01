package vnpy.event;

import java.util.HashMap;
import java.util.Map;

// 事件对象
public class Event {
	
	private String type_; // 事件类型
	private Map<String, Object> dict_; // 字典用于保存具体的事件数据
	
	// Constructor
	public Event() {
		this(null);
	}
	// Constructor
    public Event(String type_) {
        this.type_ = type_;      // 事件类型
        this.dict_ = new HashMap<String, Object>();         // 字典用于保存具体的事件数据
    }
	public String getType_() {
		return type_;
	}
	public void setType_(String type_) {
		this.type_ = type_;
	}
	public Map<String, Object> getDict_() {
		return dict_;
	}
	public void setDict_(Map<String, Object> dict_) {
		this.dict_ = dict_;
	}
}

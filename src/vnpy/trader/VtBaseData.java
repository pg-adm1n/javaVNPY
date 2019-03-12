package vnpy.trader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// 回调函数推送数据的基础类，其他数据类继承于此
public class VtBaseData implements Serializable  {
	private static final long serialVersionUID = 1L;
	
	private String gatewayName; // Gateway名称
	private Object rawData; // 原始数据
	
	public String getGatewayName() {
		return gatewayName;
	}
	public void setGatewayName(String gatewayName) {
		this.gatewayName = gatewayName;
	}
	public Object getRawData() {
		return rawData;
	}
	public void setRawData(Object rawData) {
		this.rawData = rawData;
	}
	
	public VtBaseData clone() {
		VtBaseData obj = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            obj = (VtBaseData) ois.readObject();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return obj;
    }
}

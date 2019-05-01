package vnpy.trader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import vnpy.utils.AppException;

public class VtFunction {
	// 加载全局配置
	public static Map<String, String> loadJsonSetting(String settingFilePath) {
		// 加载JSON配置
		Map<String, String> setting = new HashMap<String, String>();

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(settingFilePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new AppException("找不到配置文件");
		}
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				String[] values = line.split(",");
				setting.put(values[0], values[1]);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new AppException("读取配置文件出错");
		}
		return setting;
	}

	public static <T> T[] arrayAppend(final T[] array1, final T... array2) {
		if (array1 == null) {
			return array2.clone();
		} else if (array2 == null) {
			return array1.clone();
		}
		final Class<?> type1 = array1.getClass().getComponentType();
		@SuppressWarnings("unchecked") // OK, because array is of type T
		final
		// a处
		T[] joinedArray = (T[]) Array.newInstance(type1, array1.length + array2.length);
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		try {
			// b处
			System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		} catch (final ArrayStoreException ase) {
			// Check if problem was due to incompatible types
			/*
			 * We do this here, rather than before the copy because: - it would be a wasted
			 * check most of the time - safer, in case check turns out to be too strict
			 */
			final Class<?> type2 = array2.getClass().getComponentType();
			if (!type1.isAssignableFrom(type2)) {
				throw new IllegalArgumentException(
						"Cannot store " + type2.getName() + " in an array of " + type1.getName(), ase);
			}
			throw ase; // No, so rethrow original
		}
		return joinedArray;
	}
	
	
	public static void main(String[] args) {
		String[] a = new String[] {"1","3","5"};
		String[] b = new String[] {"2","4","6","8"};
		String[] c = arrayAppend(a, b);
		for (int i = 0; i < c.length; i++) {
			System.out.println(c[i]);
		}
	}
}

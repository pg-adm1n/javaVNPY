package vnpy.trader.app.ctaStrategy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import vnpy.trader.VtBarData;
import vnpy.trader.VtGlobal;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class CtaHistoryData {
	// 将Multicharts导出的csv格式的历史数据插入到Mongo数据库中
	public static void loadMcCsv(String fileName, String dbName, String symbol) {
		long start = System.currentTimeMillis();
		System.out.println("开始读取CSV文件" + fileName + "中的数据插入到" + dbName + "的" + symbol + "中");
		
		// 锁定集合，并创建索引
		MongoClient client = MongoClients.create(
				"mongodb://" + VtGlobal.globalSetting.get("mongoHost") + ":" + VtGlobal.globalSetting.get("mongoPort"));

		CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
		MongoDatabase database = client.getDatabase(dbName).withCodecRegistry(pojoCodecRegistry);
		MongoCollection<VtBarData> collection = database.getCollection(symbol, VtBarData.class);
		
		collection.createIndex(Indexes.ascending("datetime"), new IndexOptions().unique(true));

		// 读取数据和插入到数据库
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(fileName));
			br.readLine();// 去掉表头
			// "Date","Time","Open","High","Low","Close","TotalVolume"
			String line;
			String[] values;
			VtBarData bar;
			DateTimeFormatter yyyy_MM_dd = DateTimeFormatter.ofPattern("yyyy-M-d");
			DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");
			DateTimeFormatter yyyyMMdd_HHmmss = DateTimeFormatter.ofPattern("yyyyMMdd H:m:s");
			while ((line = br.readLine()) != null) {
				values = line.split(",");

				bar = new VtBarData();
				bar.setDate(LocalDate.parse(values[0], yyyy_MM_dd).format(yyyyMMdd));
				bar.setTime(values[1]);
				bar.setDatetime(LocalDateTime.parse(bar.getDate() + " " + bar.getTime(), yyyyMMdd_HHmmss));
				bar.setVtSymbol(symbol);
				bar.setSymbol(symbol);
				bar.setOpen(Double.parseDouble(values[2]));
				bar.setHigh(Double.parseDouble(values[3]));
				bar.setLow(Double.parseDouble(values[4]));
				bar.setClose(Double.parseDouble(values[5]));
				bar.setVolume(Integer.parseInt(values[6]));

				collection.insertOne(bar);
				System.out.println(bar.getDate() + " " + bar.getTime());
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		System.out.println("插入完毕，耗时：" + (end - start) + "ms");
	}

}

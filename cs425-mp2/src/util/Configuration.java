package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Configuration {

	private static volatile Configuration INSTANCE = null;
	private static String CONFIG_PATH = "config.txt";

	public static String orderingType = null;
	public static int numProc = 0;
	public static String[] IP;
	public static int[] delayTime;
	public static int[] dropRate;

	public static Configuration getInstance() {
		if (INSTANCE == null) {
			synchronized (Configuration.class) {
				if (INSTANCE == null) {
					INSTANCE = new Configuration();
				}
			}
		}
		return INSTANCE;
	}

	private Configuration() {
		initialize();
		setVariables();
	}

	private void initialize() {
		IP = new String[6];
		delayTime = new int[6];
		dropRate = new int[6];
	}

	private void setVariables() {
		String currLine = "";
		BufferedReader br;
		String[] strArr = null;
		String val = "";
		try {
			br = new BufferedReader(new FileReader(CONFIG_PATH));
			while ((currLine = br.readLine()) != null) {
				strArr = currLine.split(" ");
				if (strArr.length == 3) {
					val = strArr[2];
					switch (strArr[1]) {
					case "OrderingType":
						orderingType = val;
						break;
					case "NumberProc":
						numProc = Integer.valueOf(val);
						break;
					
					// Assign IP
					case "IP_P0":
						IP[0] = val;
						break;
					case "IP_P1":
						IP[1] = val;
						break;
					case "IP_P2":
						IP[2] = val;
						break;
					case "IP_P3":
						IP[3] = val;
						break;
					case "IP_P4":
						IP[4] = val;
						break;
					case "IP_P5":
						IP[5] = val;
						break;
						
					// Assign delayTime
					case "Delay_P0":
						delayTime[0] = Integer.valueOf(val);
						break;
					case "Delay_P1":
						delayTime[1] = Integer.valueOf(val);
						break;
					case "Delay_P2":
						delayTime[2] = Integer.valueOf(val);
						break;
					case "Delay_P3":
						delayTime[3] = Integer.valueOf(val);
						break;
					case "Delay_P4":
						delayTime[4] = Integer.valueOf(val);
						break;
					case "Delay_P5":
						delayTime[5] = Integer.valueOf(val);
						break;
						
					// Assign dropRate
					case "DropRate_P0":
						dropRate[0] = Integer.valueOf(val);
						break;
					case "DropRate_P1":
						dropRate[1] = Integer.valueOf(val);
						break;
					case "DropRate_P2":
						dropRate[2] = Integer.valueOf(val);
						break;
					case "DropRate_P3":
						dropRate[3] = Integer.valueOf(val);
						break;
					case "DropRate_P4":
						dropRate[4] = Integer.valueOf(val);
						break;
					case "DropRate_P5":
						dropRate[5] = Integer.valueOf(val);
						break;
					}
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

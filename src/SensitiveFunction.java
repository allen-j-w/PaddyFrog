import java.util.HashSet;


public class SensitiveFunction {
	
	private static final HashSet<String> functionTable = new HashSet<>();
	
	static{
		functionTable.add("Ljava/lang/System;->loadLibrary(");
		functionTable.add("Ljava/lang/Runtime;->exec(");
		functionTable.add("Ljava/net/Socket;-><init>(");
		
		functionTable.add("Landroid/media/MediaRecorder;->setAudioSource(");
		functionTable.add("Landroid/media/MediaRecorder;->setVideoSource(");
		functionTable.add("Landroid/net/wifi/WifiConfiguration;->toString(");
		functionTable.add("Landroid/net/ConnectivityManager;->getActiveNetworkInfo(");
		functionTable.add("Landroid/net/ConnectivityManager;->isActiveNetworkMetered(");
//		functionTable.add("Landroid/util/Log;->d(");
//		functionTable.add("Landroid/util/Log;->e(");
//		functionTable.add("Landroid/util/Log;->w(");
//		functionTable.add("Landroid/util/Log;->i(");
//		functionTable.add("Landroid/util/Log;->v(");
//		functionTable.add("Landroid/util/Log;->wtf(");
		functionTable.add("Landroid/content/pm/PackageManager;->getPackageInfo(");
		functionTable.add("Landroid/location/LocationManager;->getProviders(");
		functionTable.add("Landroid/telephony/SmsManager;->sendTextMessage(");
		functionTable.add("Landroid/telephony/gsm/GsmCellLocation;->getLac(");
		functionTable.add("Landroid/telephony/gsm/GsmCellLocation;->getCid(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getCallState(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getCellLocation(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getDataActivity(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getDataState(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getDeviceId(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getDeviceSoftwareVersion(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getLine1Number(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getNeighboringCellInfo(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getNetworkCountryIso(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getNetworkOperator(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getNetworkOperatorName(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getNetworkType(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getPhoneType(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getSimCountryIso(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getSimOperator(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getSimOperatorName(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getSimSerialNumber(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getSimState(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getSubscriberId(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getVoiceMailAlphaTag(");
		functionTable.add("Landroid/telephony/TelephonyManager;->getVoiceMailNumber(");
	}
	
	public static boolean isSensitiveFunction(String functionName)
	{
		//functionName不包括前边invoke那一通，从类名开始
		String func = functionName.substring(0, functionName.indexOf('(') + 1);
//		System.out.println(func);
		return functionTable.contains(func);
	}

}

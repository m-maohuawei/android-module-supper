package org.ms.module.impl.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import org.ms.module.supper.client.Modules;
import org.ms.module.supper.inter.utils.ISystemUtils;

import java.io.*;
import java.net.NetworkInterface;
import java.util.*;

import static android.content.Context.WIFI_SERVICE;
import static android.os.Build.*;

public class SystemUtils implements ISystemUtils {

    private static SystemUtils systemUtils = new SystemUtils();

    public static SystemUtils getInstance() {
        return systemUtils;
    }

    @Override
    public boolean isMobilePhone() {
        return true;
    }

    @Override
    public String getIp() {
        final WifiManager wifiManager = (WifiManager) Modules.getDataModule().getAppData().getApplication()
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        String ipStr = intToIp(ip);
        return ipStr;
    }

    private static String intToIp(int i) {

        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + (i >> 24 & 0xFF);
    }


    public static String getSysInfo() {
        String phoneInfo = "Product: " + Build.PRODUCT;
        phoneInfo += ", CPU_ABI: " + Build.CPU_ABI;
        phoneInfo += ", TAGS: " + Build.TAGS;
        phoneInfo += ", VERSION_CODES.BASE: "
                + VERSION_CODES.BASE;
        phoneInfo += ", MODEL: " + Build.MODEL;
        phoneInfo += ", SDK: " + VERSION.SDK;
        phoneInfo += ", VERSION.RELEASE: " + VERSION.RELEASE;
        phoneInfo += ", DEVICE: " + Build.DEVICE;
        phoneInfo += ", DISPLAY: " + Build.DISPLAY;
        phoneInfo += ", BRAND: " + Build.BRAND;
        phoneInfo += ", BOARD: " + Build.BOARD;
        phoneInfo += ", FINGERPRINT: " + Build.FINGERPRINT;
        phoneInfo += ", ID: " + Build.ID;
        phoneInfo += ", MANUFACTURER: " + Build.MANUFACTURER;
        phoneInfo += ", USER: " + Build.USER;

        return phoneInfo;
    }


    /**
     * 获取手机手机号
     *
     * @return
     */
    public static String getPhoneNum() {
        TelephonyManager tm = (TelephonyManager) Modules.getDataModule().getAppData().getApplication()
                .getSystemService(Context.TELEPHONY_SERVICE);
        @SuppressLint("MissingPermission")
        String phoneId = tm.getLine1Number();
        return phoneId;
    }


    @Override
    public String getImei() {
        try {
            //实例化TelephonyManager对象
            TelephonyManager telephonyManager = (TelephonyManager) Modules.getDataModule().getAppData().getApplication().getSystemService(Context.TELEPHONY_SERVICE);
            //获取IMEI号
            @SuppressLint("MissingPermission")
            String imei = telephonyManager.getDeviceId();

            return imei;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getAndroidId() {
        String ANDROID_ID = Settings.System.getString(
                Modules.getDataModule().getAppData().getApplication().getContentResolver(), Settings.Secure.ANDROID_ID);
        return ANDROID_ID;
    }

    @Override
    public String getUmid() {
        return UMIDUtils.getUmid();
    }


    /**
     * 默认的MAC
     */
    private static final String marshmallowMacAddress = "02:00:00:00:00:00";

    /**
     * MAC 位置
     */
    private static final String fileAddressMac = "/sys/class/net/wlan0/address";

    @Override
    public String getMac() {
        WifiManager wifiMan = (WifiManager) Modules.getDataModule().getAppData().getApplication()
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();

        if (wifiInf != null
                && marshmallowMacAddress.equals(wifiInf.getMacAddress())) {
            String result = null;
            try {
                result = getAdressMacByInterface();
                if (result != null) {
                    return result;
                } else {
                    result = getAddressMacByFile(wifiMan);
                    return result;
                }
            } catch (IOException e) {
                Log.e("MobileAccess", "Erreur lecture propriete Adresse MAC");
            } catch (Exception e) {
                Log.e("MobileAcces", "Erreur lecture propriete Adresse MAC ");
            }
        } else {
            if (wifiInf != null && wifiInf.getMacAddress() != null) {
                return wifiInf.getMacAddress();
            } else {
                return "";
            }
        }
        return marshmallowMacAddress;
    }

    @SuppressLint("NewApi")
    private static String getAdressMacByInterface() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface
                    .getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (nif.getName().equalsIgnoreCase("wlan0")) {
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return null;
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:", b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }

        } catch (Exception e) {
            Log.e("MobileAcces", "Erreur lecture propriete Adresse MAC ");
        }
        return null;
    }

    private static String getAddressMacByFile(WifiManager wifiMan)
            throws Exception {
        String ret;
        int wifiState = wifiMan.getWifiState();

        wifiMan.setWifiEnabled(true);
        File fl = new File(fileAddressMac);
        FileInputStream fin = new FileInputStream(fl);
        ret = crunchifyGetStringFromStream(fin);
        fin.close();

        boolean enabled = WifiManager.WIFI_STATE_ENABLED == wifiState;
        wifiMan.setWifiEnabled(enabled);
        return ret;
    }

    private static String crunchifyGetStringFromStream(
            InputStream crunchifyStream) throws IOException {
        if (crunchifyStream != null) {
            Writer crunchifyWriter = new StringWriter();

            char[] crunchifyBuffer = new char[2048];
            try {
                Reader crunchifyReader = new BufferedReader(
                        new InputStreamReader(crunchifyStream, "UTF-8"));
                int counter;
                while ((counter = crunchifyReader.read(crunchifyBuffer)) != -1) {
                    crunchifyWriter.write(crunchifyBuffer, 0, counter);
                }
            } finally {
                crunchifyStream.close();
            }
            return crunchifyWriter.toString();
        } else {
            return "No Contents";
        }
    }


    @Override
    public String getIpV6() {
        return IPv6AddressUtils.getInstance().getIpv6AddrString();
    }

    @Override
    public List<Map<String, String>> getAppList() {
        List<Map<String, String>> list = new ArrayList<>();
        List<PackageInfo> packages = Modules.getDataModule().getAppData().getApplication().getPackageManager().getInstalledPackages(0);

        for (int j = 0; j < packages.size(); j++) {
            PackageInfo packageInfo = packages.get(j);
            // 显示非系统软件
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packageInfo.applicationInfo.loadLabel(Modules.getDataModule().getAppData().getApplication().getPackageManager()).toString();
                String packageName = packageInfo.packageName;
                Drawable appIcon = packageInfo.applicationInfo.loadIcon(Modules.getDataModule().getAppData().getApplication().getPackageManager()).getCurrent();

                Map<String, String> map = new HashMap<>();
                map.put(packageName, appName);
                list.add(map);
            }
        }
        return list;
    }

    @Override
    public List<Map<String, String>> getRunningProcess() {

        List<Map<String, String>> list = new ArrayList<>();
        PackageManager pm = Modules.getDataModule().getAppData().getApplication().getApplicationContext().getPackageManager();
        List<ApplicationInfo> applications = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);

        ActivityManager activityManager = (ActivityManager) Modules.getDataModule().getAppData().getApplication().getSystemService(Context.ACTIVITY_SERVICE);
        // 获取正在运行的应用
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo ra : runningAppProcesses) {
            String processName = ra.processName;
            for (ApplicationInfo applicationInfo : applications) {
                if (processName.equals(applicationInfo.processName)) {
                    String appName = applicationInfo.loadLabel(Modules.getDataModule().getAppData().getApplication().getPackageManager()).toString();
                    String packageName = applicationInfo.packageName;
                    Drawable appIcon = applicationInfo.loadIcon(Modules.getDataModule().getAppData().getApplication().getPackageManager()).getCurrent();
                    HashMap<String, String> stringStringHashMap = new HashMap<>();
                    stringStringHashMap.put(packageName, appName);
                    list.add(stringStringHashMap);
                }
            }
        }
        return list;
    }

    @Override
    public String getCpuName() {
        String str1 = "/proc/cpuinfo";
        String str2 = "";

        try {
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr);
            while ((str2 = localBufferedReader.readLine()) != null) {
                if (str2.contains("Hardware")) {
                    return str2.split(":")[1];
                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String printResolution() {
        DisplayMetrics dm = Modules.getDataModule().getAppData().getApplication().getResources().getDisplayMetrics();
        int height = dm.heightPixels;
        int width = dm.widthPixels;
        int sw = Modules.getDataModule().getAppData().getApplication().getResources().getConfiguration().smallestScreenWidthDp;
        return width + "x" + height;
    }

    @Override
    public String getCpuAbi() {
        String cpuAbi = CPU_ABI;
        return cpuAbi;
    }

    @Override
    public String getProduct() {
        String product = PRODUCT;
        return product;
    }

    @Override
    public String getOsType() {
        return "Android";
    }

    @Override
    public String getOSVersion() {
        String version = VERSION.CODENAME;
        return version;
    }

    @Override
    public String getBrand() {
        String brand = BRAND;
        return brand;
    }

    @Override
    public String getSSID() {
        WifiManager wm = (WifiManager) Modules.getDataModule().getAppData().getApplication().getSystemService(WIFI_SERVICE);
        if (wm != null) {
            WifiInfo winfo = wm.getConnectionInfo();
            if (winfo != null) {
                String s = winfo.getSSID();
                if (s.length() > 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
                    return s.substring(1, s.length() - 1);
                }
            }
        }
        return null;
    }

    @Override
    public String getCurrentLanguage() {
        Locale locale = Modules.getDataModule().getAppData().getApplication().getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String lc = language + "_" + country;
        return lc;
    }

    @Override
    public String adCode() {
        return AdCodeUtils.getAdCode();
    }

    @Override
    public String getCurrentTimeZone() {
        TimeZone tz = TimeZone.getDefault();
        String strTz = tz.getDisplayName(false, TimeZone.SHORT);
        return strTz;
    }

    @Override
    public String getNetWorkTypeName() {
        return NetworkUtils.getInstance().getNetWorkTypeName();
    }


    @Override
    public String getProcessName() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + android.os.Process.myPid() + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }
}

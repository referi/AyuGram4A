/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.proxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.radolyn.ayugram.AyuConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.util.List;

public class AyuSmartProxyController {

    private static volatile boolean isBypassing = false;
    private static volatile String bypassReason = null;
    private static boolean inited = false;

    public static boolean isBypassing() {
        return isBypassing;
    }

    public static String getBypassReason() {
        return bypassReason;
    }

    public static void setInitialBypass(boolean bypassing) {
        isBypassing = bypassing;
    }

    public static boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return false;
        }
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isVpnActive() {
        try {
            Context context = ApplicationLoader.applicationContext;
            if (context == null) {
                return false;
            }
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return true;
                    }
                }
                Network[] allNetworks = cm.getAllNetworks();
                if (allNetworks != null) {
                    for (Network net : allNetworks) {
                        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            return true;
                        }
                    }
                }
            } else {
                NetworkInfo activeInfo = cm.getActiveNetworkInfo();
                if (activeInfo != null && activeInfo.getType() == ConnectivityManager.TYPE_VPN) {
                    return true;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return false;
    }

    public static String getCurrentWifiSsid() {
        try {
            Context context = ApplicationLoader.applicationContext;
            if (context == null) {
                return null;
            }

            // Android 10+ (API 29+) via NetworkCapabilities TransportInfo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Network activeNetwork = cm.getActiveNetwork();
                    if (activeNetwork != null) {
                        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            TransportInfo transportInfo = caps.getTransportInfo();
                            if (transportInfo instanceof WifiInfo) {
                                String ssid = cleanSsid(((WifiInfo) transportInfo).getSSID());
                                if (ssid != null) {
                                    return ssid;
                                }
                            }
                        }
                    }
                }
            }

            // Fallback via WifiManager
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null) {
                    String ssid = cleanSsid(info.getSSID());
                    if (ssid != null) {
                        return ssid;
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return null;
    }

    private static String cleanSsid(String ssid) {
        if (ssid == null) {
            return null;
        }
        ssid = ssid.trim();
        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() >= 2) {
            ssid = ssid.substring(1, ssid.length() - 1).trim();
        }
        if (TextUtils.isEmpty(ssid) || "<unknown ssid>".equalsIgnoreCase(ssid)) {
            return null;
        }
        return ssid;
    }

    public static boolean isCurrentWifiTrusted() {
        if (!AyuConfig.smartProxyWifi) {
            return false;
        }
        String currentSsid = getCurrentWifiSsid();
        if (TextUtils.isEmpty(currentSsid)) {
            return false;
        }
        List<String> trustedList = AyuConfig.getSmartProxySsidsList();
        for (String trusted : trustedList) {
            if (currentSsid.equalsIgnoreCase(trusted.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldBypassAtStart() {
        try {
            AyuConfig.loadConfig();
            if (AyuConfig.smartProxyVpn && isVpnActive()) {
                isBypassing = true;
                bypassReason = "VPN";
                return true;
            }
            if (AyuConfig.smartProxyWifi && isCurrentWifiTrusted()) {
                isBypassing = true;
                String ssid = getCurrentWifiSsid();
                bypassReason = ssid != null ? ssid : "Wi-Fi";
                return true;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return false;
    }

    public static void init() {
        if (inited) {
            return;
        }
        inited = true;

        try {
            Context context = ApplicationLoader.applicationContext;
            if (context == null) {
                return;
            }

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        AndroidUtilities.runOnUIThread(AyuSmartProxyController::checkProxyState);
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        AndroidUtilities.runOnUIThread(AyuSmartProxyController::checkProxyState);
                    }

                    @Override
                    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                        AndroidUtilities.runOnUIThread(AyuSmartProxyController::checkProxyState);
                    }
                });
            }

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    AndroidUtilities.runOnUIThread(AyuSmartProxyController::checkProxyState);
                }
            };
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }

            checkProxyState();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static synchronized void checkProxyState() {
        try {
            SharedPreferences preferences = MessagesController.getGlobalMainSettings();
            boolean masterProxyEnabled = preferences.getBoolean("proxy_enabled", false);
            SharedConfig.ProxyInfo currentProxy = SharedConfig.currentProxy;

            if (!masterProxyEnabled || currentProxy == null) {
                if (isBypassing) {
                    isBypassing = false;
                    bypassReason = null;
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                }
                return;
            }

            boolean shouldBypass = false;
            String reason = null;

            if (AyuConfig.smartProxyVpn && isVpnActive()) {
                shouldBypass = true;
                reason = LocaleController.getString("SmartProxyBypassedVpn", R.string.SmartProxyBypassedVpn);
            } else if (AyuConfig.smartProxyWifi && isCurrentWifiTrusted()) {
                shouldBypass = true;
                String ssid = getCurrentWifiSsid();
                reason = LocaleController.formatString("SmartProxyBypassedWifi", R.string.SmartProxyBypassedWifi, ssid != null ? ssid : "");
            }

            if (shouldBypass) {
                bypassReason = reason;
                if (!isBypassing) {
                    isBypassing = true;
                    ConnectionsManager.setProxySettings(false, "", 1080, "", "", "");
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                }
            } else {
                if (isBypassing) {
                    isBypassing = false;
                    bypassReason = null;
                    ConnectionsManager.setProxySettings(true, currentProxy.address, currentProxy.port, currentProxy.username, currentProxy.password, currentProxy.secret);
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}

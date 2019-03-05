package de.bfpi.cordova.zebraprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.os.Looper;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Zebraprinter extends CordovaPlugin {
  private static final String LOG_TAG = "Zebraprinter";
  private static final String PRINT = "print";

  private JSONArray args;
  private CallbackContext callbackContext;
  private final int REQUEST_ACCESS_COARSE_LOCATION_CODE = 0;

  public ZebraPrinter() {
    Log.d(LOG_TAG, "Plugin created");
  }

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    this.args = args;
    this.callbackContext = callbackContext;
    if (action.equals("print")) {
      this.print();
    }

    return true;
  }

  private void print() {
    if(hasPermission()) {
      _print();
    }
    else {
      requestPermissionAndPrint();
    }
  }

  private void _print() {
    String macAddress = this.args.getString(0);
    String textToPrint = this.args.getString(1);
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Connection conn = new BluetoothConnection(macAddress);
          Looper.prepare();
          conn.open();
          String cpclData = "! 0 200 200 210 1\r\n"
            + "TEXT 4 0 30 40 This is a CPCL test.\r\n"
            + "FORM\r\n"
            + "PRINT\r\n";
          conn.write(cpclData.getBytes());
          Thread.sleep(500);
          conn.close();
          Looper.myLooper().quit();
        } catch (Exception e) {
          Log.e(LOG_TAG, "Exception: " + e.getMessage());
          callbackContext.error(e.getMessage());
        }
      }
    });
  }

  private boolean hasPermission() {
    return cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
  }

  private void requestPermissionAndPrint() {
    cordova.requestPermission(this, REQUEST_ACCESS_COARSE_LOCATION_CODE,
        Manifest.permission.ACCESS_COARSE_LOCATION);
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
    throws JSONException {
    for(int r:grantResults) {
      if(r == PackageManager.PERMISSION_DENIED) {
        Log.d(LOG_TAG, "Permission denied");
        this.cbContext.error("permissions");
        return;
      }
    }
    _print();
  }
}

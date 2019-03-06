package de.bfpi.cordova.zebraprinter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;

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

  public Zebraprinter() {
    Log.d(LOG_TAG, "Plugin created");
  }

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    this.args = args;
    this.callbackContext = callbackContext;
    if (action.equals("print")) {
      this.print();
    }
    else if (action.equals("discover")) {
      this.discover();
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
    String macAddress;
    String textToPrint;
    try {
      macAddress = this.args.getString(0);
      textToPrint = this.args.getString(1);
    }
    catch(JSONException e) {
      Log.e(LOG_TAG, "Exception: "+ e.getMessage());
      callbackContext.error(e.getMessage());
      return;
    }
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        try {
          Connection conn = new BluetoothConnection(macAddress);
          Looper.prepare();
          conn.open();
          conn.write(textToPrint.getBytes());
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
        this.callbackContext.error("permissions");
        return;
      }
    }
    _print();
  }

  private void discover() {
    final CallbackContext callbackContext = this.callbackContext;
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        if (Looper.myLooper() == null) {
          Looper.prepare();
        }
        try {
          BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
          if (bluetoothAdapter.isEnabled()) {
            Log.d(LOG_TAG, "Searching for printers...");
            JSONArray printer = new JSONArray();
            BluetoothDiscoverer.findPrinters(cordova.getActivity().getApplicationContext(),
              new DiscoveryHandler() {
                public void discoveryError(String message) {
                  Log.e(LOG_TAG, "An error occurred while searching for printers. Message: " + message);
                  callbackContext.success();
                }

                public void discoveryFinished() {
                  Log.d(LOG_TAG, "Finished searching for printers...");
                  callbackContext.success(printer);
                }

                public void foundPrinter(final DiscoveredPrinter p) {
                  Log.d(LOG_TAG, "Printer found: " + p.address);
                  printer.put(p.address);
                }
              });
          } else {
            Log.d(LOG_TAG, "Bluetooth is disabled...");
            callbackContext.error("Bluetooth is disabled");
          }

        } catch (ConnectionException e) {
          Log.e(LOG_TAG, "Connection exception: " + e.getMessage());
          callbackContext.error(e.getMessage());
        } finally {
          Looper.myLooper().quit();
        }
      }
    });
  }
}

package gd.phacsin.mdstech;

import android.*;
import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

/*import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;*/

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Created by GD on 2/3/2016.
 */
public class DetailFragment extends Fragment {
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    RequestParams params = new RequestParams();
    GoogleCloudMessaging gcmObj;
    String regId = "";
    ProgressDialog prgDialog;
    String EMAIL_ID;
    List<String> list = new ArrayList<>();
    TextView imei, gcm_id;
    DB snappydb;
    String IMEI;
    String Fileid= String.valueOf(123456789);
    String Pieceid=String.valueOf(1);
    SharedPreferences prefs;
    AppCompatButton test;
    private static final int REQUEST_CODE_EMAIL = 1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        imei = (TextView) rootView.findViewById(R.id.imei);
        gcm_id = (TextView) rootView.findViewById(R.id.gcm_id);


        prefs = getActivity().getSharedPreferences("UserDetails",
                Context.MODE_PRIVATE);
        gcm_id.setText(prefs.getString("regID", ""));
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED ) {


                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.READ_PHONE_STATE},
                        0);
            } else {
                TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
                IMEI = telephonyManager.getDeviceId();
                imei.setText(IMEI);
                if (!prefs.contains("email")) {
                    try {
                        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                        startActivityForResult(intent, REQUEST_CODE_EMAIL);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getActivity(), "Does not have Play Services installed", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            IMEI = telephonyManager.getDeviceId();
            imei.setText(IMEI);
            if (!prefs.contains("email")) {
                try {
                    Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                            new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                    startActivityForResult(intent, REQUEST_CODE_EMAIL);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), "Does not have Play Services installed", Toast.LENGTH_LONG).show();
                }
            }
        }


        return rootView;
    }

    private void registerInBackground(final String emailID) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcmObj == null) {
                        gcmObj = GoogleCloudMessaging
                                .getInstance(getActivity());
                    }
                    regId = gcmObj
                            .register(ApplicationConstants.GOOGLE_PROJ_ID);
                    msg = "Registration ID :" + regId;
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                if (!TextUtils.isEmpty(regId)) {
                    // Store RegId created by GCM Server in SharedPref
                    storeRegIdinSharedPref(getActivity(), emailID, regId);
                    storeRegIdinServer();
                    gcm_id.setText(regId);
                    Toast.makeText(
                            getActivity(),
                            "Registered with GCM Server successfully.\n\n"
                                    + msg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            getActivity(),
                            "Reg ID Creation Failed.\n\nEither you haven't enabled Internet or GCM server is busy right now. Make sure you enabled Internet and try registering again after some time."
                                    + msg, Toast.LENGTH_LONG).show();
                }
            }
        }.execute(null, null, null);
    }

    private void storeRegIdinSharedPref(Context context,
                                        String emailID, String regId) {
        SharedPreferences prefs = getActivity().getSharedPreferences("UserDetails",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("email", emailID);
        editor.putString("regID", regId);
        editor.commit();
    }

    // Share RegID with GCM Server Application (Php)
    private void storeRegIdinServer() {
        prgDialog = new ProgressDialog(getActivity());
        prgDialog.setCancelable(false);
        prgDialog.setMessage("Registering");
        prgDialog.setTitle("Push Notifications");
        prgDialog.show();
        params.put("gcm_id", regId);
        params.put("email", EMAIL_ID);
        params.put("imei", IMEI);

        // Make RESTful webservice call using AsyncHttpClient object
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(ApplicationConstants.APP_SERVER_URL, params,
                new AsyncHttpResponseHandler() {
                    // When the response returned by REST has Http
                    // response code '200'
                    @Override
                    public void onSuccess(String response) {
                        // Hide Progress Dialog
                        prgDialog.hide();
                        if (prgDialog != null) {
                            prgDialog.dismiss();
                        }
                        Toast.makeText(getActivity(),
                                "Registered Successfully",
                                Toast.LENGTH_LONG).show();
                        storeRegIdinSharedPref(getActivity(), EMAIL_ID, regId);
                    }

                    // When the response returned by REST has Http
                    // response code other than '200' such as '404',
                    // '500' or '403' etc
                    @Override
                    public void onFailure(int statusCode, Throwable error,
                                          String content) {
                        // Hide Progress Dialog
                        prgDialog.hide();
                        if (prgDialog != null) {
                            prgDialog.dismiss();
                        }
                        // When Http response code is '404'
                        if (statusCode == 404) {
                            Toast.makeText(getActivity(),
                                    "Requested resource not found",
                                    Toast.LENGTH_LONG).show();
                        }
                        // When Http response code other than 404, 500
                        else {
                            Toast.makeText(
                                    getActivity(),
                                    "Unexpected Error occcured! [Most common Error: Device might "
                                            + "not be connected to Internet or remote server is not up and running], check for other errors as well",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Check if Google Playservices is installed in Device or not
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getActivity());
        // When Play services not found in device
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                // Show Error dialog to install Play services
                GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(),
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(
                        getActivity(),
                        "This device doesn't support Play services, App will not work normally",
                        Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    // When Application is resumed, check for Play services support to make sure app will be running normally
    @Override
    public void onResume() {
        super.onResume();
        checkPlayServices();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!prefs.contains("email")) {
                        try {
                            Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                                    new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                            startActivityForResult(intent, REQUEST_CODE_EMAIL);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getActivity(), "Does not have Play Services installed", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                    break;
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EMAIL && resultCode == Activity.RESULT_OK) {
            EMAIL_ID = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            registerInBackground(EMAIL_ID);
        }
    }
}

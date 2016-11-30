package com.studiomoob.googlefitindoor;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.concurrent.TimeUnit;

import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    TextView txtLoading;
    TextView txtCollectData;
    float distance = 0;

    GoogleApiClient googleApiClient;
    static final int REQUEST_OAUTH = 1;
    static final String AUTH_PENDING = "auth_state_pending";
    boolean authInProgress = false;

    OnDataPointListener onDataPointListener;
    DataSource currentDataSource;
    Button btnStartCollectData;
    Button btnStopCollectData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
        {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        loadView();
    }

    private void loadView() {

        txtLoading = (TextView) findViewById(R.id.txtLoading);
        txtCollectData = (TextView) findViewById(R.id.txtCollectData);


        Button btnGoogleConnect = (Button) findViewById(R.id.btnGoogleConnect);
        btnGoogleConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestGoogleConnect();
            }
        });

        btnStartCollectData = (Button) findViewById(R.id.btnStartCollectData);
        btnStartCollectData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                distance = 0;
                txtCollectData.setText("");



                Fitness.SensorsApi.findDataSources(googleApiClient, new DataSourcesRequest.Builder().setDataTypes(DataType.TYPE_DISTANCE_DELTA).setDataSourceTypes(DataSource.TYPE_DERIVED).build()).setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult)
                    {
                        if (dataSourcesResult.getDataSources().size() > 0)
                        {
                            currentDataSource = dataSourcesResult.getDataSources().get(0);
                            indoorRegisterFitnessDataListener();

                            btnStartCollectData.setEnabled(false);
                            btnStopCollectData.setEnabled(true);
                        }
                        else
                        {
                            showAlertMessage("Error: Sensor DataType.TYPE_DISTANCE_DELTA | DataSource.TYPE_DERIVED not found");
                        }
                    }
                });

            }
        });

        btnStopCollectData = (Button) findViewById(R.id.btnStopCollectData);
        btnStopCollectData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Fitness.SensorsApi.remove(googleApiClient,onDataPointListener);
                btnStartCollectData.setEnabled(true);
                btnStopCollectData.setEnabled(false);


            }
        });


    }
    public void indoorRegisterFitnessDataListener()
    {
        onDataPointListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint)
            {
                for (Field field : dataPoint.getDataType().getFields())
                {
                    Value value = dataPoint.getValue(field);
                    distance = distance+(value.asFloat()/1000);
                    txtCollectData.setText(String.format("%.2f", distance)+" KM");
                }
            }
        };

        Fitness.SensorsApi.add(googleApiClient, new SensorRequest.Builder().setDataSource(currentDataSource).setDataType(DataType.TYPE_DISTANCE_DELTA).setSamplingRate(2, TimeUnit.SECONDS).build(), onDataPointListener).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status)
            {
                if (!status.isSuccess())
                {
                    showAlertMessage("Error: "+status.getStatusMessage());
                }
            }
        });
    }
    public void requestGoogleConnect()
    {
        showLoading();

        //REQUEST PERMISSION
        Nammu.init(getApplicationContext());
        if (Nammu.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION))
        {
            googleConnect();
        }
        else
        {
            if (Nammu.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {

            }
            else
            {
                Nammu.askForPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION, new PermissionCallback() {
                    @Override
                    public void permissionGranted() {

                        googleConnect();
                    }

                    @Override
                    public void permissionRefused() {

                    }
                });
            }
        }
    }
    private void googleConnect()
    {
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Fitness.SENSORS_API).addScope(new Scope(Scopes.FITNESS_LOCATION_READ)).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        googleApiClient.connect();
    }

    public void showLoading() {

        txtLoading.setVisibility(View.VISIBLE);

    }

    public void hideLoading() {
        txtLoading.setVisibility(View.GONE);
    }
    public void showAlertMessage(String message)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Google Fit Indoor");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        hideLoading();
        showAlertMessage("Google Service Connected");
        btnStartCollectData.setEnabled(true);
        btnStopCollectData.setEnabled(false);

    }
    @Override
    public void onConnectionSuspended(int i)
    {
        showAlertMessage("Google Service Fail");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        if (!authInProgress)
        {
            try
            {
                authInProgress = true;
                connectionResult.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
            }
            catch (IntentSender.SendIntentException e)
            {

            }
        }
        else
        {
            Log.v("Google Fit Indoor", "authInProgress");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH)
        {
            authInProgress = false;
            if (resultCode == RESULT_OK)
            {
                if (!googleApiClient.isConnecting() && !googleApiClient.isConnected())
                {
                    googleApiClient.connect();
                }
                else
                {
                    hideLoading();
                }
            }
            else if (resultCode == RESULT_CANCELED)
            {
                hideLoading();
                showAlertMessage("Google Service Canceled");
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Nammu.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

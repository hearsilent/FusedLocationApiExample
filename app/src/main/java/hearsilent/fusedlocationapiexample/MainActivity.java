package hearsilent.fusedlocationapiexample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
		LocationListener {

	private TextView mLngTextView, mLatTextView, mAltTextView, mAddressTextView;

	private Location mLastLocation;
	private GoogleApiClient mGoogleApiClient;

	LocationRequest mLocationRequest;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!hasGooglePlayServices()) {
			Toast.makeText(this, "Google play services not found.", Toast.LENGTH_LONG).show();
			return;
		}

		findViews();
		setUpViews();
		checkPermission();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGoogleApiClient != null) {
			mGoogleApiClient.disconnect();
		}
	}

	private void findViews() {
		mLngTextView = (TextView) findViewById(R.id.longitude);
		mLatTextView = (TextView) findViewById(R.id.latitude);
		mAltTextView = (TextView) findViewById(R.id.altitude);
		mAddressTextView = (TextView) findViewById(R.id.address);
	}

	private void setUpViews() {
		mLatTextView.setText(getString(R.string.latitude_label, ""));
		mLngTextView.setText(getString(R.string.longitude_label, ""));
		mAltTextView.setText(getString(R.string.altitude_label, ""));
		mAddressTextView.setText(getString(R.string.address_label, ""));
	}

	private void checkPermission() {
		if (isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
			setUpGoogleApiClient();
		} else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
		}
	}

	synchronized void setUpGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
		mGoogleApiClient.connect();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
	                                       @NonNull int[] grantResults) {
		switch (requestCode) {
			case 100: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					setUpGoogleApiClient();
				} else {
					Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		}
	}

	@SuppressWarnings("MissingPermission")
	@Override
	public void onConnected(@Nullable Bundle bundle) {
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(1000);
		mLocationRequest.setFastestInterval(500);

		if (!isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
			return;
		}

		if (!checkGPSisOpen()) {
			Toast.makeText(this, "Enable location services for accurate data.", Toast.LENGTH_SHORT)
					.show();
			Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivity(viewIntent);
		} else {
			LocationServices.FusedLocationApi
					.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

			getLocation(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
		}
	}

	private void getLocation(Location location) {
		if (location != null) {
			mLastLocation = location;

			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			double altitude = location.getAltitude();

			mLatTextView.setText(getString(R.string.latitude_label, latitude));
			mLngTextView.setText(getString(R.string.longitude_label, longitude));
			mAltTextView.setText(getString(R.string.altitude_label, altitude));
			mAddressTextView
					.setText(getString(R.string.address_label, getAddressByLocation(location)));
		}
	}

	private String getAddressByLocation(Location location) {
		String returnAddress = "";
		try {
			if (location != null) {
				double longitude = location.getLongitude();
				double latitude = location.getLatitude();

				Geocoder geocoder = new Geocoder(this, Locale.getDefault());
				List<Address> lstAddress = geocoder.getFromLocation(latitude, longitude, 1);

				returnAddress = lstAddress.get(0).getAddressLine(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnAddress;
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onLocationChanged(Location location) {
		if (mLastLocation == null || location.getAccuracy() > mLastLocation.getAccuracy()) {
			getLocation(location);
		}
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		setUpGoogleApiClient();
	}

	private boolean hasGooglePlayServices() {
		return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) ==
				ConnectionResult.SUCCESS;
	}

	private boolean checkGPSisOpen() {
		LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
				manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	public static boolean isPermissionGranted(Context context, String permission) {
		return ContextCompat.checkSelfPermission(context, permission) ==
				PackageManager.PERMISSION_GRANTED;
	}

}
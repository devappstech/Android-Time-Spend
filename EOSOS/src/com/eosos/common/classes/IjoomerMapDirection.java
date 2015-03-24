package com.eosos.common.classes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import pl.mg6.android.maps.extensions.Circle;
import pl.mg6.android.maps.extensions.GoogleMap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.eosos.components.main.EososMasterActivity;
import com.eosos.customviews.IjoomerTextView;
import com.eosos.src.R;
import com.eosos.weservice.WebCallListener;

public class IjoomerMapDirection extends EososMasterActivity implements GoogleMap.OnMapClickListener, GoogleMap.OnMyLocationChangeListener {
	private ImageView imgDirection;
	private String IN_DESTINATION_LAT;
	private String IN_DESTINATION_LONG;
	private String IN_TRAVEL_MODE = "driving";
	private int addressPointCurrentPosition = 0;
	private Circle circle;
	private Address end;
	private GoogleMap googleMap;
	private boolean isDirectionSet;
	ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
	private LinearLayout lnrDirection;
	private LinearLayout lnrLeftArrow;
	private LinearLayout lnrRightArrow;
	private Location myLocation;
	private JSONParser parser;
	private ProgressBar pbrGetRoutePath;
	private Address start;
	private String IN_FROM_LAT;
	private String IN_FROM_LONG;
	private IjoomerTextView txtCurrentPoint;
	private IjoomerTextView txtCurrentSteps;
	private IjoomerTextView txtDuration, txtDistance;

	private void setDirection() {
		if (IN_DESTINATION_LONG.length() > 0 || IN_DESTINATION_LAT.length() > 0) {
			list.clear();
			isDirectionSet = true;
			googleMap.clear();

			pbrGetRoutePath.setVisibility(View.VISIBLE);

			parser.getJSONFromUrl(
					makeURL(Double.parseDouble(IN_FROM_LAT), Double.parseDouble(IN_FROM_LONG), Double.parseDouble(IN_DESTINATION_LAT), Double.parseDouble(IN_DESTINATION_LONG),
							IN_TRAVEL_MODE), new WebCallListener() {

						@Override
						public void onProgressUpdate(int progressCount) {

						}

						@Override
						public void onCallComplete(int responseCode, String errorMessage, ArrayList<HashMap<String, String>> data1, Object data2) {
							try {
								try {
									txtDistance.setText(new JSONObject(data2.toString()).getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0)
											.getJSONObject("distance").getString("text"));
									txtDuration.setText(new JSONObject(data2.toString()).getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0)
											.getJSONObject("duration").getString("text"));
								} catch (Exception e) {
									e.printStackTrace();
								}
								JSONArray step = new JSONObject(data2.toString()).getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0)
										.getJSONArray("steps");

								for (int i = 0; i < step.length(); i++) {
									HashMap<String, Object> row = new HashMap<String, Object>();
									row.put("address", step.getJSONObject(i).getString("html_instructions"));
									row.put("start",
											new LatLng(Double.parseDouble(step.getJSONObject(i).getJSONObject("start_location").getString("lat")), Double.parseDouble(step
													.getJSONObject(i).getJSONObject("start_location").getString("lng"))));
									row.put("end",
											new LatLng(Double.parseDouble(step.getJSONObject(i).getJSONObject("start_location").getString("lat")), Double.parseDouble(step
													.getJSONObject(i).getJSONObject("start_location").getString("lng"))));
									list.add(row);
								}

								PolylineOptions polylineOptions;
								polylineOptions = new PolylineOptions().width(5).color(getResources().getColor(R.color.darkgray)).geodesic(true);
								polylineOptions.add(new LatLng(Double.parseDouble(IN_FROM_LAT), Double.parseDouble(IN_FROM_LONG)));
								for (HashMap<String, Object> row : list) {
									polylineOptions.add((LatLng) row.get("start"));
									polylineOptions.add((LatLng) row.get("end"));
									CircleOptions circleOption = new CircleOptions().center((LatLng) row.get("start")).radius(5).strokeWidth(5)
											.strokeColor(getResources().getColor(R.color.green));
									row.put("circle", googleMap.addCircle(circleOption));
								}
								polylineOptions.add(new LatLng(Double.parseDouble(IN_DESTINATION_LAT), Double.parseDouble(IN_DESTINATION_LONG)));
								googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ijoomer_map_pin)).position(
										new LatLng(Double.parseDouble(IN_DESTINATION_LAT), Double.parseDouble(IN_DESTINATION_LONG))));

								CircleOptions circleOption = new CircleOptions().center((LatLng) ((HashMap<String, Object>) list.get(0)).get("start")).radius(5).strokeWidth(5.0F)
										.strokeColor(getResources().getColor(R.color.blue));
								circle = googleMap.addCircle(circleOption);
								CameraPosition camera = new CameraPosition.Builder()
										.target(new LatLng(((LatLng) ((HashMap<String, Object>) list.get(0)).get("start")).latitude, ((LatLng) ((HashMap<String, Object>) list
												.get(0)).get("start")).longitude)).tilt(50).zoom(17).build();
								googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera));
								((Circle) ((HashMap<String, Object>) list.get(0)).get("circle")).setVisible(false);

								googleMap.addPolyline(polylineOptions);
								addressPointCurrentPosition = 0;

								txtCurrentPoint.setText(Html.fromHtml(((HashMap<String, Object>) list.get(0)).get("address").toString()));
								pbrGetRoutePath.setVisibility(View.GONE);
								lnrDirection.setVisibility(View.VISIBLE);
								txtCurrentSteps.setText(String.format(String.valueOf(1 + addressPointCurrentPosition) + getString(R.string.steps_of), list.size()));
								isDirectionSet = false;
							} catch (Throwable e) {
								pbrGetRoutePath.setVisibility(View.GONE);
								ting("Route Not Getting...");
								e.printStackTrace();
							}
						}
					});

		} else {
			ting("Destination Address Not Set");
		}
	}

	public void initComponents() {
		googleMap = getMapView();
		txtCurrentPoint = (IjoomerTextView) findViewById(R.id.txtCurrentPoint);
		txtCurrentSteps = (IjoomerTextView) findViewById(R.id.txtCurrentSteps);
		lnrLeftArrow = (LinearLayout) findViewById(R.id.lnrLeftArrow);
		lnrRightArrow = (LinearLayout) findViewById(R.id.lnrRightArrow);
		lnrDirection = (LinearLayout) findViewById(R.id.lnrDirection);
		txtDuration = (IjoomerTextView) findViewById(R.id.txtDuration);
		txtDistance = (IjoomerTextView) findViewById(R.id.txtDistance);
		pbrGetRoutePath = (ProgressBar) findViewById(R.id.pbrGetRoutePath);
		// googleMap.setMyLocationEnabled(true);
		imgDirection = (ImageView) getHeaderView().findViewById(R.id.imgDirection);
		imgDirection.setVisibility(View.VISIBLE);
		googleMap.setOnMapClickListener(this);
		googleMap.setOnMyLocationChangeListener(this);
		parser = new JSONParser();
	}

	public String makeURL(double startLatitude, double startLongitude, double endLatitude, double endLongitude, String travelMode) {
		StringBuilder localStringBuilder = new StringBuilder();
		localStringBuilder.append("http://maps.googleapis.com/maps/api/directions/json");
		localStringBuilder.append("?origin=");
		localStringBuilder.append(Double.toString(startLatitude));
		localStringBuilder.append(",");
		localStringBuilder.append(Double.toString(startLongitude));
		localStringBuilder.append("&destination=");
		localStringBuilder.append(Double.toString(endLatitude));
		localStringBuilder.append(",");
		localStringBuilder.append(Double.toString(endLongitude));
		localStringBuilder.append("&sensor=false&mode=");
		localStringBuilder.append(travelMode);
		localStringBuilder.append("&alternatives=true");
		if (travelMode.equalsIgnoreCase("transit")) {
			localStringBuilder.append("&departure_time=");
			Long tsLong = System.currentTimeMillis() / 1000;
			String ts = tsLong.toString();
			localStringBuilder.append(ts);

		}
		Log.e("MapURl", localStringBuilder.toString());
		return localStringBuilder.toString();
	}

	public void onCheckedChanged(RadioGroup paramRadioGroup, int paramInt) {
	}

	public void onMapClick(LatLng paramLatLng) {
	}

	public void onMyLocationChange(Location location) {
		myLocation = location;
		if (!isDirectionSet && list.size() == 0)
			setDirection();
	}

	public void prepareViews() {
		getIntentData();

		setDirection();
		googleMap.setMyLocationEnabled(true);
		googleMap.getUiSettings().setMyLocationButtonEnabled(false);
	}

	public void getIntentData() {
		IN_DESTINATION_LAT = getIntent().getStringExtra("IN_DESTINATION_LAT") != null ? getIntent().getStringExtra("IN_DESTINATION_LAT") : "";
		IN_DESTINATION_LONG = getIntent().getStringExtra("IN_DESTINATION_LONG") != null ? getIntent().getStringExtra("IN_DESTINATION_LONG") : "";
		IN_FROM_LAT = getIntent().getStringExtra("IN_FROM_LAT") != null ? getIntent().getStringExtra("IN_FROM_LAT") : "";
		IN_FROM_LONG = getIntent().getStringExtra("IN_FROM_LONG") != null ? getIntent().getStringExtra("IN_FROM_LONG") : "";
		IN_TRAVEL_MODE = getIntent().getStringExtra("IN_TRAVEL_MODE") != null ? getIntent().getStringExtra("IN_TRAVEL_MODE") : "driving";
	}

	private void getMyLocation() {
		try {
			LatLng latLng = new LatLng(Double.parseDouble(getLatitude()), Double.parseDouble(getLongitude()));
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
			googleMap.animateCamera(cameraUpdate);
			// googleMap.addMarker(new
			// MarkerOptions().icon(bitmapDescriptorActive).position(new
			// LatLng(Double.parseDouble(getLatitude()),
			// Double.parseDouble(getLongitude()))));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setActionListeners() {
		imgDirection.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getMyLocation();

			}
		});
		txtCurrentPoint.setMovementMethod(new ScrollingMovementMethod());
		lnrLeftArrow.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (addressPointCurrentPosition != 0) {
					if (circle != null) {
						circle.remove();
					}
					addressPointCurrentPosition = (-1 + addressPointCurrentPosition);
					((Circle) list.get(addressPointCurrentPosition).get("circle")).setVisible(false);
					((Circle) list.get(1 + addressPointCurrentPosition).get("circle")).setVisible(true);

					CircleOptions circleOption = new CircleOptions().center((LatLng) list.get(addressPointCurrentPosition).get("start")).radius(5).strokeWidth(5)
							.strokeColor(getResources().getColor(R.color.txt_header));
					circle = googleMap.addCircle(circleOption);

					CameraPosition camera = new CameraPosition.Builder()
							.target(new LatLng(((LatLng) list.get(addressPointCurrentPosition).get("start")).latitude,
									((LatLng) list.get(addressPointCurrentPosition).get("start")).longitude)).tilt(50).zoom(17).build();

					googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera));

					txtCurrentSteps.setText(String.valueOf(1 + addressPointCurrentPosition) + " " + String.format(getString(R.string.steps_of), list.size()));
					txtCurrentPoint.setText(Html.fromHtml((String) list.get(addressPointCurrentPosition).get("address")));
				}
			}
		});
		lnrRightArrow.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (addressPointCurrentPosition < -1 + list.size()) {
					if (circle != null) {
						circle.remove();
					}

					addressPointCurrentPosition = (1 + addressPointCurrentPosition);

					((Circle) list.get(addressPointCurrentPosition).get("circle")).setVisible(false);
					((Circle) list.get(-1 + addressPointCurrentPosition).get("circle")).setVisible(true);

					CircleOptions circleOption = new CircleOptions().center((LatLng) list.get(addressPointCurrentPosition).get("start")).radius(5).strokeWidth(5)
							.strokeColor(getResources().getColor(R.color.txt_header));
					circle = googleMap.addCircle(circleOption);
					CameraPosition camera = new CameraPosition.Builder()
							.target(new LatLng(((LatLng) list.get(addressPointCurrentPosition).get("start")).latitude,
									((LatLng) list.get(addressPointCurrentPosition).get("start")).longitude)).tilt(50).zoom(17).build();
					googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera));

					txtCurrentSteps.setText(String.valueOf(1 + addressPointCurrentPosition) + " " + String.format(getString(R.string.steps_of), list.size()));
					txtCurrentPoint.setText(Html.fromHtml((String) list.get(addressPointCurrentPosition).get("address")));
				}
			}
		});
	}

	public int setFooterLayoutId() {
		return 0;
	}

	public int setLayoutId() {
		return R.layout.ijoomer_map_direction;
	}

	public View setLayoutView() {
		return null;
	}

	public int setTabBarDividerResId() {
		return 0;
	}

	public int setTabItemLayoutId() {
		return 0;
	}

	public String[] setTabItemNames() {
		return null;
	}

	public int[] setTabItemOffDrawables() {
		return null;
	}

	public int[] setTabItemOnDrawables() {
		return null;
	}

	public int[] setTabItemPressDrawables() {
		return null;
	}

	public class JSONParser {
		InputStream is = null;
		JSONObject jObj = null;
		String json = "";

		public JSONParser() {
		}

		public void getJSONFromUrl(final String url, final WebCallListener target) {
			new AsyncTask<Void, Void, String>() {
				protected String doInBackground(Void... params) {
					HttpURLConnection httpURLConnection = null;
					StringBuilder stringBuilder = new StringBuilder();
					try {
						httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
						InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());

						int read;
						char[] buff = new char[1024];
						while ((read = inputStreamReader.read(buff)) != -1) {
							stringBuilder.append(buff, 0, read);
						}
						return stringBuilder.toString();
					} catch (MalformedURLException localMalformedURLException) {
						return "";
					} catch (IOException localIOException) {
						return "";
					} finally {
						if (httpURLConnection != null)
							httpURLConnection.disconnect();
					}
				}

				protected void onPostExecute(String result) {
					super.onPostExecute(result);
					target.onCallComplete(200, null, null, result);
				}
			}.execute();
		}
	}
}
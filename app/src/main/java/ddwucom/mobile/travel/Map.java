package ddwucom.mobile.travel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Map extends AppCompatActivity {

    String [] permission_list ={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private Marker currentMarker = null;
    LocationManager locationManager;
    GoogleMap map;
    private RecyclerView listview;
    private ArrayList<MyCourse> courseList = null;
    private CourseListAdapter courseListAdapter;
    LatLng position;
    double latitude = 100.0;
    double longitude = 200.0;
    String placeName = "현재 위치";
    String address;
    Location location1;
    Location location2;
    int i = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission_list, 0);
        } else {
            init();
        }

        listview = findViewById(R.id.y_course_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        listview.setLayoutManager(layoutManager);
        courseList = new ArrayList();

        String apiKey = getResources().getString(R.string.google_maps_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
    }

    private void courseAdd() {
        courseList.add(new MyCourse(i, placeName));
        i++;
        courseListAdapter = new CourseListAdapter(this, courseList, onClickItem);
        listview.setAdapter(courseListAdapter);
    }

    private View.OnClickListener onClickItem = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String str = (String) v.getTag();
            Toast.makeText(Map.this, str, Toast.LENGTH_SHORT).show();
        }//수정해야됨
    };

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.y_placeSearch:
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fieldList).build(Map.this);
                startActivityForResult(intent, 100);
                break;
            case R.id.y_courseAdd:
                courseAdd();
                break;
            case R.id.y_courseRegister:
                Intent planIntent = new Intent(Map.this, PlanLastStep.class);
                startActivity(planIntent);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            longitude = place.getLatLng().longitude;
            latitude = place.getLatLng().latitude;
            placeName = place.getName();
            address = place.getAddress();
            getMyLocation();
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(Map.this, status.getStatusMessage(), Toast.LENGTH_LONG).show();
        }
        else if (requestCode == 200 && resultCode == RESULT_OK) {
            courseAdd();
        }
    }

    public void findPlace(double lat, double lon) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
                Place.Field.LAT_LNG);

        // Use the builder to create a FindCurrentPlaceRequest.
        FindCurrentPlaceRequest request =
                FindCurrentPlaceRequest.newInstance(placeFields);

        // Get the likely places - that is, the businesses and other points of interest that
        // are the best match for the device's current location.
        PlacesClient mPlacesClient = Places.createClient(this);;
        @SuppressWarnings("MissingPermission") final Task<FindCurrentPlaceResponse> placeResult =
                mPlacesClient.findCurrentPlace(request);
        placeResult.addOnCompleteListener (new OnCompleteListener<FindCurrentPlaceResponse>() {
            @Override
            public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    FindCurrentPlaceResponse likelyPlaces = task.getResult();

                    // Set the count, handling cases where less than 5 entries are returned.
                    int count;
                    if (likelyPlaces.getPlaceLikelihoods().size() < 20) {
                        count = likelyPlaces.getPlaceLikelihoods().size();
                    } else {
                        count = 20;
                    }

                    int i = 0;
                    String[] mLikelyPlaceNames = new String[count];
                    String[] mLikelyPlaceAddresses = new String[count];

                    for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
                        // Build a list of likely places to show the user.
                        mLikelyPlaceNames[i] = placeLikelihood.getPlace().getName();
                        mLikelyPlaceAddresses[i] = placeLikelihood.getPlace().getAddress();

                        i++;
                        if (i > (count - 1)) {
                            break;
                        }
                    }

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    address = mLikelyPlaceAddresses[0];
                    placeName = mLikelyPlaceNames[0];
                }
                else {
                    Log.e("yyj", "Exception: %s", task.getException());
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }
        init();
    }

    public void init() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment)fragmentManager.findFragmentById(R.id.map);

        MapReadyCallback callback1 = new MapReadyCallback();
        mapFragment.getMapAsync(callback1);
    }

    //Google 지도 사용 준비 완료시 동작하는 콜백
    class MapReadyCallback implements OnMapReadyCallback {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;
            getMyLocation();
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    //findPlace(latLng.latitude, latLng.longitude);
                    setMarker(latLng);
                }
            });
        }
    }

    //현재위치 측정 메소드
    public void getMyLocation() {
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        //권한 확인 작업(일부 코드가 권한 작업을 해야 오류 안남)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }

        //이전 측정 값
        location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        location2 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        //이전 측정 값이 있다면
        if(location1 != null) {
            setMyLocation(location1);Log.d("yj","loca1");
        }
        else {
            if(location2 != null) {
                setMyLocation(location2);Log.d("yj","loca2");
            }
        }
        //새로운 측정 값
        GetMyLocationListener listener = new GetMyLocationListener();

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, (android.location.LocationListener) listener);
        }
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10f, (android.location.LocationListener) listener);
        }
    }

    public void setMyLocation(Location location) {
        //위도와 경도값을 관리하는 객체
        if (latitude == 100.0 && longitude == 200.0) {
            position = new LatLng(location.getLatitude(), location.getLongitude());
        }
       else {
            position = new LatLng(latitude, longitude);
        }

        setMarker(position);

        //권한 확인 작업
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }
        //현재 위치 표시
        map.setMyLocationEnabled(true);
    }

    public void setMarker(LatLng position) {
        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(position);
        markerOptions.title(placeName);
        markerOptions.snippet(address);
        markerOptions.draggable(true);
        currentMarker = map.addMarker(markerOptions);

        MarkerWindowAdapter customInfoWindow = new MarkerWindowAdapter(Map.this);
        map.setInfoWindowAdapter(customInfoWindow);
        currentMarker.showInfoWindow();//되는지 확인
        Log.d("yj", "윈도우");

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
//                Intent intent = new Intent(Map.this ,ReviewList.class);
//                startActivity(intent);
                Intent intent = new Intent(Map.this, ReviewList.class);
                startActivityForResult(intent, 200);
            }
        });

        CameraUpdate update1 = CameraUpdateFactory.newLatLngZoom(position, 15);
        map.moveCamera(update1);
    }

    //현재 위치 측정이 성공하면 반응하는 리스너
    class GetMyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            setMyLocation(location);
            locationManager.removeUpdates((android.location.LocationListener) this);//위치 측정 중단
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //상태 변경 시 호출
        }

        @Override
        public void onProviderEnabled(String provider) {
            //위치 측정하기 위한 요소가 사용 불가능시 호출
        }

        @Override
        public void onProviderDisabled(String provider) {
            //위치 측정하기 위한 요소가 사용 가능시 호출
        }
    }
}

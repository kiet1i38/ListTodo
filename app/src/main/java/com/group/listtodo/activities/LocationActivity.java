package com.group.listtodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.group.listtodo.R;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Mặc định TDTU
        LatLng tdtu = new LatLng(10.7324, 106.6992);
        googleMap.addMarker(new MarkerOptions().position(tdtu).title("Tôn Đức Thắng University"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tdtu, 15));

        // Sự kiện click bản đồ -> Chọn vị trí và trả về
        googleMap.setOnMapClickListener(latLng -> {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng).title("Vị trí đã chọn"));

            // Trả kết quả về (Mockup tên địa điểm, thực tế cần Geocoder để lấy tên đường)
            String locationName = "Vị trí: " + String.format("%.4f, %.4f", latLng.latitude, latLng.longitude);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("location_name", locationName);
            setResult(RESULT_OK, resultIntent);

            Toast.makeText(this, "Đã chọn: " + locationName, Toast.LENGTH_SHORT).show();
            finish(); // Đóng bản đồ
        });
    }
}
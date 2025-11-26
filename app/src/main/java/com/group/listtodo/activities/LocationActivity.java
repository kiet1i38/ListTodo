package com.group.listtodo.activities;

import android.os.Bundle;
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
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Mặc định map vào Tôn Đức Thắng Uni
        LatLng tdtu = new LatLng(10.7324, 106.6992);
        googleMap.addMarker(new MarkerOptions().position(tdtu).title("Marker at TDTU"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tdtu, 15));

        // Sự kiện click bản đồ để chọn vị trí
        googleMap.setOnMapClickListener(latLng -> {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng).title("Vị trí đã chọn"));
            // Có thể trả về LatLng cho màn hình AddTask
        });
    }
}
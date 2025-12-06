package com.group.listtodo.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.group.listtodo.R;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedAddressName = "";
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        edtSearch = findViewById(R.id.edt_search_location);
        ImageView btnSearch = findViewById(R.id.btn_search_map);
        Button btnConfirm = findViewById(R.id.btn_confirm_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnSearch.setOnClickListener(v -> searchLocation());

        // KHI BẤM XÁC NHẬN -> TRẢ VỀ CẢ TÊN VÀ TỌA ĐỘ
        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                String resultName = selectedAddressName.isEmpty()
                        ? String.format("Lat: %.4f, Lng: %.4f", selectedLatLng.latitude, selectedLatLng.longitude)
                        : selectedAddressName;

                resultIntent.putExtra("location_name", resultName);
                resultIntent.putExtra("lat", selectedLatLng.latitude);   // Trả về vĩ độ
                resultIntent.putExtra("lng", selectedLatLng.longitude); // Trả về kinh độ

                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Vui lòng chọn một địa điểm trên bản đồ!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 1. KIỂM TRA XEM CÓ TỌA ĐỘ CŨ TRUYỀN SANG KHÔNG
        double oldLat = getIntent().getDoubleExtra("old_lat", 0);
        double oldLng = getIntent().getDoubleExtra("old_lng", 0);

        if (oldLat != 0 && oldLng != 0) {
            // Nếu có -> Di chuyển đến vị trí cũ
            LatLng oldPos = new LatLng(oldLat, oldLng);
            String oldName = getIntent().getStringExtra("old_name");
            moveCameraAndMarker(oldPos, oldName != null ? oldName : "Vị trí đã lưu");
        } else {
            // Nếu không -> Mặc định TDTU
            LatLng tdtu = new LatLng(10.7324, 106.6992);
            moveCameraAndMarker(tdtu, "Tôn Đức Thắng University");
        }

        mMap.setOnMapClickListener(latLng -> {
            String address = getAddressFromLatLng(latLng);
            moveCameraAndMarker(latLng, address);
        });
    }

    private void searchLocation() {
        String location = edtSearch.getText().toString();
        if (location.isEmpty()) return;

        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(location, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                moveCameraAndMarker(latLng, location);
            } else {
                Toast.makeText(this, "Không tìm thấy địa điểm!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi kết nối tìm kiếm!", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveCameraAndMarker(LatLng latLng, String title) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng).title(title)).showInfoWindow();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        selectedLatLng = latLng;
        selectedAddressName = title;
    }

    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Vị trí đã chọn";
    }
}
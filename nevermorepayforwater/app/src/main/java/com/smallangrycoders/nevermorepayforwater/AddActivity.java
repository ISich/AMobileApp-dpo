package com.smallangrycoders.nevermorepayforwater;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.time.LocalDateTime;

public class AddActivity extends Activity {
    private Button btSave,btCancel;
    private EditText etLoc,etLat,etLon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_activity);
        btSave=(Button)findViewById(R.id.butSave);
        btCancel=(Button)findViewById(R.id.butCancel);
        etLoc=(EditText)findViewById(R.id.City);
        etLat=(EditText)findViewById(R.id.etLat);
        etLon=(EditText)findViewById(R.id.etLon);

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Проверка на пустые поля
                if (TextUtils.isEmpty(etLoc.getText().toString())) {
                    Toast.makeText(AddActivity.this, getString(R.string.error_empty_location), Toast.LENGTH_SHORT).show();
                    etLoc.requestFocus();
                    return;
                }
                
                if (TextUtils.isEmpty(etLat.getText().toString())) {
                    Toast.makeText(AddActivity.this, getString(R.string.error_empty_latitude), Toast.LENGTH_SHORT).show();
                    etLat.requestFocus();
                    return;
                }
                
                if (TextUtils.isEmpty(etLon.getText().toString())) {
                    Toast.makeText(AddActivity.this, getString(R.string.error_empty_longitude), Toast.LENGTH_SHORT).show();
                    etLon.requestFocus();
                    return;
                }
                
                // Проверка корректности координат
                try {
                    double lat = Double.parseDouble(etLat.getText().toString());
                    if (lat < -90 || lat > 90) {
                        Toast.makeText(AddActivity.this, getString(R.string.error_invalid_latitude), Toast.LENGTH_SHORT).show();
                        etLat.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(AddActivity.this, getString(R.string.error_invalid_latitude_format), Toast.LENGTH_SHORT).show();
                    etLat.requestFocus();
                    return;
                }
                
                try {
                    double lon = Double.parseDouble(etLon.getText().toString());
                    if (lon < -180 || lon > 180) {
                        Toast.makeText(AddActivity.this, getString(R.string.error_invalid_longitude), Toast.LENGTH_SHORT).show();
                        etLon.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(AddActivity.this, getString(R.string.error_invalid_longitude_format), Toast.LENGTH_SHORT).show();
                    etLon.requestFocus();
                    return;
                }
                
                // Если все проверки пройдены, создаем объект и возвращаем результат
                StCity stcity=new StCity(-1,etLoc.getText().toString(),"0", etLat.getText().toString(), etLon.getText().toString(), 1, LocalDateTime.now());
                Intent intent=getIntent();
                intent.putExtra("StCity", stcity);
                setResult(RESULT_OK,intent);
                finish();
            }
        });

        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

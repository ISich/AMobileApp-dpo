package com.smallangrycoders.nevermorepayforwater;

import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    DBCities stcConnector;
    Context oContext;
    ArrayList<StCity> states = new ArrayList<StCity>();
    StCityAdapter adapter;
    int ADD_ACTIVITY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.list);
        oContext=this;
        stcConnector=new DBCities(this);
        
        // Получаем список городов
        ArrayList<StCity> cities = stcConnector.selectAll();
        
        // Проверяем, пуст ли список при запуске
        if (cities.isEmpty()) {
            Toast.makeText(oContext, oContext.getString(R.string.empty_cities_list), Toast.LENGTH_LONG).show();
        }
        
        adapter = new StCityAdapter(this, cities, null, oContext);
        StCityAdapter.OnStCityClickListener stateClickListener = (state, position) -> {
           sendPOST(state, adapter);
           state.setSyncDate(LocalDateTime.now());
        };
        
       adapter.SetOnCl(stateClickListener);
       recyclerView.setAdapter(adapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    private void updateList () {
        ArrayList<StCity> cities = stcConnector.selectAll();
        adapter.setArrayMyData(cities);
        adapter.notifyDataSetChanged();
        
        // Показываем сообщение, если список городов пуст
        if (cities.isEmpty()) {
            Toast.makeText(oContext, oContext.getString(R.string.empty_cities_list), Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                Intent i = new Intent(oContext, AddActivity.class);
                startActivityForResult (i, ADD_ACTIVITY);
                return true;
            case R.id.deleteAll:
                // Проверяем, есть ли что удалять
                ArrayList<StCity> cities = stcConnector.selectAll();
                if (cities.isEmpty()) {
                    Toast.makeText(oContext, getString(R.string.list_already_empty), Toast.LENGTH_SHORT).show();
                    return true;
                }
                
                // Показываем диалог подтверждения
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.delete_confirmation_title))
                       .setMessage(getString(R.string.delete_confirmation_message))
                       .setPositiveButton(getString(R.string.delete_yes), (dialog, which) -> {
                           stcConnector.deleteAll();
                           updateList();
                           Toast.makeText(oContext, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                       })
                       .setNegativeButton(getString(R.string.delete_cancel), (dialog, which) -> {
                           dialog.dismiss();
                       })
                       .show();
                return true;
            case R.id.exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            StCity st = (StCity) data.getExtras().getSerializable("StCity");
            stcConnector.insert(st.getName(), st.getTemp(), st.getStrLat(), st.getStrLon(), st.getFlagResource(), st.getSyncDate());
            updateList();

        }
    }
    public void sendPOST(StCity state, StCityAdapter adapter) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
                
        String foreAddr = oContext.getString(R.string.forecast_addr);
        
        // Проверка корректности координат перед отправкой запроса
        try {
            double lat = Double.parseDouble(state.getStrLat());
            double lon = Double.parseDouble(state.getStrLon());
            
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                MainActivity.this.runOnUiThread(() -> {
                    state.setTemp(oContext.getString(R.string.error_invalid_latitude) + " или " + 
                                 oContext.getString(R.string.error_invalid_longitude));
                    adapter.notifyDataSetChanged();
                    stcConnector.update(state);
                    Toast.makeText(oContext, oContext.getString(R.string.error_invalid_latitude) + " или " + 
                                 oContext.getString(R.string.error_invalid_longitude), Toast.LENGTH_SHORT).show();
                });
                return;
            }
        } catch (NumberFormatException e) {
            MainActivity.this.runOnUiThread(() -> {
                state.setTemp(oContext.getString(R.string.err_text));
                adapter.notifyDataSetChanged();
                stcConnector.update(state);
                Toast.makeText(oContext, oContext.getString(R.string.err_text), Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse(foreAddr+oContext.getString(R.string.lat_condition)+state.getStrLat()+oContext.getString(R.string.lon_condition)+state.getStrLon()+oContext.getString(R.string.add_condition)).newBuilder();
        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(url)
                .cacheControl(new CacheControl.Builder().maxStale(3, TimeUnit.SECONDS).build())
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    final int statusCode = response.code();
                    MainActivity.this.runOnUiThread(() -> {
                        String errorMessage;
                        if (statusCode >= 400 && statusCode < 500) {
                            errorMessage = oContext.getString(R.string.err_text) + " (Код: " + statusCode + ")";
                        } else if (statusCode >= 500) {
                            errorMessage = oContext.getString(R.string.error_api) + " (Код: " + statusCode + ")";
                        } else {
                            errorMessage = oContext.getString(R.string.error_unknown) + " (Код: " + statusCode + ")";
                        }
                        
                        state.setTemp(errorMessage);
                        adapter.notifyDataSetChanged();
                        stcConnector.update(state);
                        Toast.makeText(oContext, errorMessage, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    final String responseData = response.body().string();
                    JSONObject jo;
                    try {
                        jo = new JSONObject(responseData);
                        
                        // Проверка наличия необходимых полей в ответе
                        if (!jo.has(oContext.getString(R.string.cur_weather))) {
                            throw new JSONException("Отсутствует поле current_weather в ответе");
                        }
                        
                        JSONObject currentWeather = jo.getJSONObject(oContext.getString(R.string.cur_weather));
                        if (!currentWeather.has(oContext.getString(R.string.temperature))) {
                            throw new JSONException("Отсутствует поле temperature в ответе");
                        }
                        
                        String tempFromAPI = currentWeather.get(oContext.getString(R.string.temperature)).toString();
                        
                        MainActivity.this.runOnUiThread(() -> {
                            state.setTemp(tempFromAPI);
                            adapter.notifyDataSetChanged();
                            stcConnector.update(state);
                        });
                    } catch (JSONException e) {
                        final String errorMessage = e.getMessage();
                        MainActivity.this.runOnUiThread(() -> {
                            state.setTemp(oContext.getString(R.string.err_text));
                            adapter.notifyDataSetChanged();
                            stcConnector.update(state);
                            Toast.makeText(oContext, oContext.getString(R.string.err_text) + ": " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                        e.printStackTrace();
                    }
                }
            }
            
            @Override
            public void onFailure(Call call, IOException e) {
                final String errorMessage = e.getMessage();
                MainActivity.this.runOnUiThread(() -> {
                    state.setTemp(oContext.getString(R.string.err_connect));
                    adapter.notifyDataSetChanged();
                    stcConnector.update(state);
                    Toast.makeText(oContext, oContext.getString(R.string.err_connect) + ": " + errorMessage, Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        });
    }

}
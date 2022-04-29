package com.example.gallerycracker.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.gallerycracker.R;
import com.example.gallerycracker.adapters.AdapterOwnPhotos;
import com.example.gallerycracker.workers.ImagesWorker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Maps extends FragmentActivity implements OnMapReadyCallback {

    private String username;
    // Variables para la gestión del mapa y los marcadores
    private boolean estadoGooglePlay;
    // Mapa Google Maps
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Acceder a las preferencias para conseguir el valor de la clave del idioma
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma", "es");

        // Cambiar el idioma
        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());

        setContentView(R.layout.maps_activity);

        username = getIntent().getExtras().getString("username");

        // Comprobar el estado de GooglePlayServices
        estadoGooglePlay = comprobarGooglePlayServices();
        if(estadoGooglePlay) {
            // Cargar el fragment que contiene el mapa de GoogleMaps
            SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            fragment.getMapAsync(this);
        }
    }

    // Comprobar el estado de Google Play Services del dispositivo
    private boolean comprobarGooglePlayServices(){
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            return true;
        }
        else {
            if (api.isUserResolvableError(code)){
                api.getErrorDialog(this, code, 58).show();
            }
            return false;
        }
    }

    // Método que devuelve el objeto 'GoogleMap' para gestionar el mapa
    @Override
    public void onMapReady(GoogleMap googleMap) {

        // Se inicializará el mapa (en modo NORMAL)
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map = googleMap;

        // Hacer una llamada al backend para conseguir las coordenadas de todas las imágenes subidas por el usuario
        // Información a enviar
        Data datos = new Data.Builder()
                .putString("function", "getLocations")
                .putString("username", username)
                .build();

        // Restricciones, conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // ejecutar solo una vez
        OneTimeWorkRequest one = new OneTimeWorkRequest.Builder(ImagesWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperar los datos
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(one.getId())
                .observe(this, status -> {
                    // En caso de éxito mostrar las fotos en el recyclerview
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);

                            if(jsonArray.length() == 0) {
                                Toast.makeText(this, getString(R.string.noOwnPhotos), Toast.LENGTH_SHORT).show();
                            }
                            else{
                                String[][] info = new String[jsonArray.length()/3][3];
                                // Se tiene un array de [imagen1, latitud1, longitud1, imagen2, lat2, long2, etc]
                                // por lo que vamos a sacar la info de cada foto
                                int i = 0;
                                int vuelta = 0;
                                while(i< jsonArray.length()) {
                                    for(int j=0; j<3; j++) {
                                        info[vuelta][j] = (String) jsonArray.get(i);
                                        i++;
                                    }
                                    vuelta++;
                                }

                                // Añadir los marcadores al GoogleMaps
                                for(int row=0; row < info.length; row++) {
                                    LatLng sydney = new LatLng(Float.parseFloat(info[row][1]), Float.parseFloat(info[row][2]));
                                    map.addMarker(new MarkerOptions()
                                            .position(sydney)
                                            .title(info[row][0]));

                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(one);
    }
}

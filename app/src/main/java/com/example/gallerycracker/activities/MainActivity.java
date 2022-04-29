package com.example.gallerycracker.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.gallerycracker.R;
import com.example.gallerycracker.adapters.AdapterOwnPhotos;
import com.example.gallerycracker.dialogs.DialogoBorrarFoto;
import com.example.gallerycracker.workers.ImagesWorker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements DialogoBorrarFoto.ListenerDialogo {

    private String username;
    private RecyclerView recycler;
    private GridLayoutManager gridLayout;
    private AdapterOwnPhotos adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Acceder a las preferencias para conseguir el valor de la clave del idioma
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma", "es");
        System.out.println(idioma);

        // Cambiar el idioma
        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());

        setContentView(R.layout.main_activity);

        username = getIntent().getExtras().getString("username");
        recycler = (RecyclerView) findViewById(R.id.recycler_profile);

        int orientacion = getResources().getConfiguration().orientation;

        // Definir el layout del recyclerview
        gridLayout = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        recycler.setLayoutManager(gridLayout);

        // Asignar del Toolbar personalizado
        setSupportActionBar(findViewById(R.id.toolbarPrincipal));

        // Obtener las fotos del usuario de la base de datos
        // Información a enviar
        Data datos = new Data.Builder()
                .putString("function", "getPhotos")
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
                                Toast.makeText(this, getString(R.string.cargandoPhotos), Toast.LENGTH_LONG).show();
                                String[] ids = new String[jsonArray.length()];

                                for(int i=0; i<jsonArray.length(); i++) {
                                    ids[i] = (String) jsonArray.get(i);
                                }
                                adaptador = new AdapterOwnPhotos(this,username,ids);
                                recycler.setAdapter(adaptador);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(one);
    }

    public void onDevice(View v) {
        Intent intent = new Intent(this, UploadImage.class);
        intent.putExtra("username", username);
        intent.putExtra("whattodo", "storage");
        startActivity(intent);
    }

    public void onCamera(View v) {
        Intent intent = new Intent(this, UploadImage.class);
        intent.putExtra("username", username);
        intent.putExtra("whattodo", "camera");
        startActivity(intent);
    }

    // Método para asignar menu.xml con la definición del menú al Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Método para gestionar la interacción del usuario ante cualquier elección del menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // Acceder a la sección del perfil
        if (id == R.id.profile) { // Si se ha pulsado profile, se accederá a la actividad 'Profile'
            Intent intent = new Intent(this, Maps.class);
            intent.putExtra("username", username);
            startActivity(intent);
            //Acceder a la activitad de amigos
        } else if (id == R.id.contacts) { // Si se ha pulsado contacs. acceder a la actividad 'Contacts'
            Intent i = new Intent(this, PeoplePhotos.class);
            i.putExtra("username", username);
            startActivity(i);
        // Acceder a las preferencias de la aplicación
        } else if (id == R.id.preferences) {
            Intent i = new Intent(this, GestionPreferencias.class);
            i.putExtra("username", username);
            startActivity(i);
        // Logout de la aplicación, volver a la actividad del login
        } else if (id == R.id.logout) { // Si se ha pulsado logout, se volverá a la pantalla del login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // Borrar la foto de firebase una vez aceptado el diálogo
    @Override
    public void borrarFoto(String img) {
        // Borramos la imagen
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        StorageReference path = storageReference.child(img);
        path.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(@NonNull Void unused) {
                // Destruimos la actividad actual para que se actualicen los cambios
                Toast.makeText(MainActivity.this, getString(R.string.imagenBorrada), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
                finish();
            }
        });

    }
}
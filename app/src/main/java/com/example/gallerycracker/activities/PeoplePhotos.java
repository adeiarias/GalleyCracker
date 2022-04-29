package com.example.gallerycracker.activities;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.gallerycracker.R;
import com.example.gallerycracker.adapters.AdapterOwnPhotos;
import com.example.gallerycracker.adapters.AdapterPeoplePhotos;
import com.example.gallerycracker.dialogs.DialogoComentario;
import com.example.gallerycracker.dialogs.DialogoDescargarFoto;
import com.example.gallerycracker.workers.ComentariosWorker;
import com.example.gallerycracker.workers.ImagesWorker;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.Locale;

public class PeoplePhotos extends AppCompatActivity implements DialogoDescargarFoto.ListenerDialogo, DialogoComentario.ListenerDialogo {

    private String username;
    private String quien;
    private RecyclerView recycler;
    private EditText edit;
    private GridLayoutManager gridLayout;
    private AdapterPeoplePhotos adaptador;
    private Uri uri_imagen_descargar;

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

        setContentView(R.layout.people_activity);

        username = getIntent().getExtras().getString("username");
        recycler = (RecyclerView) findViewById(R.id.recycler_people);
        edit = (EditText) findViewById(R.id.edit);
        int orientacion = getResources().getConfiguration().orientation;
        if(orientacion == Configuration.ORIENTATION_PORTRAIT) {
            // Si la orientación es vertical
            gridLayout = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        } else {
            // Si la orientación es horizontal
            gridLayout = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        }
        recycler.setLayoutManager(gridLayout);
    }

    public void search(View v) {
        quien = edit.getText().toString();
        // Obtener las fotos del usuario de la base de datos
        // Información a enviar
        displayPhotos(quien);

    }

    private void displayPhotos(String quien) {
        Data datos = new Data.Builder()
                .putString("function", "getPhotos")
                .putString("username", quien)
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
                        if(!result.isEmpty()) {
                            try {
                                JSONArray jsonArray = new JSONArray(result);

                                if(jsonArray.length() == 0) {
                                    Toast.makeText(this, getString(R.string.noPhotos), Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Toast.makeText(this, getString(R.string.cargandoPhotos), Toast.LENGTH_SHORT).show();
                                    String[] ids = new String[jsonArray.length()];

                                    for(int i=0; i<jsonArray.length(); i++) {
                                        ids[i] = (String) jsonArray.get(i);
                                    }
                                    adaptador = new AdapterPeoplePhotos(this,quien,ids);
                                    recycler.setAdapter(adaptador);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.noPhotos), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(one);
    }

    // Gestionar que no se pierda la información cuando se cambie de orientación en el dispositivo
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("quien", edit.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            // Cogemos el string guardado al destruir la actividad
            String nombre = savedInstanceState.getString("quien");
            if(!nombre.isEmpty()) {
                // Llamamos de nuevo al método que cargará las imágenes de la base de datos
                displayPhotos(nombre);
            }
        }
    }

    // Método para deacargar la foto una vez aceptado el diálogo
    // https://stackoverflow.com/questions/39363661/how-to-store-image-using-downloader-manager-downloaded-image-name-in-external-st
    @Override
    public void descargarFoto(Uri uri) {
        uri_imagen_descargar = uri;
        // Descargar la foto de firebase y guardarla en el dispositivo
        download();
    }

    private void download() {
        // Comprobar los permisos de escritura en el almacenamiento
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Pedir el permiso al usuario
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                // El permiso no está condecido
                // El usuario no quiere que le vuelvan a pedir el permiso de nuevo
            }
            // Pedir permiso al usuario
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            // El permiso ya estaba concedido
            File path = new File(uri_imagen_descargar.toString());
            String filename = path.getName();
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(uri_imagen_descargar);
            request.setTitle(filename);
            request.setDescription(filename);
            request.setVisibleInDownloadsUi(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, filename);
            downloadManager.enqueue(request);
            Toast.makeText(PeoplePhotos.this, getString(R.string.imagenGuardada), Toast.LENGTH_SHORT).show();
        }
    }

    // Método que se ejecuta al permitir o denegar los permisos almacenamiento
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Si el permiso de almacenamiento se ha concedido, descargar la imagen
                    download();
                }
                return;
            }
        }
    }

    @Override
    public void enviarComentario(String comentario, String quien) {
        // Enviar un comentario a una persona sobre su foto
        // Información a enviar
        Data datos = new Data.Builder()
                .putString("funcion", "enviarComentario")
                .putString("emisor", username)
                .putString("receptor", quien)
                .putString("comentario", comentario)
                .build();

        // Restricciones : conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Ejecutar una vez
        OneTimeWorkRequest one = new OneTimeWorkRequest.Builder(ComentariosWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperar la información
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(one.getId())
                .observe(this, status -> {
                    // En caso de que todo haya ido bien, mostrar toast
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(this, getString(R.string.enviado), Toast.LENGTH_SHORT).show();
                    }
                });
        WorkManager.getInstance(this).enqueue(one);
    }
}

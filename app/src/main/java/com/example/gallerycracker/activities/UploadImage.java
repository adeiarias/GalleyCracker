package com.example.gallerycracker.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.gallerycracker.R;
import com.example.gallerycracker.workers.ImagesWorker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.location.LocationRequest;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadImage extends AppCompatActivity {

    private static int CODIGO_GALERIA = 1;
    private static int CODIGO_FOTO_ARCHIVO = 2;

    private String username;
    private String task;
    private String latitud;
    private String longitud;
    private String name;
    private File ImgFichero;
    private Uri imagen;

    private StorageReference storageReference;

    private ImageView imageView;

    // Atributos para la localización
    private FusedLocationProviderClient provider;
    private LocationCallback refresh;

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

        setContentView(R.layout.upload_image);

        username = getIntent().getExtras().getString("username");
        System.out.println("UsernameInicall ====== "+ username);
        System.out.println(username);
        task = getIntent().getExtras().getString("whattodo");
        longitud = "";
        latitud = "";

        imageView = findViewById(R.id.imageOwn);

        // Ver si hay que sacar la foto desde la galeria o cogerla desde el almacenamiento del dispositivo
        if(task.equals("camera")) {
            // Comprobar permisos de la camara
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Si el permiso no se ha pedido, pedirlo
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // Aceptar el permiso
                } else {
                    // El usuario ha decidido que no quiere aceptar el permiso
                    Toast.makeText(this, getString(R.string.noPermisoCamara), Toast.LENGTH_SHORT).show();
                }
                // Pedir permiso al usuario
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
            } else { // El permiso ya estaba concedido
                // Definir donde almacenar las imágenes
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fichero = "imagen-"+timeStamp;
                File directorio = this.getFilesDir();
                // Crear archivo
                try {
                    ImgFichero = File.createTempFile(fichero, ".jpg", directorio);
                    imagen = FileProvider.getUriForFile(this, "com.example.gallerycracker.provider", ImgFichero);
                } catch (Exception e) {
                    System.out.println("Error al crear el fichero");
                }
                // Abrir la cámara
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imagen);
                startActivityForResult(intent, CODIGO_FOTO_ARCHIVO);
            }
        } else if(task.equals("storage")) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, CODIGO_GALERIA);
        }
    }

    // Método engargado de procesar la petición de abrir los intents
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // En caso de coger la imagen desde la galería, ponerla en el imageview
        if(requestCode == CODIGO_GALERIA && resultCode == RESULT_OK) {
            imagen = data.getData();
            name = new File(imagen.getPath()).getName();
            imageView.setImageURI(imagen);
        } else if(requestCode == CODIGO_FOTO_ARCHIVO && resultCode == RESULT_OK) {
            String path = ImgFichero.getAbsolutePath();
            name = ImgFichero.getName();
            imageView.setImageBitmap(decodeFile(path));

        } else {
            // En caso de que el usuario haya pulsado el botón 'atrás' o haya salido de la galería
            // Destruir la actividad
            finish();
        }
    }

    public void upload(View v) {
        // Cuando se suba una foto se cogerá la localización actual si hubiera permisos de localización
        // Comprobar los permisos de la localización
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Pedir el permiso al usuario
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                // El permiso no está condecido
                // El usuario no quiere que le vuelvan a pedir el permiso de nuevo
                Toast.makeText(this, getString(R.string.noPermiso), Toast.LENGTH_SHORT).show();
            }
            // Pedir permiso al usuario
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            // El permiso ya estaba concedido
            LocationRequest location = LocationRequest.create();
            location.setInterval(1000);
            location.setFastestInterval(5000);
            location.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            refresh = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if(locationResult != null) {
                        // Obtener localización actual
                        latitud = String.valueOf(locationResult.getLastLocation().getLatitude());
                        longitud = String.valueOf(locationResult.getLastLocation().getLongitude());

                        System.out.println("Printeando info...");
                        System.out.println("Latitud: " + latitud);
                        System.out.println("Longitud: " + longitud);

                        provider.removeLocationUpdates(refresh);
                        // Ahora que ya tenemos todos los datos, vamos a subir la imagen al storage de firebase
                        uploadPhoto();
                    }
                }
            };

            provider = LocationServices.getFusedLocationProviderClient(this);
            provider.requestLocationUpdates(location, refresh, null);
        }
    }

    private void uploadPhoto() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        StorageReference spaceReference = storageReference.child(name);
        spaceReference.putFile(imagen);

        Toast.makeText(this, getString(R.string.uploading), Toast.LENGTH_LONG).show();
        System.out.println("Username_upload ====== "+ username);
        // Después vamos a guardar los datos de la imagen en la base de datos
        Data data = new Data.Builder()
                .putString("function", "subir")
                .putString("username", username)
                .putString("imageName", name)
                .putString("latitude", latitud)
                .putString("longitude", longitud)
                .build();

        // Restricciones de la conexión a internet
        Constraints restri = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Ejecutarlo solo una vez
        OneTimeWorkRequest one = new OneTimeWorkRequest.Builder(ImagesWorker.class)
                .setConstraints(restri)
                .setInputData(data)
                .build();

        // Recuperar la información de la respuesta
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(one.getId())
                .observe(this, status -> {
                    // Si todo ha ido correctamente, destruir la actividad para que el usuario pueda seguir subiendo fotos
                    if(status != null && status.getState().isFinished()) {

                        // Borrar el archivo temporal que se ha creado con la cámara
                        if("camera".equals(task)) {
                            try {
                                ImgFichero.delete();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Se hace esto para que en la actividad de profile puede cargarse de nuevo y aparezca la imagen que se subido ahora mismo
                                Intent intent = new Intent(UploadImage.this, MainActivity.class);
                                System.out.println("Username_rum ====== "+ username);
                                intent.putExtra("username", username);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }, 5000);
                    }
                });

        WorkManager.getInstance(this).enqueue(one);

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Llamamos de nuevo al método que cargará las imágenes de la base de datos
        //profile_fragment.loadImages(username);
    }

    // Método que se ejecuta al permitir o denegar los permisos de cámara o geolocalización
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.CAMERA)) {
                    // Si el permiso de cámara se ha concedido, se destruye la actividad y se vuelve a crear
                    Intent intent = new Intent(this, UploadImage.class);
                    System.out.println("UsernameCamera ====== "+ username);
                    intent.putExtra("username", username);
                    intent.putExtra("whattodo", task);
                    startActivity(intent);
                    finish();

                } else if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "Location permission granted. Click upload button again", Toast.LENGTH_SHORT).show();
                } else {
                    // Si el permiso de cámara se ha denegado, se destruye la actividad
                    finish();
                }
                return;
            }
        }
    }

    public Bitmap decodeFile(String path) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // The new size we want to scale to
            final int REQUIRED_SIZE = 70;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}

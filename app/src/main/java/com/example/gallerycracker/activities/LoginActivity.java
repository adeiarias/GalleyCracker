package com.example.gallerycracker.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.gallerycracker.R;
import com.example.gallerycracker.workers.TokenWorker;
import com.example.gallerycracker.workers.UsersWorker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText user_text;
    private EditText pass_text;
    private ImageView logo;

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

        setContentView(R.layout.login_page);

        user_text = findViewById(R.id.uname_login);
        pass_text = findViewById(R.id.passwd_login);
        logo = findViewById(R.id.imageView);

        // Añadir el logo de la aplicación al inicio
        logo.setImageResource(getApplicationContext().getResources().getIdentifier("logo", "drawable", getApplicationContext().getPackageName()));
    }

    // En este método se verificarán las credenciales de los usuarios
    public void onLogin(View v) {
        String username = user_text.getText().toString();
        String password = pass_text.getText().toString();

        // Los campos del login no pueden estar vacios
        if(username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.camposVacios), Toast.LENGTH_SHORT).show();
        } else {
            // login al usuario en la aplicación
            try {
                // Preparar los datos para enviar al backend
                Data logindata = new Data.Builder()
                        .putString("funcion", "login")
                        .putString("username", username)
                        .putString("password", password)
                        .build();

                // Tiene que existir conexión a internet
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                // Preparar la petición
                OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(UsersWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(logindata)
                        .build();

                // Lanzar la petición
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(req.getId())
                        .observe(this, status -> {
                            if (status != null && status.getState().isFinished()) {
                                String id_user = status.getOutputData().getString("datos");
                                if(!id_user.isEmpty()) {
                                    // añadir el token del dispositivo a la base de datos
                                    addFirebasetoken(username);
                                    // Avanzar a la siguiente actividad (MainActivity)
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.putExtra("username", username);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, getString(R.string.invalidCredentials), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                WorkManager.getInstance(this).enqueue(req);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Conseguimos el token de firebase y lo insertamos en la base de datos
    // El script del backend está configurado tal que si el token ya existiera, no se haría nada,
    // en caso de no existir, se inserta en la base de datos
    private void addFirebasetoken(String username) {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.d("firebase_error", "Firebase error: " + task.getException().toString());
                            return;
                        }
                        String token = task.getResult().getToken();
                        Log.d("token_firebase", "Token: " + token);
                        addToken(username, token);
                    }
                });
    }

    private void addToken(String username, String token) {
        try {
            // Preparar los datos para enviar al backend
            Data tokenData = new Data.Builder()
                    .putString("funcion", "addToken")
                    .putString("username", username)
                    .putString("token", token)
                    .build();

            // Tiene que existir conexión a internet
            Constraints restricciones = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            // Preparar la petición
            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(TokenWorker.class)
                    .setConstraints(restricciones)
                    .setInputData(tokenData)
                    .build();

            // Lanzar la petición
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(req.getId())
                    .observe(this, status -> {
                        if (status != null && status.getState().isFinished()) {
                            // El token se ha añadido correctamente
                            System.out.println("Token añadido correctamente");
                        }
                    });

            WorkManager.getInstance(this).enqueue(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Botón para crear un nuevo usuario
    public void onCreateAccount(View v) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}
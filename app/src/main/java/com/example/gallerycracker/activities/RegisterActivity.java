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
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.gallerycracker.R;
import com.example.gallerycracker.workers.UsersWorker;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText user_text;
    private EditText pass_text;
    private EditText email_text;
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

        setContentView(R.layout.register_page);

        user_text = findViewById(R.id.uname_register);
        pass_text = findViewById(R.id.passwd_register);
        email_text = findViewById(R.id.email_register);
    }

    // Método que se usa para crear un registro de un usuario
    public void onRegister(View v) {
        String username = user_text.getText().toString();
        String password = pass_text.getText().toString();
        String email = email_text.getText().toString();

        // Los campos del registro no pueden estar vacios
        if(username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, getString(R.string.camposVacios), Toast.LENGTH_SHORT).show();
        } else {
            // Registrar al usuario en la base de datos
            try {
                // https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work
                // Preparar los datos para enviar al backend
                Data registerData = new Data.Builder()
                        .putString("funcion", "register")
                        .putString("username", username)
                        .putString("password", password)
                        .putString("email", email)
                        .build();

                // Tiene que existir conexión a internet
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                // Preparar la petición
                OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(UsersWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(registerData)
                        .build();

                // Lanzar la petición
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(req.getId())
                        .observe(this, status -> {
                            if (status != null && status.getState().isFinished()) {
                                String id_user = status.getOutputData().getString("datos");
                                if(id_user.isEmpty()) {
                                    // El usuario no existe, por lo que se crea
                                    Toast.makeText(this, getString(R.string.userCreated), Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(this, getString(R.string.userExist), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                WorkManager.getInstance(this).enqueue(req);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
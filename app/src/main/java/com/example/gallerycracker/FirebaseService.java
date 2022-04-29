package com.example.gallerycracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseService extends FirebaseMessagingService {

    private String emisor;
    private String receptor;
    private String comentario;

    public FirebaseService(){}

    // Método que se ejecuta al recibir un mensaje FCM
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Si el mensaje FCM viene con datos guardarlas en las variables
        if (remoteMessage.getData().size() > 0) {
            emisor = remoteMessage.getData().get("emisor");
            receptor = remoteMessage.getData().get("receptor");
            comentario = remoteMessage.getData().get("mensaje");
        }

        // Si el mensaje FCM es una notificación
        if (remoteMessage.getNotification() != null) {
            // Creación del canal de notificaciones
            NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(this, "Notificaciones");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel elCanal = new NotificationChannel("Notificaciones", "CanalNotificaciones",
                        NotificationManager.IMPORTANCE_DEFAULT);
                elCanal.setDescription("Canal de notificaciones");
                elCanal.enableLights(true);
                elCanal.setLightColor(Color.RED);
                elCanal.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                elCanal.enableVibration(true);
                elManager.createNotificationChannel(elCanal);
            }

            // Configurar el texto del comentario
            elBuilder.setSmallIcon(android.R.drawable.ic_menu_send)
                    .setContentTitle("¡HOLA! " + receptor + ". El usuario " + emisor + " ha comentado en una de tus fotos con este comentario:")
                    .setContentText(comentario)
                    .setSubText("Comment")
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setAutoCancel(true);


            elManager.notify(1, elBuilder.build());

        }

    }
}

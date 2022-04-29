package com.example.gallerycracker.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.gallerycracker.R;
import com.example.gallerycracker.workers.ImagesWorker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DialogoBorrarFoto extends DialogFragment {

    // Listener del diálogo
    private ListenerDialogo miListener;
    public interface ListenerDialogo {
        void borrarFoto(String img);
    }

    private String imagen;
    private String username;

    public DialogoBorrarFoto(String pImagen, String pUsername) {
        imagen = pImagen;
        username = pUsername;
    }

    // Ejecutarse al iniciarse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        // Mantener la información del diálogo al girar el dispositivo
        setRetainInstance(true);

        miListener = (ListenerDialogo) getActivity();

        // Crear el diálogo de tipo alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.delete));
        builder.setMessage(getString(R.string.sureDelete));

        // Cuando se haga click en el botón sí
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Información a enviar
                Data datos = new Data.Builder()
                        .putString("function", "delete")
                        .putString("usuario", username)
                        .putString("imagen", imagen)
                        .build();

                // Restricciones: conexión a internet
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                // Ejecutar una sola vez
                OneTimeWorkRequest one = new OneTimeWorkRequest.Builder(ImagesWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

                // Recuperación de los resultados de la tarea
                WorkManager.getInstance(getActivity()).getWorkInfoByIdLiveData(one.getId())
                        .observe(getActivity(), status -> {
                            // Llamar al método borrar foto de la clase que use este listener
                            if (status != null && status.getState().isFinished()) {
                                miListener.borrarFoto(imagen);
                            }
                        });

                WorkManager.getInstance(getActivity()).enqueue(one);
            }
        });

        // En caso de que se haga click en el botón No, no se hará nada
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // No se hace nada
            }
        });

        return builder.create();
    }
}

package com.example.gallerycracker.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.gallerycracker.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Diálogo que se va mostrar cuando un usuario quiera descargar una foto de otra persona
public class DialogoDescargarFoto extends DialogFragment {

    // Listener del diálogo
    private ListenerDialogo miListener;
    public interface ListenerDialogo {
        void descargarFoto(Uri uri);
    }

    private String imagen;

    public DialogoDescargarFoto(String pImagen) {
        imagen = pImagen;
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
        builder.setTitle(getString(R.string.download));
        builder.setMessage(getString(R.string.sureDownload));

        // Cuando se haga click en el botón sí
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Descargar la foto del almacenamiento de firebase
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference pathReference = storageRef.child(imagen);
                pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // Llamar al método 'descargarFoto' de la actividad que implemente el listener
                        miListener.descargarFoto(uri);
                    }
                });
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

package com.example.gallerycracker.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.gallerycracker.R;

// Dialogo que usará para enviar un comentario a una persona sobre su foto
public class DialogoComentario extends DialogFragment {

    // Interfaz del listener
    private ListenerDialogo miListener;
    private String aquien;

    public interface ListenerDialogo {
        void enviarComentario(String comentario, String quien);
    }

    public DialogoComentario(String Paquien) {
        aquien = Paquien;
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        // Mantener información cuando se rote el dispositivo
        setRetainInstance(true);

        miListener =(ListenerDialogo) getActivity();

        // Crear un diálogo personalizado que permite meter un texto al usuario (comentario.xml)
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.comentarioTexto));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.comentario,null);
        builder.setView(view);

        EditText editTextComentario = view.findViewById(R.id.editTextComentario);

        // Se define el botón 'positivo' --> Enviará el comentario (notificación) al usuario propietario de la foto compartida
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String comentario = editTextComentario.getText().toString();
                if(!comentario.isEmpty()){
                    // Se llama al método 'enviarComentario' del listener en la actividad asociada
                    miListener.enviarComentario(comentario, aquien);
                }
                else {
                    // Si el comentario está vacío se vuelve a crear el diálogo
                    Toast.makeText(getActivity(), getString(R.string.camposVacios), Toast.LENGTH_SHORT).show();
                    DialogFragment dialogoEnviarComentario = new DialogoComentario(aquien);
                    dialogoEnviarComentario.show(getActivity().getSupportFragmentManager(), "enviar_comentario");
                }

            }
        });

        // Se define el botón 'negativo' --> Cancelará el diálogo actual
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'negativo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }

}

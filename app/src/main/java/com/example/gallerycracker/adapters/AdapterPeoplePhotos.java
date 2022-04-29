package com.example.gallerycracker.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gallerycracker.R;
import com.example.gallerycracker.activities.PeoplePhotos;
import com.example.gallerycracker.dialogs.DialogoComentario;
import com.example.gallerycracker.dialogs.DialogoDescargarFoto;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AdapterPeoplePhotos extends RecyclerView.Adapter<ViewHolderPeoplePhotos> {

    private String[] ids;
    private String username;
    private PeoplePhotos context;

    // Constructor del adapter
    public AdapterPeoplePhotos(PeoplePhotos pContext, String pUsername, String[] pIds) {
        context = pContext;
        username = pUsername;
        ids = pIds;
    }

    // Definir el cardview que se va a mostar para las imágenes
    @NonNull
    @Override
    public ViewHolderPeoplePhotos onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagen_cardview, parent, false);
        ViewHolderPeoplePhotos viewHolderOwnPhotos = new ViewHolderPeoplePhotos(layout);
        return viewHolderOwnPhotos;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderPeoplePhotos holder, int position) {
        holder.id = ids[position];
        holder.usuario = username;

        // Descaegar las fotos del storage de firebase
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        StorageReference path = storageReference.child(ids[position]);
        path.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context).load(uri).into(holder.imagen);
            }
        });

        // Definir qué hacer cuando se haga click en un elemento
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            // Se ejecuta al pulsar en un itemView (cuando se pulsa en un CardView que contiene la información de una foto)
            @Override
            public boolean onLongClick(View view) {
                // Crear el diálogo para descargar la foto
                DialogFragment dialogoDescargar = new DialogoDescargarFoto(holder.id);
                dialogoDescargar.show(context.getSupportFragmentManager(), "descargar_foto");
                // Descargar la imagen
                return false;
            }
        });

        // Definir qué hacer cuando se haga un click normal
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Crear un diálogo para enviar un comentario
                DialogFragment dialogoComentario = new DialogoComentario(username);
                dialogoComentario.show(context.getSupportFragmentManager(), "enviar_comentario");
            }
        });
    }

    // Devolver la cantidad de fotos
    @Override
    public int getItemCount() {
        return ids.length;
    }


}

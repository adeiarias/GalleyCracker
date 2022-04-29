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
import com.example.gallerycracker.activities.MainActivity;
import com.example.gallerycracker.dialogs.DialogoBorrarFoto;
import com.example.gallerycracker.dialogs.DialogoDescargarFoto;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AdapterOwnPhotos extends RecyclerView.Adapter<ViewHolderOwnPhotos> {

    private String[] ids;
    private String username;
    private MainActivity context;

    // Constructor del adapter
    public AdapterOwnPhotos(MainActivity pContext, String pUsername, String[] pIds) {
        context = pContext;
        username = pUsername;
        ids = pIds;
    }

    // Definir el cardview que se va a mostar para las imágenes
    @NonNull
    @Override
    public ViewHolderOwnPhotos onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagen_cardview, parent, false);
        ViewHolderOwnPhotos viewHolderOwnPhotos = new ViewHolderOwnPhotos(layout);
        return viewHolderOwnPhotos;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderOwnPhotos holder, int position) {
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
                // Se borrará la imagen de la base de datos
                DialogFragment dialogoBorrar = new DialogoBorrarFoto(holder.id, username);
                dialogoBorrar.show(context.getSupportFragmentManager(), "borrar_foto");
                // borrar imagen de la db
                return false;
            }
        });
    }

    // Devolver la cantidad de fotos
    @Override
    public int getItemCount() {
        return ids.length;
    }


}

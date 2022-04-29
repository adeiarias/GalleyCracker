package com.example.gallerycracker.adapters;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallerycracker.R;

public class ViewHolderPeoplePhotos extends RecyclerView.ViewHolder {

    public String usuario;
    public String id;
    public ImageView imagen;

    public ViewHolderPeoplePhotos(@NonNull View itemView) {
        super(itemView);

        // Inicializar los elementos del cardview
        imagen = itemView.findViewById(R.id.imagen_comida);
    }
}

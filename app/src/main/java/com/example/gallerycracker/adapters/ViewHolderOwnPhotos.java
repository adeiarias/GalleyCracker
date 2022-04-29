package com.example.gallerycracker.adapters;

import android.view.View;
import android.widget.ImageView;
import com.example.gallerycracker.R;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ViewHolderOwnPhotos extends RecyclerView.ViewHolder {

    public String usuario;
    public String id;
    public ImageView imagen;

    public ViewHolderOwnPhotos(@NonNull View itemView) {
        super(itemView);

        // Inicializar los elementos del cardview
        imagen = itemView.findViewById(R.id.imagen_comida);
    }
}

package com.example.gallerycracker.workers;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class ImagesWorker extends Worker {
    public ImagesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String funcion = getInputData().getString("function");

        try {
            // Se genera un HttpURLConnection para conectarse con el script de php
            // Direción en la que se encuentra el fichero php
            String direccion = "http://ec2-52-56-170-196.eu-west-2.compute.amazonaws.com/aarias023/WEB/php_scripts/images.php";
            HttpURLConnection urlConnection = null;
            URL url = new URL(direccion);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            // Todas las peticiones que se realicen (añadirToken y conseguirTokens) se realizarán mediante el método POST
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            // Cabecera para especificar de qué forma se envía la información
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Comprobar qué se quiere hacer

            // Añadir token a la base de datos
            if("subir".equals(funcion)){
                // Se recogen el usuario y la contraseña
                String username = getInputData().getString("username");
                String name = getInputData().getString("imageName");
                String latitude = getInputData().getString("latitude");
                String longitude = getInputData().getString("longitude");

                // Preparar los parámetros para enviar en la petición
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("function", funcion)
                        .appendQueryParameter("username", username)
                        .appendQueryParameter("imageName", name)
                        .appendQueryParameter("longitude", longitude)
                        .appendQueryParameter("latitude", latitude);
                String parametros = builder.build().getEncodedQuery();

                // Se incluyen los parámetros en la petición HTTP
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                // Se ejecuta la llamada al servicio web
                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    return Result.success();
                } else { // En caso de haber algún error
                    return Result.failure();
                }
            } else if("getPhotos".equals(funcion)) {
                // Se realizar el registro del usuario en la base de datos
                String username = getInputData().getString("username");

                // Preparar los parámetros para enviar en la petición
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("function", funcion)
                        .appendQueryParameter("username", username);
                String parametros = builder.build().getEncodedQuery();

                // Se incluyen los parámetros en la petición HTTP
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                // Se consiguen los tokens del usuario
                int statusCode = urlConnection.getResponseCode();
                String line, result = "";
                if (statusCode == 200) {
                    // Cósigo 200 OK, se leen los datos de la respuesta
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    inputStream.close();

                    // Devlver los resultados

                }
                Data resultados = new Data.Builder()
                        .putString("datos", result)
                        .build();

                // Devolver que todo ha ido bien
                return Result.success(resultados);

            } else if("delete".equals(funcion)){
                // Se recogen el usuario y la contraseña
                String username = getInputData().getString("usuario");
                String name = getInputData().getString("imagen");

                // Preparar los parámetros para enviar en la petición
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("function", funcion)
                        .appendQueryParameter("username", username)
                        .appendQueryParameter("imageName", name);
                String parametros = builder.build().getEncodedQuery();

                // Se incluyen los parámetros en la petición HTTP
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                // Se ejecuta la llamada al servicio web
                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    return Result.success();
                } else { // En caso de haber algún error
                    return Result.failure();
                }

            } else if("getLocations".equals(funcion)){
                // Conseguir las localizaciones de las imágenes subidas por el usuario
                String username = getInputData().getString("username");

                // Preparar los parámetros para enviar en la petición
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("function", funcion)
                        .appendQueryParameter("username", username);
                String parametros = builder.build().getEncodedQuery();

                // Se incluyen los parámetros en la petición HTTP
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                // Se ejecuta la llamada al servicio web
                int statusCode = urlConnection.getResponseCode();
                String line, result = "";
                if (statusCode == 200) {
                    // Cósigo 200 OK, se leen los datos de la respuesta
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    inputStream.close();

                    // Devlver los resultados

                }

                Data resultados = new Data.Builder()
                        .putString("datos", result)
                        .build();

                // Devolver que todo ha ido bien
                return Result.success(resultados);

            } else {
                // Algo no ha ido de forma correcta
                return Result.failure();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.failure();
    }
}

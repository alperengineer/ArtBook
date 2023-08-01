package com.aok.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.aok.artbook.databinding.ActivityArtBinding;
import com.aok.artbook.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;

    SQLiteDatabase sqLiteDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        sqLiteDatabase = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.matches("new")){
            binding.artNameText.setText("");
            binding.artistNameText.setText("");
            binding.yearText.setText("");
            binding.btnSave.setVisibility(View.VISIBLE);

            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.selectimg);
            binding.imageView.setImageResource(R.drawable.selectimg);

        }else{
            int artId = intent.getIntExtra("artId", 0);
            binding.btnSave.setVisibility(View.INVISIBLE);

            try {

                Cursor cursor =sqLiteDatabase.rawQuery("SELECT * FROM arts WHERE id = ?", null);

                int artNameIx = cursor.getColumnIndex("artName");
                int artistNameIx = cursor.getColumnIndex("artistName");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while(cursor.moveToNext()){
                    binding.artNameText.setText(cursor.getString(artNameIx));
                    binding.artistNameText.setText(cursor.getString(artistNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] byteArray = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }


    }

    public void save(View view){

        String artName = binding.artNameText.getText().toString();
        String artistName = binding.artistNameText.getText().toString();
        String year = binding.yearText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        ByteArrayOutputStream outputStream =  new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artName VARCHAR, artistName VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = ("INSERT INTO  arts (artName, artistName, year, image) VALUES(?, ?, ?, ?)");

            SQLiteStatement sqLiteStatement = sqLiteDatabase.compileStatement(sqlString);
            sqLiteStatement.bindString(1, artName);
            sqLiteStatement.bindString(2, artistName);
            sqLiteStatement.bindString(3, year);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(ArtActivity.this, MainActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Program içinde açılan tüm operasyonları kapatır ve sadece yeni açılacak olan açılır
        startActivity(intent);

    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if(bitmapRatio > 1){
            // Görsel yataydır(Landscape)

            width = maximumSize;
            height = (int) (width * bitmapRatio);

        }else { // (bitmapRatio <= 1)
            // Görsel kare veya dikeydir(Portrait)

            height = maximumSize;
            width = (int) (height * bitmapRatio);

        }

        return image.createScaledBitmap(image, width, height, true);
    }

    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view, "Galeriyi görüntülemek için izin gerek", Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            } else {

                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

            }

            // İznin verilip verilmediğini kontrol edilir. Eğer verildiyse else bloğuna gider.


        }else{
            // İzin daha verilmiştir.

            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);

        }

    }

    private void registerLauncher(){

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == RESULT_OK){
                    // Kullanıcı galeriden bir şeyler seçti.
                    Intent intentFromResult = result.getData();
                    if(intentFromResult != null){
                        Uri imageData = intentFromResult.getData();
                        //binding.imageView.setImageURI(imageData);

                        try {
                            // Seçilen fotoğrafı bitmapa çevirme işlemi

                            if(Build.VERSION.SDK_INT >= 28) {

                                // Versiyon kontrolü yaptık.

                                ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(), imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            }else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    // İzin verildi
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }else {
                    // İzin verilmedi
                    Toast.makeText(ArtActivity.this, "İzin Verilmedi", Toast.LENGTH_LONG).show();
                }
            }
        });


    }
}
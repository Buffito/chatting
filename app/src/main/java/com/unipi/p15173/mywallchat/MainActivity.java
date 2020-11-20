package com.unipi.p15173.mywallchat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messengerTextView = itemView.findViewById(R.id.messengerTextView);
            messengerImageView = itemView.findViewById(R.id.messengerImageView);
        }
    }

    String username, photoUrl;

    RecyclerView messageRecyclerView;
    LinearLayoutManager linearLayoutManager;
    EditText editText;
    ImageView imageViewSend;

    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;
    FirebaseRecyclerAdapter<Message, MessageViewHolder>
            firebaseAdapter;
    FirebaseStorage storage;
    StorageReference storageReference;

    LocationManager locationManager;
    boolean stop;
    Geocoder geocoder;
    Double latitude, longitude;

    File storagePath;
    File localFile;
    String filePrefix, fileSuffix;

    String[] appPermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final int PERMISSIONS_CODE = 123;
    static boolean granted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askForPermissions();

    }

    private void init() {
        editText = findViewById(R.id.editText);
        imageViewSend = findViewById(R.id.imageViewSend);
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageRecyclerView.setLayoutManager(linearLayoutManager);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        imageViewSend.setClickable(true);
        imageViewSend.setOnClickListener(v -> sendMessage());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        geocoder = new Geocoder(this, Locale.getDefault());
        storage = FirebaseStorage.getInstance();



        messageRecyclerView.addOnItemTouchListener(new CustomTouchListener(this, (view, index) -> download(index)));

    }


    private void updateRecyclerView() {
        SnapshotParser<Message> parser = dataSnapshot -> {
            Message message = dataSnapshot.getValue(Message.class);
            if (message != null) {
                message.setId(dataSnapshot.getKey());
            }
            return message;
        };

        DatabaseReference messagesRef = databaseReference.child("messages");
        FirebaseRecyclerOptions<Message> options =
                new FirebaseRecyclerOptions.Builder<Message>()
                        .setQuery(messagesRef, parser)
                        .build();
        firebaseAdapter = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                View v = inflater.inflate(R.layout.item_message, viewGroup, false);

                return new MessageViewHolder(v);
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            Message message) {

                if (message.getText() != null) {
                    viewHolder.messageTextView.setText(message.getText());
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else if (message.getUrl() != null) {
                    String url = message.getUrl();
                    if (url.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(url);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                task -> {
                                    if (task.isSuccessful()) {
                                        String downloadUrl = task.getResult().toString();
                                        Glide.with(viewHolder.messageImageView.getContext())
                                                .load(downloadUrl)
                                                .into(viewHolder.messageImageView);
                                    } else {
                                        Toast.makeText(MainActivity.this, "Getting download url was not successful.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(message.getUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.messageTextView.setVisibility(TextView.GONE);
                }


                viewHolder.messengerTextView.setText(message.getName());
                if (message.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(MainActivity.this)
                            .load(message.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }

            }
        };

        firebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = firebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    messageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        messageRecyclerView.setAdapter(firebaseAdapter);
    }

    private void sendMessage() {
        if (editText.getText().toString().isEmpty()) {
            Toast.makeText(MainActivity.this, "Type a message first.", Toast.LENGTH_SHORT).show();
        } else {
            Message message = new
                    Message(editText.getText().toString(),
                    username,
                    photoUrl,
                    null /* no image */);
            databaseReference.child("messages")
                    .push().setValue(message);
            editText.getText().clear();
        }
        messageRecyclerView.setAdapter(firebaseAdapter);
    }

    @Override
    public void onStart() {
        if (granted)
            firebaseAdapter.startListening();
        super.onStart();
    }

    @Override
    public void onPause() {
        if (granted)
            firebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        if (granted){
            firebaseAdapter.startListening();
            getUserInfo();
        }
        super.onResume();

    }

    @Override
    public void onDestroy() {
        if (granted)
            firebaseAdapter.stopListening();
        super.onDestroy();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.features, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sign_out) {
            AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> signOut());
        } else if (item.getItemId() == R.id.image) {
            uploadImage();
        } else if (item.getItemId() == R.id.file) {
            uploadPDF();
        } else if (item.getItemId() == R.id.location) {
            stop = false;
            uploadLocation();
        } else if (item.getItemId() == R.id.voice) {
            uploadVoice();
        }
        return true;
    }

    private void uploadPDF() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF file"), 1212);
    }

    private void uploadImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 234);
    }

    private void uploadVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
        startActivityForResult(intent, 742);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void uploadLocation() {
        getDeviceLocation();
    }

    private void signOut() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(task -> {
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
            this.recreate();
        });
    }

    private void authenticateUser() {
        if (firebaseUser == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), 1);
        } else {
            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();

        }
        getUserInfo();
        init();
        updateRecyclerView();
    }

    private void getUserInfo() {
        try {
            username = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null) {
                photoUrl = firebaseUser.getPhotoUrl().toString();
            }
        } catch (Exception ignored) {

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 234 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();

            Message tempMessage = new Message(null, username, photoUrl, null);
            databaseReference.child("messages").push()
                    .setValue(tempMessage, (databaseError, databaseReference) -> {
                        if (databaseError == null) {
                            try {
                                String key = databaseReference.getKey();
                                StorageReference storageReference = FirebaseStorage.getInstance()
                                        .getReference(firebaseUser.getUid())
                                        .child(key).child(uri.getLastPathSegment());
                                putImageInStorage(storageReference, uri, key);
                            } catch (NullPointerException ignored) {
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to write message to database.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else if (requestCode == 1212 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();

            String uriString = uri.toString();
            File myFile = new File(uriString);
            String displayName = null;

            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = this.getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                displayName = myFile.getName();
            }

            Message tempMessage = new Message(null, username, photoUrl, null);
            String finalDisplayName = displayName;
            databaseReference.child("messages").push()
                    .setValue(tempMessage, (databaseError, databaseReference) -> {
                        if (databaseError == null) {
                            try {
                                String key = databaseReference.getKey();
                                StorageReference storageReference = FirebaseStorage.getInstance()
                                        .getReference(firebaseUser.getUid())
                                        .child(key).child(uri.getLastPathSegment());
                                putFileInStorage(storageReference, uri, key, finalDisplayName);
                            } catch (NullPointerException ignored) {
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to write message to database.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else if (requestCode == 742 && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (!results.isEmpty()) {
                editText.setText(results.get(0) + ("  (Speech-to-text message)"));
                sendMessage();
            } else
                Toast.makeText(MainActivity.this, "Could not pick up your voice.", Toast.LENGTH_SHORT).show();
        }else if (requestCode == 1 && requestCode == RESULT_OK){
            getUserInfo();
        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading");
        progressDialog.show();

        storageReference.putFile(uri)
                .addOnCompleteListener(MainActivity.this, task -> {
                    if (task.isSuccessful()) {
                        task.getResult().getMetadata().getReference().getDownloadUrl()
                                .addOnCompleteListener(MainActivity.this,
                                        task1 -> {
                                            if (task1.isSuccessful()) {
                                                progressDialog.dismiss();
                                                Message message =
                                                        new Message(null, username, photoUrl,
                                                                task1.getResult().toString());
                                                databaseReference.child("messages").child(key)
                                                        .setValue(message);
                                            }
                                        });
                    } else {
                        Toast.makeText(MainActivity.this, "File upload task was not successful.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(taskSnapshot -> {
            //calculating progress percentage
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

            //displaying percentage in progress dialog
            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
        });
    }

    private void putFileInStorage(StorageReference storageReference, Uri uri, final String key, String displayName) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading");
        progressDialog.show();

        storageReference.putFile(uri)
                .addOnCompleteListener(MainActivity.this, task -> {
                    if (task.isSuccessful()) {
                        task.getResult().getMetadata().getReference().getDownloadUrl()
                                .addOnCompleteListener(MainActivity.this,
                                        task1 -> {
                                            if (task1.isSuccessful()) {
                                                progressDialog.dismiss();
                                                Message message =
                                                        new Message(displayName, username, photoUrl,
                                                                task1.getResult().toString());
                                                databaseReference.child("messages").child(key)
                                                        .setValue(message);
                                            }
                                        });
                    } else {
                        Toast.makeText(MainActivity.this, "File upload task was not successful.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(taskSnapshot -> {
            //calculating progress percentage
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

            //displaying percentage in progress dialog
            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getDeviceLocation() {

        stop = false;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForPermissions();
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (!stop) {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            stop = true;
                        }
                    } else {
                        List<Address> addresses = null;

                        try {
                            addresses = geocoder.getFromLocation(
                                    latitude,
                                    longitude,
                                    1);

                        } catch (Exception ignored) {
                        }
                        if (addresses != null || addresses.size() != 0) {
                            Address address = addresses.get(0);
                            ArrayList<String> addressFragments = new ArrayList<>();

                            // Fetch the address lines using getAddressLine,
                            // join them, and send them to the thread.
                            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                addressFragments.add(address.getAddressLine(i));
                            }
                            String str = TextUtils.join(System.getProperty("line.separator"),
                                    addressFragments);

                            locationManager.removeUpdates(this);
                            stop = true;
                            editText.setText("Current location is: " + str);
                            sendMessage();
                        } else {
                            Toast.makeText(MainActivity.this, "Could not get address.", Toast.LENGTH_SHORT).show();
                        }

                    }
                } else {
                    Toast.makeText(MainActivity.this, "Could not get address.", Toast.LENGTH_SHORT).show();
                    locationManager.removeUpdates(this);
                    stop = true;
                }
            }


            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

        });
    }

    private void download(int key)  {
        DatabaseReference dbRef = databaseReference.child("messages");

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                int length = (int) dataSnapshot.getChildrenCount();
                String[] sampleString = new String[length];
                int i = 0;
                while(i < length) {
                    sampleString[i] = iterator.next().getValue().toString();
                    i++;
                }
                dbRef.removeEventListener(this);


                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                if (sampleString[key].contains("https://firebasestorage")){
                    String[] strings1 = sampleString[key].split("url=");
                    String replace = strings1[1].replace("}", "");

                    if (strings1[0].contains(".pdf")){
                        filePrefix = "document";
                        fileSuffix = ".pdf";
                    }else{
                        filePrefix = "image";
                        fileSuffix = ".jpg";
                    }

                    storagePath = new File(Environment.getExternalStorageDirectory(), "chat_downloads");

                    if(storagePath.exists()) {
                        //localFile = new File(storagePath,filePrefix);
                        try {
                            localFile = File.createTempFile(filePrefix,fileSuffix,storagePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        storageReference = storage.getReferenceFromUrl(replace);
                        storageReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                            Toast.makeText(MainActivity.this,"Saved "+filePrefix,Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }).addOnFailureListener(exception -> {
                            Toast.makeText(MainActivity.this,"Could not download " + filePrefix ,Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }).addOnProgressListener(taskSnapshot -> {
                            progressDialog.setTitle("Downloading");
                            progressDialog.show();
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            progressDialog.setMessage("Downloaded " + ((int) progress) + "%...");
                        });
                    }else {
                        storagePath.mkdir();
                    }


                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void askForPermissions() {
        ActivityCompat.requestPermissions(this,appPermissions,PERMISSIONS_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_CODE) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED ) {
                granted = true;
                authenticateUser();

            }else
                askForPermissions();
        }
    }

}

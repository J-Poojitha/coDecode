package com.koddev.chatapp.Adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;
import com.koddev.chatapp.Model.Chat;
import com.koddev.chatapp.R;
import com.scottyab.aescrypt.AESCrypt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private static final int SELECT_FILE1 = 1;
    int CAMERA_REQUEST =123;
    int sa=0;
    String selectedPath1 = "NONE";
    String api_key="4b17b505b467ae52fed2c8045cbe638934de3ebd";
    ProgressDialog progressDialog;
    Button b1,b3,b2;
    AlertDialog alertDialog;
    public static  final int MSG_TYPE_LEFT = 0;
    public static  final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<Chat> mChat;
    private String imageurl;
    AlertDialog a;
    ViewGroup p;
    FirebaseUser fuser;

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageurl){
        this.mChat = mChat;
        this.mContext = mContext;
        this.imageurl = imageurl;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            p=parent;
            return new MessageAdapter.ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            p=parent;
            return new MessageAdapter.ViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull final MessageAdapter.ViewHolder holder, int position) {

        Chat chat = mChat.get(position);

        holder.show_message.setText(chat.getMessage());
        holder.show_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(sa==1) {
                    AlertDialog.Builder b = new AlertDialog.Builder(mContext);

                    LayoutInflater l = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


                    View v1 = l.inflate(R.layout.alert_password, p, false);
                    ;

                    final EditText name = (EditText) v1.findViewById(R.id.pass);
                    Button submit = (Button) v1.findViewById(R.id.save);
                    Button cancel = (Button) v1.findViewById(R.id.cancel);
                    submit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!TextUtils.isEmpty(name.getText().toString())) {
                                try {
                                    holder.show_message.setText(AESCrypt.decrypt(name.getText().toString(), holder.show_message.getText().toString()));
                                    a.dismiss();
                                    Toast.makeText(mContext, "Decrypted Successfully", Toast.LENGTH_SHORT).show();

                                } catch (GeneralSecurityException e) {
                                    e.printStackTrace();
                                    Toast.makeText(mContext, "Wrong Password", Toast.LENGTH_SHORT).show();


                                }
                            } else {
                                Toast.makeText(mContext, "Please enter the Password", Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            a.dismiss();
                        }
                    });
                    b.setView(v1);
                    a = b.create();
                    a.show();
                }
                else{
                    takeImageFromCamera(v);
                }
            }
        });

        if (imageurl.equals("default")){
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            Glide.with(mContext).load(imageurl).into(holder.profile_image);
        }

        if (position == mChat.size()-1){
            if (chat.isIsseen()){
                holder.txt_seen.setText("Seen");
            } else {
                holder.txt_seen.setText("Delivered");
            }
        } else {
            holder.txt_seen.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public  class ViewHolder extends RecyclerView.ViewHolder{

        public TextView show_message;
        public ImageView profile_image;
        public TextView txt_seen;

        public ViewHolder(View itemView) {
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            profile_image = itemView.findViewById(R.id.profile_image);
            txt_seen = itemView.findViewById(R.id.txt_seen);
        }
    }

    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChat.get(position).getSender().equals(fuser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }


    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = ((Activity) mContext).managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    public void takeImageFromCamera(View view) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        ((Activity) mContext).startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }
    private void storeImage(Bitmap image) {
        String imageFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/name.png";
        File pictureFile = new File (imageFilePath);
        if (pictureFile == null) {
            Log.d("Data",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();

            selectedPath1 = imageFilePath;
            data d=new data();
            d.execute();
        } catch (FileNotFoundException e) {
            Log.d("Data", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("Data", "Error accessing file: " + e.getMessage());
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SELECT_FILE1 &&resultCode == RESULT_OK) {

            Uri selectedImageUri = data.getData();
            if (requestCode == SELECT_FILE1)
            {
                selectedPath1 = getPath(selectedImageUri);
                System.out.println("selectedPath1 : " + selectedPath1);
            }

        }
        else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap mphoto = (Bitmap) data.getExtras().get("data");
            storeImage(mphoto);
        }
    }
    class data extends AsyncTask<Void,Void,String>{
        @Override
        protected String doInBackground(Void... voids) {
            IamOptions options = new IamOptions.Builder()
                    .apiKey("Isrft072J4Fu9i4wVVNwDcECFLsfZaaBpGGfNsKdBzNh")
                    .build();

            VisualRecognition service = new VisualRecognition("2018-03-19", options);
            List s=new ArrayList();
            s.add("DefaultCustomModel_645260346");
            InputStream imagesStream = null;
            try {
                imagesStream = new FileInputStream(selectedPath1);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            ClassifyOptions classifyOptions = new ClassifyOptions.Builder()
                    .imagesFile(imagesStream)
                    .classifierIds(s)
                    .imagesFilename("name.jpg")
                    .threshold((float) 0.6)
                    .owners(Arrays.asList("me"))
                    .build();
            ClassifiedImages result = service.classify(classifyOptions).execute();
            System.out.println(result);
            return result.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("Data",s);
            try {
                JSONObject jsonObj = new JSONObject(s);
               JSONArray ja= jsonObj.getJSONArray("images");
                    JSONObject j=ja.getJSONObject(0);
                JSONArray ja1= j.getJSONArray("classifiers");
                JSONObject j1=ja1.getJSONObject(0);
                JSONArray ja2= j1.getJSONArray("classes");
                JSONObject j2=ja2.getJSONObject(0);
               Log.d("PredictedOutput", j2.getString("class"));
               String d=j2.getString("class");
               if(!TextUtils.isEmpty(d)){
                    sa=1;
                   Toast.makeText(mContext, "Authentication Done", Toast.LENGTH_SHORT).show();

               }
               else {
                   Toast.makeText(mContext, "Invalid Authentication", Toast.LENGTH_SHORT).show();
               }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }
}
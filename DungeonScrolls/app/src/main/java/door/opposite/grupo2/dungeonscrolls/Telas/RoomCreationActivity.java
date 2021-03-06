package door.opposite.grupo2.dungeonscrolls.Telas;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;


import door.opposite.grupo2.dungeonscrolls.R;
import door.opposite.grupo2.dungeonscrolls.commands.Eventos;
import door.opposite.grupo2.dungeonscrolls.databinding.ActivityRoomCreationBinding;
import door.opposite.grupo2.dungeonscrolls.graficAssets.DialogFragmentCreator;
import door.opposite.grupo2.dungeonscrolls.model.SQLite;
import door.opposite.grupo2.dungeonscrolls.model.Sala;
import door.opposite.grupo2.dungeonscrolls.model.Usuario;
import door.opposite.grupo2.dungeonscrolls.viewmodel.SalaModel;

public class RoomCreationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    DialogFragmentCreator geraDialog = new DialogFragmentCreator();     // Objeto da classe DialogFragmentCreator aonde estão ferramentas para criar Dialog Fragments
    AlertDialog dialog;
    AlertDialog dialogCamera;
    DialogFragmentCreator geraDialogCamera = new DialogFragmentCreator();
    Intent extra;
    Usuario usuarioLogado;
    ActivityRoomCreationBinding binding;
    SQLite sqLite;
    ImageView campoImagem;
    boolean pegoFoto = false;
    StorageReference storage;
    Uri buffer;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this,R.layout.activity_room_creation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        campoImagem = (ImageView) findViewById(R.id.sala_imageView);
        sqLite = new SQLite(this);
        binding.setSalamodel(new SalaModel());
        storage = FirebaseStorage.getInstance().getReference();
        final AlertDialog.Builder builder = new AlertDialog.Builder(RoomCreationActivity.this);
        final String salaJaExistente = getResources().getString(R.string.toast_roomCreation_salaJaExistente);
        final String salaCriada = getResources().getString(R.string.toast_roomCreation_salaCriada);

        sqLite.atualizaDataSala(this);

        extra = getIntent();
        usuarioLogado = (Usuario) extra.getSerializableExtra("usuarioLogado");
        extra = new Intent(this, RoomsMenu.class);

        binding.setCadevent(new Eventos() {
            @Override
            public void onClickCad() {

                // Cria uma referência para o dialogfragment_loadingcircle para poder gerar seu layout e referenciar aquilo que tem dentro dele
                View loadingCircleDialog = getLayoutInflater().inflate(R.layout.dialogfragment_loadingcircle, null);
                // Cria o Dialog Fragment através de um dos métodos da classe DialogFragmentCreator e pega a referência para ele, além de rodar a animação de Loading
                dialog = geraDialog.criaDialogFragmentLoadingCircle(RoomCreationActivity.this, loadingCircleDialog);

                try{
                    sqLite.selecionarSala(binding.getSalamodel().getNome());
                    Toast.makeText(RoomCreationActivity.this, salaJaExistente, Toast.LENGTH_LONG).show();
                    geraDialog.fechaDialogFragment(dialog);
                }catch(Exception e) {


                    Uri uri;

                    if (pegoFoto) {
                        uri = buffer;
                    } else {

                        uri = Uri.parse("android.resource://door.opposite.grupo2.dungeonscrolls/" + R.drawable.no_image_selection_room_1);
                    }
                    StorageReference path = storage.child("FotosSala").child(uri.getLastPathSegment());
                        path.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                boolean foiInserido = false;
                                Uri uriCerta = taskSnapshot.getDownloadUrl();
                                Sala sala = new Sala(binding.getSalamodel().getNome(), binding.getSalamodel().getSenha(), usuarioLogado.getID(),
                                        binding.getSalamodel().getHistoria(), usuarioLogado.getNick());
                                if (binding.roomPasswordPlainText.getText().length() == 0) {
                                    sala.setSenha(" ");
                                }
                                sala.setUri(uriCerta.toString());
                                foiInserido = sqLite.insereDataSala(sala);
                                Sala sala1;
                                sala1 = sqLite.selecionarSala(binding.getSalamodel().getNome());

                                int[] aux = new int[usuarioLogado.toIntArray(usuarioLogado.getSalasID()).length + 1];

                                for (int i = 0; i < usuarioLogado.toIntArray(usuarioLogado.getSalasID()).length; i++) {
                                    aux[i] = usuarioLogado.toIntArray(usuarioLogado.getSalasID())[i];
                                }
                                aux[usuarioLogado.toIntArray(usuarioLogado.getSalasID()).length] = sala1.getID();

                                usuarioLogado.setSalasID(usuarioLogado.toIntList(aux));

                                sqLite.updateDataUsuario(usuarioLogado);
                                if (foiInserido == true) {
                                    Toast.makeText(RoomCreationActivity.this, salaCriada, Toast.LENGTH_LONG).show();
                                    extra.putExtra("usuarioLogado", usuarioLogado);
                                    startActivity(extra);
                                } else {
                                    geraDialog.fechaDialogFragment(dialog);
                                }
                            }
                        });

                }   }
            @Override
            public void onClickLogin(){
                View loadingCircleDialog = getLayoutInflater().inflate(R.layout.dialogfragment_photos, null);
                dialogCamera = geraDialogCamera.criaDialogFragmentLoadingCamera(RoomCreationActivity.this, loadingCircleDialog);
                Button bt_camera = (Button)loadingCircleDialog.findViewById(R.id.bt_camera);
                Button bt_galeria = (Button)loadingCircleDialog.findViewById(R.id.bt_galeria);
                bt_camera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(i, 0);
                        geraDialogCamera.fechaDialogFragment(dialogCamera);
                    }
                });

                bt_galeria.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, 1);
                        geraDialogCamera.fechaDialogFragment(dialogCamera);

                    }
                });


            }
        });


    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 0) {
            if (data != null) {
                Bundle bundle = data.getExtras();

                // Recupera o Bitmap retornado pela c�mera
                Bitmap bitmap = (Bitmap) bundle.get("data");

                // Atualiza a imagem na tela
                buffer = getImageUri(this, bitmap);
                campoImagem.setImageBitmap(bitmap);
                pegoFoto = true;


            }
        }

        if(requestCode == 1){

            if (data != null) {


                // Atualiza a imagem na tela
                buffer = data.getData();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(RoomCreationActivity.this.getContentResolver(),buffer);
                } catch (IOException e) {
                    System.out.println("oush");
                }
                campoImagem.setImageBitmap(bitmap);
                pegoFoto = true;

            }

        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // dialog só é null antes de ser instanciado, apenas por garantia para não dar erros
        if(dialog != null){
            // Usado para fechar o Dialog Fragment do Loading Magic Circle, é chamado no onStop() pois ele apenas ocorre quando outra activity é chamada
            // e essa sai de visualização, logo após não estar mais visível.
            geraDialog.fechaDialogFragment(dialog);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_navigationDrawer_item_listaDeSalas:
                extra = new Intent(RoomCreationActivity.this, RoomsMenu.class);
                extra.putExtra("usuarioLogado", usuarioLogado);

                startActivity(extra);
                return true;

            case R.id.menu_navigationDrawer_item_sairDaConta:
                extra = new Intent(RoomCreationActivity.this, MainActivity.class);
                startActivity(extra);
                return true;
        }
        return true;
    }
}

package com.example.teacher.myapplication.view;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.teacher.myapplication.BaseActivity;
import com.example.teacher.myapplication.R;
import com.example.teacher.myapplication.model.Contact;
import com.example.teacher.myapplication.view.adapter.ContactAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity implements ContactAdapter.ContactListener {

    private ListView listViewContacts;
    private ContactAdapter contactAdapter;

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    List<String> contactos;
    List<String> numeros;
    List<Contact> informacionContactos;

    @Override
    public int getLayout() {
        return R.layout.activity_home;
    }

    @Override
    public void onLayoutIsReady(Bundle savedInstanceState, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        }

        SharedPreferences misPreferencias = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        boolean esPrimeraVez = misPreferencias.getBoolean("primeraVez", true);

        obtenerNombresContactos();
        mostrarMenu();


        Button botonGuardar =  (Button) findViewById(R.id.guardar);
        botonGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                obtenerNombresContactos();
                mostrarMenu();
            }
        });






        listViewContacts = (ListView) findViewById(R.id.listViewContacts);
        List<Contact> mockContacts = this.informacionContactos;
        contactAdapter = new ContactAdapter(
                this,
                R.layout.item_contact,
                mockContacts,
                this);

        listViewContacts.setAdapter(contactAdapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ADD_CONTACT && resultCode == RESULT_OK) {
            String name = data.getStringExtra(EXTRA_KEY_NAME);
            String family = data.getStringExtra(EXTRA_KEY_FAMILY);
            String phoneNumber = data.getStringExtra(EXTRA_KEY_PHONE_NUMBER);

            contactAdapter.addContact(name, family, phoneNumber);
        }
    }

    public void onAddContact(View view) {
        Intent intent = new Intent(this, AddContactActivity.class);
        startActivityForResult(intent, REQ_ADD_CONTACT);
    }

    @Override
    public void onContactClicked(Contact contact, int position) {
        Toast.makeText(this, "Clicked at pos " + position + " Contact name : " + contact.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onContactLongClicked(Contact contact, int position) {
        Toast.makeText(this, "Long clicked at pos " + position + " Contact name : " + contact.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreClicked(View view, final Contact contact, final int position) {
        Toast.makeText(this, "More clicked at pos " + position + " Contact name : " + contact.getName(), Toast.LENGTH_SHORT).show();
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.menu_contact_more);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                int itemId = menuItem.getItemId();

                switch (itemId) {

                    case R.id.actionCall:
                        // TODO: 2017/12/23 add a number on dialing !
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        startActivity(intent);
                        break;

                    case R.id.actionEdit:
                        // TODO: 2017/12/23 show a dialog that has three edit texts that can update contact info
                        promptUserToUpdateContactInfo(contact,position);
                        break;

                    case R.id.actionRemove:
                        contactAdapter.removeContact(position);
                        break;
                }

                return true;
            }
        });

        popupMenu.show();
    }

    private void promptUserToUpdateContactInfo(final Contact contact , final int position){

        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_edit_contact, null);
        final EditText editName = (EditText) dialogView.findViewById(R.id.editName);
        final EditText editFamily = (EditText) dialogView.findViewById(R.id.editFamily);
        final EditText editPhoneNumber = (EditText) dialogView.findViewById(R.id.editPhoneNumber);

        editName.setText(contact.getName());
        editFamily.setText(contact.getFamily());
        editPhoneNumber.setText(contact.getPhoneNumber());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update contact info !");
        builder.setView(dialogView);
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                contact.setName(editName.getText().toString().trim());
                contact.setFamily(editFamily.getText().toString().trim());
                contact.setPhoneNumber(editPhoneNumber.getText().toString().trim());

                contactAdapter.updateContact(contact, position);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private List<String> obtenerNombresContactos() {
        contactos = new ArrayList<>();
        numeros = new ArrayList<>();

        informacionContactos  = new ArrayList<>();
        boolean primero = true;
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                primero = true;

                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0){


                    Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);

                    while (phones.moveToNext()) {
                        if(primero){
                            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            Log.i("App", phoneNumber);
                            numeros.add(phoneNumber);
                            primero = false;
                        }

                    }
                    phones.close();

                }

                contactos.add(name);

            } while (cursor.moveToNext());
        }
        cursor.close();
        for(int i = 0; i < contactos.size(); i++){
            informacionContactos.add(new Contact(contactos.get(i), "" ,numeros.get(i)));
            Log.d("Aplicacion","Size del vector: " + this.informacionContactos.size());
        }
        return contactos;
    }


    public void escribirEnAlmacenamientoInterno(Context mcoContext, String sFileName, String sBody){
        File file = new File(mcoContext.getFilesDir(),"mydir");
        if(!file.exists()){
            file.mkdir();
        }

        try{
            File gpxfile = new File(file, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();

        }catch (Exception e){
            e.printStackTrace();

        }
    }

    private void escrbiriEnAlmacenamientoExterno(){

        File root = android.os.Environment.getExternalStorageDirectory();

        File dir = new File (root.getAbsolutePath() + "/");
        dir.mkdirs();
        File file = new File(dir, "contactos.csv");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println("Hi , How are you");
            pw.println("Hello");
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("APP", "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mostrarMenu(){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(HomeActivity.this);
        builder1.setMessage("¿Qué memoria utiliza para salvar los contactos?");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Interna",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getApplicationContext(), "Guardando en memoria interna", Toast.LENGTH_SHORT).show();
                        escribirEnAlmacenamientoInterno(HomeActivity.this, "salvado.csv", infoToString());
                    }
                });

        builder1.setNegativeButton(
                "Externa",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(HomeActivity.this, "Guardando en memoria externa", Toast.LENGTH_SHORT).show();
                        escrbiriEnAlmacenamientoExterno();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    public String infoToString(){
        String texto = "";
        texto += "NAME,PHONE_NUMBER\n";
        for(int i = 0; i < this.contactos.size();i++){
            texto += this.contactos.get(i) + "," + this.numeros.get(i) + "\n";
        }
        return texto;
    }

}

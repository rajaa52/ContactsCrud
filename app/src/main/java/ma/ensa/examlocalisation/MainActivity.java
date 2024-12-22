package ma.ensa.examlocalisation;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ma.ensa.examlocalisation.adapter.ContactAdapter;
import ma.ensa.examlocalisation.classes.Contact;
import ma.ensa.examlocalisation.classes.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 123;

    private ContactApi contactApi;
    private RecyclerView recyclerView;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList;
    private EditText etAddName, etAddNumber;
    private Button btnAddContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactApi = RetrofitClient.getClient().create(ContactApi.class);
        initViews();
        checkPermissions();
        loadContacts();
    }

    private void initViews() {
        contactList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        etAddName = findViewById(R.id.et_add_name);
        etAddNumber = findViewById(R.id.et_add_number);
        btnAddContact = findViewById(R.id.btn_add_contact);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactAdapter = new ContactAdapter(contactList);
        recyclerView.setAdapter(contactAdapter);

        btnAddContact.setOnClickListener(v -> {
            if (checkPermissions()) {
                addNewContact();
            }
        });
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_CONTACTS,
                            Manifest.permission.READ_CONTACTS
                    },
                    PERMISSIONS_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void addNewContact() {
        String name = etAddName.getText().toString().trim();
        String number = etAddNumber.getText().toString().trim();

        if (name.isEmpty() || number.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        Contact newContact = new Contact(name, number);

        // Add to phone contacts
        addToPhoneContacts(name, number);

        // Add to API
        contactApi.createContact(newContact).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contactList.add(response.body());
                    contactAdapter.notifyItemInserted(contactList.size() - 1);
                    clearInputFields();
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erreur lors de l'ajout sur le serveur", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearInputFields() {
        etAddName.setText("");
        etAddNumber.setText("");
    }

    private void addToPhoneContacts(String name, String phoneNumber) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Toast.makeText(this, "Contact ajouté avec succès", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors de l'ajout du contact local", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadContacts() {
        contactApi.getAllContacts().enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contactList.clear();
                    contactList.addAll(response.body());
                    contactAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erreur de chargement des contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions accordées", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions requises pour ajouter des contacts", Toast.LENGTH_LONG).show();
            }
        }
    }
}
package ma.ensa.examlocalisation;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
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
import ma.ensa.examlocalisation.classes.ShakeDetector;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 123;

    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;
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

        initSensorManager();
        initViews();
        initApi();
        checkPermissions();
        loadContacts();
    }

    private void initSensorManager() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        shakeDetector = new ShakeDetector();

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            shakeDetector.setOnShakeListener(() -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Secousse détectée!", Toast.LENGTH_SHORT).show();
                    handleShakeDetected();
                });
            });
        } else {
            Toast.makeText(this, "Pas d'accéléromètre trouvé sur l'appareil", Toast.LENGTH_LONG).show();
        }
    }

    private void initApi() {
        contactApi = RetrofitClient.getClient().create(ContactApi.class);
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
        String[] permissions = {
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.SEND_SMS
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void handleShakeDetected() {
        Log.d(TAG, "Shake detected, checking SMS permission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            sendEmergencyMessages();
        } else {
            Toast.makeText(this, "Permission d'envoi de SMS nécessaire", Toast.LENGTH_LONG).show();
        }
    }

    private void sendEmergencyMessages() {
        String message = "SOS ! Je suis en danger !";
        List<String> emergencyContacts = getEmergencyContacts();

        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "Aucun contact d'urgence trouvé!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Liste des contacts vide");
            return;
        }

        Log.d(TAG, "Envoi de messages à " + emergencyContacts.size() + " contacts");

        for (String contact : emergencyContacts) {
            sendSMS(contact, message);
        }
    }

    private List<String> getEmergencyContacts() {
        List<String> numbers = new ArrayList<>();
        for (Contact contact : contactList) {
            numbers.add(contact.getNumber());
        }
        return numbers;
    }

    private String formatToInternationalNumber(String phoneNumber) {
        // Supprimer tous les caractères non numériques sauf le +
        String cleanNumber = phoneNumber.replaceAll("[^+0-9]", "");

        // Si le numéro commence déjà par +, le retourner tel quel
        if (cleanNumber.startsWith("+")) {
            return cleanNumber;
        }

        // Si le numéro commence par 00, remplacer par +
        if (cleanNumber.startsWith("00")) {
            return "+" + cleanNumber.substring(2);
        }

        // Si le numéro commence par 0, supposer que c'est un numéro marocain
        if (cleanNumber.startsWith("0")) {
            return "+212" + cleanNumber.substring(1);
        }

        // Si le numéro n'a pas de préfixe, supposer que c'est un numéro marocain
        return "+212" + cleanNumber;
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            String formattedNumber = formatToInternationalNumber(phoneNumber);
            Log.d(TAG, "Envoi de SMS au numéro : " + formattedNumber);

            if (formattedNumber.isEmpty()) {
                Log.e(TAG, "Numéro de téléphone vide");
                return;
            }

            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);

            for (String part : parts) {
                smsManager.sendTextMessage(
                        formattedNumber,
                        null,
                        part,
                        null,
                        null
                );
            }

            Log.d(TAG, "SMS envoyé avec succès à " + formattedNumber);
            Toast.makeText(this, "SMS envoyé à " + formattedNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erreur d'envoi du SMS: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Échec de l'envoi du SMS: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void addNewContact() {
        String name = etAddName.getText().toString().trim();
        String number = etAddNumber.getText().toString().trim();

        if (name.isEmpty() || number.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedNumber = formatToInternationalNumber(number);
        Log.d(TAG, "Ajout du contact : " + name + " avec le numéro : " + formattedNumber);

        Contact newContact = new Contact(name, formattedNumber);
        addToPhoneContacts(name, formattedNumber);

        contactApi.createContact(newContact).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contactList.add(response.body());
                    contactAdapter.notifyItemInserted(contactList.size() - 1);
                    clearInputFields();
                    Log.d(TAG, "Contact ajouté avec succès");
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                Log.e(TAG, "Erreur lors de l'ajout du contact: " + t.getMessage());
                Toast.makeText(MainActivity.this,
                        "Erreur lors de l'ajout sur le serveur",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToPhoneContacts(String name, String phoneNumber) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Log.d(TAG, "Contact ajouté aux contacts du téléphone");
            Toast.makeText(this, "Contact ajouté avec succès", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'ajout du contact: " + e.getMessage());
            Toast.makeText(this, "Erreur lors de l'ajout du contact local",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadContacts() {
        Log.d(TAG, "Chargement des contacts");
        contactApi.getAllContacts().enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contactList.clear();
                    contactList.addAll(response.body());
                    contactAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Contacts chargés avec succès: " + contactList.size() + " contacts");
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                Log.e(TAG, "Erreur de chargement des contacts: " + t.getMessage());
                Toast.makeText(MainActivity.this,
                        "Erreur de chargement des contacts",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(shakeDetector,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "Accéléromètre enregistré");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeDetector);
        Log.d(TAG, "Accéléromètre désenregistré");
    }

    private void clearInputFields() {
        etAddName.setText("");
        etAddNumber.setText("");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permissions accordées");
                Toast.makeText(this, "Permissions accordées", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Permissions refusées");
                Toast.makeText(this,
                        "Les permissions sont nécessaires pour le fonctionnement de l'application",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
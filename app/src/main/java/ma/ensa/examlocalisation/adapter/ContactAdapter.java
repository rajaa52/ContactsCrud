package ma.ensa.examlocalisation.adapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import ma.ensa.examlocalisation.ContactApi;
import ma.ensa.examlocalisation.R;
import ma.ensa.examlocalisation.classes.Contact;
import ma.ensa.examlocalisation.classes.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
    private List<Contact> contactList;
    private Context context;
    private ContactApi contactApi;

    public ContactAdapter(List<Contact> contactList) {
        this.contactList = contactList;
        this.contactApi = RetrofitClient.getClient().create(ContactApi.class);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.view_contacts_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.name.setText(contact.getName());
        holder.number.setText(contact.getNumber());
        holder.initials.setText(getInitials(contact.getName()));

        holder.itemView.setOnClickListener(v -> {
            showEditDialog(contact, position);
        });

        holder.copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("phone number", contact.getNumber());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Numéro copié!", Toast.LENGTH_SHORT).show();
        });

        holder.delete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Supprimer le contact")
                    .setMessage("Voulez-vous vraiment supprimer " + contact.getName() + "?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        deleteContact(contact, position);
                    })
                    .setNegativeButton("Non", null)
                    .show();
        });
    }

    private void deleteContact(Contact contact, int position) {
        // Appel à l'API pour supprimer le contact
        contactApi.deleteContact(contact.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    contactList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, contactList.size());
                    Toast.makeText(context, "Contact supprimé avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "Erreur réseau lors de la suppression", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog(Contact contact, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.view_add_new_contact2, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_add_name);
        TextInputEditText etNumber = dialogView.findViewById(R.id.et_add_number);

        etName.setText(contact.getName());
        etNumber.setText(contact.getNumber());

        new AlertDialog.Builder(context)
                .setTitle("Modifier le contact")
                .setView(dialogView)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newNumber = etNumber.getText().toString().trim();

                    if (!newName.isEmpty() && !newNumber.isEmpty()) {
                        Contact updatedContact = new Contact(newName, newNumber);
                        updatedContact.setId(contact.getId()); // Important: conserver l'ID du contact
                        updateContact(updatedContact, position);
                    } else {
                        Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateContact(Contact updatedContact, int position) {
        contactApi.updateContact(updatedContact.getId(), updatedContact).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contactList.set(position, response.body());
                    notifyItemChanged(position);
                    Toast.makeText(context, "Contact modifié avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                Toast.makeText(context, "Erreur réseau lors de la modification", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "";
        return fullName.substring(0, 1).toUpperCase();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, number, initials;
        public ImageView copy, delete;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.name);
            number = view.findViewById(R.id.number);
            initials = view.findViewById(R.id.initials);
            copy = view.findViewById(R.id.copy);
            delete = view.findViewById(R.id.delete);
        }
    }
}
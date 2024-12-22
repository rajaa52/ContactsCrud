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

import ma.ensa.examlocalisation.R;
import ma.ensa.examlocalisation.classes.Contact;
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
    private List<Contact> contactList;
    private Context context;

    public ContactAdapter(List<Contact> contactList) {
        this.contactList = contactList;
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

        // Clic sur l'item entier pour modifier
        holder.itemView.setOnClickListener(v -> {
            showEditDialog(contact, position);
        });

        // Clic sur copy (inchangé)
        holder.copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("phone number", contact.getNumber());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Numéro copié!", Toast.LENGTH_SHORT).show();
        });

        // Clic sur delete (inchangé)
        holder.delete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Supprimer le contact")
                    .setMessage("Voulez-vous vraiment supprimer " + contact.getName() + "?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        contactList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, contactList.size());
                    })
                    .setNegativeButton("Non", null)
                    .show();
        });
    }

    private void showEditDialog(Contact contact, int position) {
        // Créer la vue pour le dialogue
        View dialogView = LayoutInflater.from(context).inflate(R.layout.view_add_new_contact2, null);

        // Récupérer les EditText
        TextInputEditText etName = dialogView.findViewById(R.id.et_add_name);
        TextInputEditText etNumber = dialogView.findViewById(R.id.et_add_number);

        // Pré-remplir avec les valeurs actuelles
        etName.setText(contact.getName());
        etNumber.setText(contact.getNumber());

        // Créer et afficher le dialogue
        new AlertDialog.Builder(context)
                .setTitle("Modifier le contact")
                .setView(dialogView)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newNumber = etNumber.getText().toString().trim();

                    if (!newName.isEmpty() && !newNumber.isEmpty()) {
                        // Mettre à jour le contact
                        Contact updatedContact = new Contact(newName, newNumber);
                        contactList.set(position, updatedContact);
                        notifyItemChanged(position);
                        Toast.makeText(context, "Contact modifié", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
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
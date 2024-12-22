package ma.ensa.examlocalisation;

import retrofit2.Call;  // Correction de l'import
import java.util.List;

import ma.ensa.examlocalisation.classes.Contact;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ContactApi {
    @GET("api/contacts")
    Call<List<Contact>> getAllContacts();

    @POST("api/contacts")
    Call<Contact> createContact(@Body Contact contact);

    @PUT("api/contacts/{id}")
    Call<Contact> updateContact(@Path("id") Long id, @Body Contact contact);

    @DELETE("api/contacts/{id}")
    Call<Void> deleteContact(@Path("id") Long id);
}
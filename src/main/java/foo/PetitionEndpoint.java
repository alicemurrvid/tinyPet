package foo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.print.PrintException;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Transaction;

@Api(name = "myApi",
     version = "v1",
     audiences = "1048646993657-jhplb72bvekc44tcf8au03vors3c99jv.apps.googleusercontent.com",
  	 clientIds = "1048646993657-jhplb72bvekc44tcf8au03vors3c99jv.apps.googleusercontent.com",
     namespace =
     @ApiNamespace(
		   ownerDomain = "helloworld.example.com",
		   ownerName = "helloworld.example.com",
		   packagePath = "")
     )

public class PetitionEndpoint {

    @ApiMethod(name = "addPetition", httpMethod=HttpMethod.POST)
    public Entity addPetition(User owner, PostMessage pm) throws UnauthorizedException {
		if (owner == null) {
				throw new UnauthorizedException("Invalid credentials");
			}
		Date date = new Date(); 
        Entity e = new Entity("Petition", Long.MAX_VALUE-(date.getTime()) + "/" + owner.getEmail());
        e.setProperty("titre",pm.titre);
		e.setProperty("body", pm.body);
        e.setProperty("owner", owner.getEmail());
        e.setProperty("date", date);
        e.setProperty("tags", pm.tag);
				
		//cree des signataire
		ArrayList<String> fset = new ArrayList<String>();
		fset.add(owner.getEmail());
		e.setProperty("signatories",fset);
		e.setProperty("nbSignatory",1);
				
				
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);
		txn.commit();

        
		return e;
    }

    //Recuperer les petitions signees par l'utilisateur
    @ApiMethod(name = "getMyPetitionsSign", httpMethod = HttpMethod.GET)
	public CollectionResponse<Entity> getMyPetitionsSign(@Named("owner") User user) throws UnauthorizedException {
		if (user == null) {
				throw new UnauthorizedException("Invalid credentials");
			}
		Query q = new Query("Petition").setFilter(new FilterPredicate("signatories", FilterOperator.EQUAL, user.getEmail()));
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        PreparedQuery pq = datastore.prepare(q);
        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(5);

        QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);

		return CollectionResponse.<Entity>builder().setItems(results).build();
    }
    
    //Recupere les 10 petitions avec le plus de signatures
    @ApiMethod(name="entity", httpMethod=HttpMethod.GET)
	public List<Entity> entity() throws Exception {
        
        Query q = new Query("Petition");
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10));

        if(result.isEmpty()){
            throw new Exception("Get request empty");
        } else {
            return result;
        }
    }  

    //Recupere les petitions en fonction du tag cherché
    @ApiMethod(name = "getPetitionByTag", httpMethod = HttpMethod.GET)
	public List<Entity> getPetitionByTag(@Named("tags") String tag) {
		Query q = new Query("Petition").setFilter(new FilterPredicate("tags", FilterOperator.EQUAL, tag));
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        
        PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));
		return result;
    }  

    //Signer une petition
    @ApiMethod(name = "signPetition", httpMethod = HttpMethod.POST)
    public Entity signPetition(User user, PostMessage pm) throws UnauthorizedException {
        if (user == null) {
				throw new UnauthorizedException("Invalid credentials");
            }
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        
        Entity ent = new Entity("Petition");
        
        Transaction txn=datastore.beginTransaction();

        try {
				ent = datastore.get(KeyFactory.createKey("Petition", pm.key));
				int nb =  Integer.parseInt(ent.getProperty("nbSignatory").toString());
			    ArrayList<String> signatories = (ArrayList<String>) ent.getProperty("signatories");
			    
			    if(!signatories.contains(user.getEmail())) {
			    	signatories.add(user.getEmail());
			    	ent.setProperty("signatories", signatories);
				    ent.setProperty("nbSignatory", nb + 1 );
				   }
				datastore.put(ent);
				txn.commit();
			} catch (EntityNotFoundException e) {
					e.printStackTrace();
				}
			  finally {
				if (txn.isActive()) {
				    txn.rollback();
				  }
        }
        return ent;
    }
}
package foo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Date;
import java.util.Random;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

import com.google.appengine.api.datastore.Entity;

@WebServlet(name = "PetServlet", urlPatterns = { "/petition" })
public class PetitionServlet extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");

        Random r = new Random();
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		// Create petitions
		for (int i = 1; i < 501; i++) {
            Date date = new Date();
            Entity e  = new Entity("Petition", Long.MAX_VALUE-(new Date()).getTime()+"petition" + i);
            e.setProperty("body", "ceci est la petitition" + i);
            e.setProperty("owner", "user" + i);
            e.setProperty("date", date);

            // Create signatories
            HashSet<String> s = new HashSet<>();
            int k = r.nextInt(1000);
            for (int j = 0; j < k; j++){
                s.add("user" + j);
            }
            e.setProperty("signatories", s);
            e.setProperty("nbSignatory", s.size());

            //Create tags
            e.setProperty("tags", "marredezoom");
            datastore.put(e);
			response.getWriter().print("<li> created post:" + e.getKey() + "<br>");
		}
	}
}
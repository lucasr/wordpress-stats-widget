package org.lucasr.wordpress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class Stats {
    private final HttpClient client;
    private final String apiKey;
    private final String blogId;

    public Stats(String apiKey, String blogId) {
        client = new DefaultHttpClient();

        this.apiKey = apiKey;
        this.blogId = blogId;
    }

    private String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();

        return dateFormat.format(now);
    }

    private HttpRequest createViewCountsRequest() throws URISyntaxException {
        Log.v("WordpressStats", "creating request");

        HttpGet request = new HttpGet();

        request.setURI(new URI("http://stats.wordpress.com/csv.php?" +
                               "api_key=" + apiKey + "&" +
                               "blog_id=" + blogId + "&" +
                               "format=csv&" +
                               "days=6&" +
                               "end=" + getFormattedDate()));

        return request;
    }

    public Vector<Integer> getViewCounts() throws NetworkException {
        BufferedReader in = null;
        Vector<Integer> viewCounts = new Vector<Integer>();

        Log.v("WordpressStats", "getViewCounts()");

        try {
            HttpRequest request = createViewCountsRequest();
            HttpResponse response = client.execute((HttpUriRequest) request);
            InputStream is = response.getEntity().getContent();

            in = new BufferedReader(new InputStreamReader(is));

            /* The returned value is a simple 2-line csv with a
             * "views" header on first line and its corresponding
             * value on the second line. We simply ignore the first
             * line and parse the value as integer.
             */
            String firstLine = in.readLine();

            if (firstLine.equals("date,views")) {
                String line;

                Log.v("WordpressStats", "has a few view counts");

                while ((line = in.readLine()) != null) {
                    String[] data = line.split(",");
                    viewCounts.add(Integer.valueOf(data[1]));
                }
            } else if (firstLine.equals("Error: zero rows returned.")) {
                /* Probably means the request returned zero rows
                 * In which case the first line is the string
                 * "Error: zero rows returned."
                 */
                Log.v("WordpressStats", "has no view counts");
            }

            in.close();
        } catch (IOException ioe) {
            Log.w("WordpressStats: Network error fetching number of views", ioe);
            throw new NetworkException();
        } catch (Exception e) {
            Log.w("WordpressStats: Error fetching number of views", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.w("WordpressStats: Error closing input stream", e);
                }
            }
        }

        Log.v("WordpressStats", "done (" + viewCounts + ")");

        return viewCounts;
    }
}
package org.lucasr.wordpress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	private HttpRequest createViewCountRequest() throws URISyntaxException {
        Log.v("WordpressStats", "creating request");

        HttpGet request = new HttpGet();

        request.setURI(new URI("http://stats.wordpress.com/csv.php?" +
        					   "api_key=" + apiKey + "&" +
        					   "blog_id=" + blogId + "&" +
        					   "summarize=1&" +
        					   "format=csv&" +
        					   "days=1&" +
        					   "end=" + getFormattedDate()));

        return request;
	}

	public Integer getViewCount() throws NetworkException {
        BufferedReader in = null;
        Integer viewCount = -1;

        Log.v("WordpressStats", "getViewCount()");

        try {
        	HttpRequest request = createViewCountRequest();
            HttpResponse response = client.execute((HttpUriRequest) request);
            InputStream is = response.getEntity().getContent();

            in = new BufferedReader(new InputStreamReader(is));

            /* The returned value is a simple 2-line csv with a
             * "views" header on first line and its corresponding
             * value on the second line. We simply ignore the first
             * line and parse the value as integer.
             */
            in.readLine();

            viewCount = Integer.valueOf(in.readLine());

            in.close();
        } catch (IOException ioe) {
        	throw new NetworkException();
        } catch (Exception e) {
        	Log.w("WordpressStats: Error fetching number of views", e);
		} finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                	Log.w("Error closing input stream", e);
                }
            }
        }

		Log.v("WordpressStats", "done (" + viewCount + ")");

		return viewCount;
	}
}
package org.lucasr.wordpress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class BlogInfo {
    private final DefaultHttpClient client;

    public BlogInfo(String username, String password) {
        client = new DefaultHttpClient();

        setupCredentials(username, password);
        setupSSLScheme();
    }

    private void setupCredentials(String username, String password) {
        Log.v("WordpressBlogInfo", "setting up credentials");

        UsernamePasswordCredentials creds =
            new UsernamePasswordCredentials(username, password);

        BasicCredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, creds);

        client.setCredentialsProvider(cp);
    }

    private void setupSSLScheme() {
        Log.v("WordpressBlogInfo", "setting up SSl scheme");

        try {
            TrustAllSSLSocketFactory sf = new TrustAllSSLSocketFactory();
            Scheme scheme = new Scheme("https", sf, 443);

            client.getConnectionManager().getSchemeRegistry().register(scheme);
        } catch (Exception e) {
            Log.w("WordpressBlogInfo: Error setting up SSL for wordpress blog info", e);
        }
    }

    private HttpRequest createBlogInfoRequest() throws URISyntaxException {
        Log.v("WordpressBlogInfo", "creating request");

        HttpPost request =
            new HttpPost("https://public-api.wordpress.com/getuserblogs.php?f=json");

        request.addHeader("charset", "UTF-8");

        HttpParams httpParams = request.getParams();
        HttpProtocolParams.setUseExpectContinue(httpParams, false);

        return request;
    }

    public HashMap<String, String> getInfo() throws NoAuthException, NetworkException {
        BufferedReader in = null;
        HashMap<String, String> blogInfo = new HashMap<String, String>();

        Log.v("WordpressBlogInfo", "getBlogInfo()");

        try {
            HttpRequest request = createBlogInfoRequest();
            HttpResponse response = client.execute((HttpUriRequest) request);

            if (response.getStatusLine().getStatusCode() == 401) {
                throw new NoAuthException();
            }

            InputStream is = response.getEntity().getContent();

            in = new BufferedReader(new InputStreamReader(is));

            StringBuffer sb = new StringBuffer("");
            String line;

            while ((line = in.readLine()) != null) {
                sb.append(line);
            }

            JSONObject json = (JSONObject) new JSONTokener(sb.toString()).nextValue();
            JSONObject userInfo = json.getJSONObject("userinfo");

            String apiKey = userInfo.getString("apikey");
            if (apiKey != null) {
                blogInfo.put("apiKey", apiKey);
            }

            JSONArray blogs = userInfo.getJSONArray("blog");
            if (blogs.length() > 0) {
                JSONObject blog = (JSONObject) blogs.get(0);
                blogInfo.put("blogId", blog.getString("id"));
                blogInfo.put("blogHost", blog.getString("url"));
            }

            in.close();
        } catch (IOException ioe) {
            throw new NetworkException();
        } catch (NoAuthException nae) {
            throw nae;
        } catch (Exception e) {
            Log.w("WordpressBlogInfo: Error fetching blog info", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.w("Error closing input stream", e);
                }
            }
        }

        Log.v("WordpressBlogInfo", "done");

        return blogInfo;
    }
}
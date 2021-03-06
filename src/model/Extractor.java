package model;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.json.JSONObject;
import org.json.JSONArray;
import view.Controller;

import java.util.Random;




/**
 * Parses through a json file on a certain subreddit, and grab direct links to images.
 */
public class Extractor {

    private List<String> subreddits; // subreddits to scan through
    private List<String> imageLinks = new ArrayList<>(); // links to all images found
    private Model _m;
    private Controller _c;


    public Extractor(Model m, Controller c){
        _m = m;
        _c = c;
        this.load();
    }

    public boolean load(){
        imageLinks.clear();
        subreddits = Model.getSubreddits();
        if(!subreddits.isEmpty()){
            imageLinks.clear();
            imageLinks = getImageLinks();
        }
        if(imageLinks.isEmpty()){
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "No images found on those subreddits.");
                alert.showAndWait();
            });
            return false;
        }
        return true;
    }

    //Load must be called first before this command
    public File get() {
        if(imageLinks.isEmpty()){
            load();
        }

        //returns a single link from the array of pulled wallpapers, at random.
        String imageURL = imageLinks.remove(new Random().nextInt(imageLinks.size()));
        String filename = imageURL.substring(imageURL.lastIndexOf("/")).substring(1);
        File file = null;


        try {
            URL url = new URL(imageURL);

            if (System.getProperty("os.name").startsWith("Windows")) {
                File dir = new File(_m.getSystemApplicationFolder());
                if(!dir.exists()){
                    dir.mkdir();
                }
                filename = filename.substring(0, filename.indexOf("."));
                file = new File(_m.getSystemApplicationFolder()+filename+".bmp");

            }else if (System.getProperty("os.name").startsWith("Mac")){
                File dir = new File(_m.getSystemApplicationFolder());
                if(!dir.exists()){
                    dir.mkdir();
                }
                file = new File(_m.getSystemApplicationFolder()+filename);
            }

            file.deleteOnExit();
            Files.copy(url.openStream(), file.toPath());
        } catch (Exception e) {
        }

        file.deleteOnExit();
        return file;
    }

    public List<String> getImageLinks() {
        for(String subreddit : subreddits) {
            _c.updateProgressBar((1.0/subreddits.size())/9.0);
            String firstPart = "https://reddit.com/r/";
            String fullLink = firstPart + subreddit + "/hot.json";


            // now perform a scan for this subreddit
            String jsonString = readJSONFromURL(fullLink);
            JSONObject jason = new JSONObject(jsonString);
            JSONArray allPosts = jason.getJSONObject("data").getJSONArray("children");

            for(int i = 0; i < allPosts.length(); i++) {
                JSONObject post = allPosts.getJSONObject(i).getJSONObject("data");
                boolean isNSFW = post.getBoolean("over_18");
                String link = post.getString("url");
                if (link.contains(".jpg") | link.contains(".jpeg") | link.contains(".png")){
                    if (isNSFW == true && _m.isNSFWAllowed() == false){
                        //do nothing lol
                    }else{
                        int imageWidth = post.getJSONObject("preview").getJSONArray("images").getJSONObject(0).getJSONObject("source").getInt("width");
                        int imageHeight = post.getJSONObject("preview").getJSONArray("images").getJSONObject(0).getJSONObject("source").getInt("height");
                        if(compareRes(imageWidth,imageHeight)){
                            imageLinks.add(link);
                        }
                    }
                }
            }
        }

        return imageLinks;
    }

// Method that checks the resolution ...

    //Compares height ... width to screen size
    private boolean compareRes(int imageWidth, int imageHeight){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screen_width = screenSize.getWidth();
        double screen_height = screenSize.getHeight();
        if ( screen_height > imageHeight || screen_width > imageWidth || imageHeight>imageWidth){


            return false;
        }else {
            return true;
        }

    }



    private String readJSONFromURL(String urlString) {
        StringBuilder sb = new StringBuilder();
        URLConnection uc = null;
        InputStreamReader in = null;

        try {
            URL url = new URL(urlString);
            uc = url.openConnection();

            //Thread.sleep(2000); // to comply with reddit's rate-limiting rules. Also i 'removed' this to fix #64 and increased the speed... #bugfixes
            uc.setRequestProperty("User-Agent", "Wallplayper"); // ^ same here. Please dont change.

            if(uc != null)
                uc.setReadTimeout(60 * 1000);

            if(uc != null && uc.getInputStream() != null) {
                in = new InputStreamReader(uc.getInputStream(),Charset.defaultCharset());
                BufferedReader br = new BufferedReader(in);
                if(br != null) {
                    int cp;
                    while((cp = br.read()) != -1) {
                        sb.append((char) cp);
                    }
                    br.close();
                }
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}
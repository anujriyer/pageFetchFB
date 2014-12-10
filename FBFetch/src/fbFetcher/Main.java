package fbFetcher;

import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;
import java.sql.Timestamp;
import java.util.Date;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.restfb.types.Post;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.exception.FacebookNetworkException;
import com.restfb.exception.FacebookOAuthException;

import fbFetcher.DBConnect;

public class Main {
	private static final String MY_APP_ID = "759223277481533";
	private static final String MY_APP_SECRET = "c9014aa2bf9ab613f63ba9d899ec3eca";
	
	public static String ts;
	public static int s_fires = 0, b_fires = 0;
	
	public static void main(String args[]) {
		try{
			//Set timestamp for program initialization
			Date date= new java.util.Date();
			ts = new Timestamp(date.getTime()).toString();
				
			//Initialize Access Token
			DefaultFacebookClient fbc1 = new DefaultFacebookClient();
			AccessToken accessToken = fbc1.obtainAppAccessToken(MY_APP_ID, MY_APP_SECRET);
			String MY_ACCESS_TOKEN = accessToken.getAccessToken();
			
			//Start Accessing Facebook
			FacebookClient facebookClient = new DefaultFacebookClient(MY_ACCESS_TOKEN, MY_APP_SECRET);
			//Enter fb page that u need to get data for:
			String product = "unilever/posts";
	
			//Begin fetching operations here:
			Connection<Post> postCon = facebookClient.fetchConnection(product, Post.class, Parameter.with("limit", 50));
			s_fires++;
			List<Post> posts = postCon.getData();
	        long next1=-1;
	        System.out.println("Fetch for " + product + " started!");
	        
	        do {
	        	insertPostDetails(posts);
	        	insertPostShares(facebookClient, posts);
	        	insertPostLikes(facebookClient, posts);
	        	insertPostComments(facebookClient, posts);
	        	
		        
	        	//Check for the next page of posts
		        if (postCon.getNextPageUrl() != null){
		        	next1 = Long.parseLong(postCon.getNextPageUrl().substring(6+postCon.getNextPageUrl().indexOf("until")));
		        	postCon = facebookClient.fetchConnection(product, Post.class, Parameter.with("limit",50), Parameter.with("until", next1));
		        	s_fires++;
		        	posts = postCon.getData();
		        }
	        }while(postCon.getNextPageUrl() != null);	        
	        System.out.println("Done! Made "+ b_fires +" batch api calls and " + s_fires + " single api calls to Facebook!");
		}
		catch (FacebookNetworkException e) {
			System.out.println("Network Error with message: "+ e.getMessage() +"\n\n Please restart program");
		}
		catch (FacebookOAuthException e) {
			if (e.getErrorCode() == 2)
				System.out.println("FB error!! Too many calls made to facebook. Please restart program :(");
			else
				System.out.println("Unknown oauth error - " + e.getErrorMessage());
		}
	}
	
	
	private static void insertPostDetails(List<Post> posts) {
		DBConnect dbc = new DBConnect();
		for (Post post : posts) {
			dbc.insertPost(post);
		}
		dbc.close();
	}

	
	private static void insertPostShares(FacebookClient facebookClient, List<Post> posts) {
		DBConnect dbc = new DBConnect();
		List<BatchRequest> sharesRequests = new ArrayList<>();
		
		//First Lets See each post how much shares were made
    	//Now create the share requests for each post
        for (Post post : posts) {
            BatchRequest request = new BatchRequestBuilder(post.getId()).parameters(Parameter.with("fields", "shares")).build();
            sharesRequests.add(request);
        }
        
        //Execute share requests for each post
        List<BatchResponse> batchResponses = facebookClient.executeBatch(sharesRequests.toArray(new BatchRequest[0]));
        b_fires++;
        for (int i = 0; i < batchResponses.size(); i++) {
        	//Get data in json format
            String json = batchResponses.get(i).getBody();
            JsonObject jsonObject = new JsonObject(json);
            
            //Total shares:
            if (jsonObject.has("shares")) {
            	int count = jsonObject.getJsonObject("shares").getInt("count");            
	            //-----------------------------------------------------------------------------------------
	            //TOTAL SHARES HERE
	            dbc.insertPostCount(posts.get(i).getId(), ts, count, -1, -1, 1);
	            //-----------------------------------------------------------------------------------------
            } //else {
            	//no shares!
            //}
            //Users who shared the post is currently unavailable feature :(
            //OR : http://stackoverflow.com/questions/7748037/list-of-people-who-shared-on-facebook : figure out the link logic!
        }
        dbc.close();
	}


	private static void insertPostLikes(FacebookClient facebookClient, List<Post> posts) {
		DBConnect dbc = new DBConnect();
		List<BatchRequest> likeRequests = new ArrayList<>();
		
		//First Lets See each post likes and who all gave likes
    	//Now create the like requests for each post
        for (Post post : posts) {
            BatchRequest request = new BatchRequestBuilder(post.getId() + "/likes").parameters
            		(Parameter.with("summary", true), 
            		 Parameter.with("limit", 1000)).build();
            likeRequests.add(request);
        }
        
        //Execute like requests for each post
        List<BatchResponse> batchResponses = facebookClient.executeBatch(likeRequests.toArray(new BatchRequest[0]));
        b_fires++;
        for (int i = 0; i < batchResponses.size(); i++) {
        	//Get data in json format
            String json = batchResponses.get(i).getBody();
            JsonObject jsonObject = new JsonObject(json);
            
            //Total likes:
            if (!jsonObject.has("summary")) {
            	System.out.println(jsonObject);
            	System.exit(0);
            }
            int count = jsonObject.getJsonObject("summary").getInt("total_count");
            
            //-----------------------------------------------------------------------------------------
            //TOTAL LIKES HERE
            dbc.insertPostCount(posts.get(i).getId(), ts, -1, count, -1, 2);
            //-----------------------------------------------------------------------------------------
            
            if (count !=0) {
	            //Array of users how gave likes:
	            for (int j =0; j < jsonObject.getJsonArray("data").length(); j++) {
	            	//-----------------------------------------------------------------------------------------
	            	//USER DETAILS OF LIKE HERE
	            	dbc.insertFan(jsonObject.getJsonArray("data").getJsonObject(j), "like");
	            	dbc.insertPostLike(posts.get(i).getId(), jsonObject.getJsonArray("data").getJsonObject(j), ts);
	            	//-----------------------------------------------------------------------------------------
	            }
	            
	            //See if there is another list of likes to acquire, then loop
	            if(jsonObject.getJsonObject("paging").has("next")) {            	
	            	getLikes(jsonObject.getJsonObject("paging").getString("next"), posts.get(i).getId(), facebookClient, dbc, 1);            	
	            }
            }            
        }
        dbc.close();
	}
	
	
	private static void getLikes(String nextURL, String id, FacebookClient facebookClient,	DBConnect dbc, int flag) {
		Connection<JsonObject> likeposts;
    	do {
    		try {
    			likeposts = facebookClient.fetchConnectionPage(nextURL, JsonObject.class);
    			s_fires++;
    			//Extended array of users who gave a like
        		for (int k = 0; k < likeposts.getData().size(); k++) {
        			//-----------------------------------------------------------------------------------------
        			//USER DETAILS OF LIKES HERE AS WELL
        			dbc.insertFan(likeposts.getData().get(k), "like");
        			switch (flag) {
	        			case 1: dbc.insertPostLike(id, likeposts.getData().get(k), ts);
	        					break;
	        			case 2: dbc.insertCommentLike(id, likeposts.getData().get(k), ts); 
	        					break;
	        			case 3: dbc.insertReplyLike(id, likeposts.getData().get(k), ts);
	        					break;
        			}
	            	
        			//-----------------------------------------------------------------------------------------
        		}
        		nextURL = likeposts.getNextPageUrl();
    		}
    		catch (FacebookOAuthException e){
    			if (e.getErrorCode() == 2)
    				System.out.println("FB error!! Too many calls, so pls wait little longer :(");
    			try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					System.out.println("Process Interupted!!");
					e1.printStackTrace();
				}
    		}	            		
    	} while(nextURL != null);
	}


	private static void insertPostComments(FacebookClient facebookClient, List<Post> posts) {
		DBConnect dbc = new DBConnect();	
		List<BatchRequest> commentRequests = new ArrayList<>();
		
		//First Lets See each post comments
    	//Now create the comment requests for each post
        for (Post post : posts) {
            BatchRequest request = new BatchRequestBuilder(post.getId() + "/comments").parameters
            		(Parameter.with("summary", true), 
            		 Parameter.with("limit", 1000), 
            		 Parameter.with("fields", "id,from,message,created_time,like_count,likes,comments")).build();
            commentRequests.add(request);
        }
        
        //Execute comment requests for each post
        List<BatchResponse> batchResponses = facebookClient.executeBatch(commentRequests.toArray(new BatchRequest[0]));
        b_fires++;
        for (int i = 0; i < batchResponses.size(); i++) {
        	//Get data in json format
            String json = batchResponses.get(i).getBody();
            JsonObject jsonObject = new JsonObject(json);
            
           //Check if there are any comments available:
            if (jsonObject.has("summary")) {
            	 //Total comments:
            	int count = jsonObject.getJsonObject("summary").getInt("total_count");
            
	            //-----------------------------------------------------------------------------------------
	            //TOTAL COMMENTS HERE:
            	dbc.insertPostCount(posts.get(i).getId(), ts, -1, -1, count, 3);
	            //-----------------------------------------------------------------------------------------
	            
            	//Array of comments data:
	            if (count > 0) {
	            	for (int j =0; j < jsonObject.getJsonArray("data").length(); j++) {
	            		processComments(jsonObject.getJsonArray("data").getJsonObject(j), posts.get(i).getId(), facebookClient, dbc);
	            	}
	            	
		            //See if there is another list of comments to acquire, then loop
		            if(jsonObject.has("paging") && jsonObject.getJsonObject("paging").has("next")) {
		            	String nextURL2 = jsonObject.getJsonObject("paging").getString("next");
		            	Connection<JsonObject> commentsnext = null;
		            	do {
		            		try {
			            		commentsnext = facebookClient.fetchConnectionPage(nextURL2, JsonObject.class);
			            		s_fires++;
			            		//Extended array of users who have written comments
			            		for (int k = 0; k < commentsnext.getData().size(); k++){
			            			processComments(commentsnext.getData().get(k), posts.get(i).getId(), facebookClient, dbc);
			            		}
			            		nextURL2 = commentsnext.getNextPageUrl();
		            		} catch (FacebookOAuthException e){
		            			if (e.getErrorCode() == 2)
		            				System.out.println("FB error!! Too many calls, so pls wait little longer :(");
		            			try {
		        					Thread.sleep(5000);
		        				} catch (InterruptedException e1) {
		        					System.out.println("Process Interupted!!");
		        					e1.printStackTrace();
		        				}
		            		}
		            	} while(commentsnext.getNextPageUrl() != null);
		            }
	            }
            }
        }
        dbc.close();
	}


	private static void processComments(JsonObject jO, String postId, FacebookClient facebookClient, DBConnect dbc) {
		String c_id = jO.getString("id");
    	
    	//-----------------------------------------------------------------------------------------
    	//COMMENT DATA HERE i.e. id, user details, comment text and creation time. e.g.:
    	dbc.insertFan(jO.getJsonObject("from"), "comment");
    	dbc.insertCommentData(postId, jO);
    	//-----------------------------------------------------------------------------------------
    	
    	//---------------------------------------------------------------------------------------
    	//Total Comment Likes count here
    	if (jO.getInt("like_count") > 0) {
    		dbc.insertCommentCount(c_id, ts, jO.getInt("like_count"), -1, 1);
    	}
    	//-----------------------------------------------------------------------------------------
    		            	
    	//get comment likes details
    	int lcount2 = 0;
    	if (jO.has("likes")) {
    		lcount2 = jO.getJsonObject("likes").getJsonArray("data").length();
    	}		       
    	
    	if (jO.getInt("like_count") > 0) {
    		if  (lcount2 != jO.getInt("like_count")) {
    			System.out.println("Likes data incomplete for comment id "+ c_id +"!!");
    		} 
        	for (int k = 0; k < lcount2; k++) {
        		//-----------------------------------------------------------------------------------------
                //COMMENT LIKES HERE:
        		dbc.insertFan(jO.getJsonObject("likes").getJsonArray("data").getJsonObject(k), "comment_l");
        		dbc.insertCommentLike(c_id, jO.getJsonObject("likes").getJsonArray("data").getJsonObject(k), ts);
        		//------------------------------------------------------------------------------------------
                if (jO.getJsonObject("likes").getJsonObject("paging").has("next")) {
                	getLikes(jO.getJsonObject("likes").getJsonObject("paging").getString("next"), c_id, facebookClient, dbc, 2);
                }
        	}
        	
    	}
    	
    	//For replies:
    	if (jO.has("comments")) { 
        	int reps = jO.getJsonObject("comments").getJsonArray("data").length();
        	
        	if (reps > 0) {
            	for (int k = 0; k < reps; k++) {
            		//GET REPLY DATA --                	
            		processReplies(c_id, jO.getJsonObject("comments").getJsonArray("data").getJsonObject(k), dbc, facebookClient);
            	}
            	
            	if (jO.getJsonObject("comments").getJsonObject("paging").has("next")) {
                	Connection<JsonObject> repliesposts;
                	String nextURL = jO.getJsonObject("comments").getJsonObject("paging").getString("next");
                	do {
                		try {
                			repliesposts = facebookClient.fetchConnectionPage(nextURL, JsonObject.class);
                			s_fires++;
                			//Extended array of replies to comments
                			for (int l = 0; l < repliesposts.getData().size(); l++) {
                				//-----------------------------------------------------------------------------------------
                				//REPLIES DATA HERE AS WELL
                				processReplies(c_id, repliesposts.getData().get(l), dbc, facebookClient);
                				//-----------------------------------------------------------------------------------------
                			}
                			reps += repliesposts.getData().size();
                			nextURL = repliesposts.getNextPageUrl();
                	    }
                	    catch (FacebookOAuthException e) {				                    	    	
                	    	if (e.getErrorCode() == 2)
                	    		System.out.println("FB error!! Too many calls, so pls wait little longer :(");
                	        try {
                	        	Thread.sleep(5000);
                	        } catch (InterruptedException e1) {
                	        	System.out.println("Process Interupted!!");
                	        	e1.printStackTrace();
                	        }
                	    }
                	} while(nextURL != null);				                    	
                }
        	}
        	//------------------------------------------------------
        	//No. of replies to comments
        	dbc.insertCommentCount(c_id, ts, -1, reps, 2);
        	//------------------------------------------------------        	
    	} 
	}


	private static void processReplies(String c_id, JsonObject jsonObject, DBConnect dbc, FacebookClient fbc) {
		//-----------------------------------------------------------------------------------------
        //COMMENT REPLIES DATA HERE:
		dbc.insertFan(jsonObject.getJsonObject("from"), "reply");
		dbc.insertReplyData(c_id, jsonObject);
        //-----------------------------------------------------------------------------------------
		
		//GET REPLY LIKES DATA:
		int l_count = jsonObject.getInt("like_count");
		if(l_count > 0) {
			String rep_id = jsonObject.getString("id");
			dbc.insertReplyCount(rep_id, ts, l_count);
						
			JsonObject rep_like_data = fbc.fetchObject(rep_id + "/likes", JsonObject.class, Parameter.with("limit", 1000));
			s_fires++;
			for (int i = 0; i < rep_like_data.getJsonArray("data").length(); i++) {
				dbc.insertFan(rep_like_data.getJsonArray("data").getJsonObject(i), "reply_l");
				dbc.insertReplyLike(rep_id, rep_like_data.getJsonArray("data").getJsonObject(i), ts);			
			}
			if (rep_like_data.has("paging")) {
				if (rep_like_data.getJsonObject("paging").has("next")) {
					getLikes(rep_like_data.getJsonObject("likes").getJsonObject("paging").getString("next"), rep_id, fbc, dbc, 3);
				}
			}
		}		
	}
}

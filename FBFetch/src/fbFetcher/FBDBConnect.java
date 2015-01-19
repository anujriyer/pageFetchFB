package fbFetcher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.restfb.json.JsonObject;
import com.restfb.types.Post;

public class FBDBConnect {
	private Connection connection = null;
	private final String dbUserName = "postgres";
	private final String dbPassword = "anuj";
	
	public FBDBConnect(){
		try { 
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/mydb", dbUserName, dbPassword);
			if (connection == null) {				
				System.out.println("Failed to make connection to database! Contact Database admin!");
				System.exit(0);
			}
		} 
		catch (ClassNotFoundException e) { 
			System.out.println("JDBC Driver not found! Include in your library path!");
			e.printStackTrace();
			return; 
		} 
		catch (SQLException e) { 
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
			return; 
		}
	}
	
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			System.out.println("Error in closing DB connection!");
			e.printStackTrace();
		}
	}

	public void insertPageData(JsonObject pg, String ts) {
		String sql = "";
		try {			
			Statement s1 = connection.createStatement();
			
			sql = String.format("SELECT * FROM page_table WHERE page_id = '%s' AND ts_page = '%s'", pg.getString("id"), ts);
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				sql = String.format("INSERT INTO page_table VALUES (%s, '%s', '%s', %s, %s, %s)", 
						pg.getString("id"), ts, pg.getString("name"), pg.getString("likes"), pg.getString("talking_about_count"), pg.getString("were_here_count"));
				s1.executeUpdate(sql);				
			} else {
				//Repeat data!!
			}
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}
	
	public void insertPost(Post post, String page_name){
		String sql = "";
		try {			
			Statement s1 = connection.createStatement();
			String msg, story;
			
			//First check if post is already available:
			sql = String.format("SELECT * FROM post_table WHERE post_id = '%s'", post.getId());
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				if (post.getMessage() == null) {
					msg = "null";
				} else {
					msg = "'" + post.getMessage().replace("'", "") + "'";
				}
				
				if (post.getStory() == null) {
					story = "null";
				} else {
					story = "'" + post.getStory().replace("'", "") + "'";
				}
				
				sql = String.format("INSERT INTO post_table VALUES ('%s', '%s', %s, %s, '%s', '%s', '%s', %s, '%s')", 
						post.getId(), post.getCreatedTime(), post.getFrom().getId(), msg, post.getType(), post.getStatusType(),	post.getTo(), story, page_name);
				s1.executeUpdate(sql);				
			}					
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}
	
	public void insertPostCount(String id, String ts, int s_count, int l_count, int c_count, int flag) {
		String sql = "";
		try{
			String s_str, l_str, c_str;
			if (s_count == -1) {
				s_str = "null";
			} else {
				s_str = Integer.toString(s_count);
			}
			if (l_count == -1) {
				l_str = "null";
			} else {
				l_str = Integer.toString(l_count);
			}
			if (c_count == -1) {
				c_str = "null";
			} else {
				c_str = Integer.toString(c_count);
			}
			
			Statement s1 = connection.createStatement();
			sql = String.format("SELECT * FROM post_counts_table "
							  + "WHERE post_id = '%s' AND ts_post = '%s'", 
							  id, ts);
			ResultSet rs = s1.executeQuery(sql);
			
			if (!rs.isBeforeFirst()) {
				sql = String.format("INSERT INTO post_counts_table "
								  + "VALUES ('%s', '%s', %s, %s, %s)", 
								  id, ts, s_str, l_str, c_str);
				s1.executeUpdate(sql);
			} else {
				switch (flag) {
					case 1: sql = String.format("UPDATE post_counts_table "
								  + "SET shares_count = %s "
								  + "WHERE post_id = '%s' AND ts_post = '%s'", 
								  s_str, id, ts);
							s1.executeUpdate(sql);
							break;
					case 2: sql = String.format("UPDATE post_counts_table "
							  + "SET likes_count = %s "
							  + "WHERE post_id = '%s' AND ts_post = '%s'", 
							  l_str, id, ts);
							s1.executeUpdate(sql);
							break;
					case 3: sql = String.format("UPDATE post_counts_table "
							  + "SET comments_count = %s "
							  + "WHERE post_id = '%s' AND ts_post = '%s'", 
							  c_str, id, ts);
							s1.executeUpdate(sql);
							break;
				}
			}
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}

	public void insertFan(JsonObject jsonObject, String type) {
		String sql = "", name;
		try {			
			Statement s1 = connection.createStatement();
					
			//First check if fan is already available:
			sql = String.format("SELECT * FROM page_fans WHERE user_id = '%s'", jsonObject.getString("id"));
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				name = jsonObject.getString("name").replace("'", " ");
				sql = String.format("INSERT INTO page_fans VALUES ('%s', '%s', '%s')", jsonObject.getString("id"), name, type);
				s1.executeUpdate(sql);				
			}					
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}		
	}
	
	public void insertPostLike(String postId, JsonObject jsonObject, String ts) {
		String sql = "";
		try {			
			Statement s1 = connection.createStatement();
			
			//First check if post is already available:
			sql = String.format("SELECT * FROM post_likes_table WHERE post_id = '%s' AND user_id = '%s'", postId, jsonObject.getString("id"));
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				sql = String.format("INSERT INTO post_likes_table VALUES ('%s', '%s', '%s')", jsonObject.getString("id"), postId, ts);
				s1.executeUpdate(sql);				
			}					
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}		
	}
	

	public void insertCommentData(String postId, JsonObject jsonObject) {
		String sql = "", msg;
		try {			
			Statement s1 = connection.createStatement();
			
			//First check if post is already available:
			sql = String.format("SELECT * FROM comments_table WHERE comment_id = '%s'", jsonObject.getString("id"));
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				msg = "'" + jsonObject.getString("message").replace("'", "") + "'";
				
				sql = String.format("INSERT INTO comments_table VALUES ('%s', '%s', '%s', %s, '%s')", 
						jsonObject.getString("id"),	postId, jsonObject.getJsonObject("from").getString("id"),
						msg, jsonObject.getString("created_time"));
				s1.executeUpdate(sql);				
			}					
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}

	public void insertCommentCount(String comment_id, String ts, int l_count, int r_count, int flag) {
		String sql = "";
		try{
			String l_str, r_str;
			if (l_count == -1) {
				l_str = "null";
			} else {
				l_str = Integer.toString(l_count);
			}
			if (r_count == -1) {
				r_str = "null";
			} else {
				r_str = Integer.toString(r_count);
			}
			
			Statement s1 = connection.createStatement();
			sql = String.format("SELECT * FROM comment_count_table "
							  + "WHERE comment_id = '%s' AND ts_comment = '%s'", 
							  comment_id, ts);
			ResultSet rs = s1.executeQuery(sql);
			
			if (!rs.isBeforeFirst()) {
				sql = String.format("INSERT INTO comment_count_table "
								  + "VALUES ('%s', '%s', %s, %s)", 
								  comment_id, ts, l_str, r_str);
				s1.executeUpdate(sql);
			} else {
				switch (flag) {
					case 1: sql = String.format("UPDATE comment_count_table "
							  + "SET like_count = %s "
							  + "WHERE comment_id = '%s' AND ts_comment = '%s'", 
							  l_str, comment_id, ts);
							s1.executeUpdate(sql);
							break;
					case 2: sql = String.format("UPDATE comment_count_table "
							  + "SET reply_count = %s "
							  + "WHERE comment_id = '%s' AND ts_comment = '%s'", 
							  r_str, comment_id, ts);
							s1.executeUpdate(sql);
							break;
				}
			}
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}

	public void insertCommentLike(String comment_id, JsonObject jsonObject, String ts) {
		String sql = "";
		try {			
			Statement s1 = connection.createStatement();
			
			//First check if post is already available:
			sql = String.format("SELECT * FROM comment_likes_table WHERE comment_id = '%s' AND user_id = '%s'", comment_id, jsonObject.getString("id"));
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				sql = String.format("INSERT INTO comment_likes_table VALUES ('%s', '%s', '%s')", jsonObject.getString("id"), comment_id, ts);
				s1.executeUpdate(sql);				
			}					
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}

	public void insertReplyData(String c_id, JsonObject jO) {
		String sql = "", msg;
		try {			
			Statement s1 = connection.createStatement();
			
			//First check if reply is already available:
			sql = String.format("SELECT * FROM reply_table WHERE reply_id = '%s'", jO.getString("id"));
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				msg = "'" + jO.getString("message").replace("'", "") + "'";
				sql = String.format("INSERT INTO reply_table VALUES ('%s', '%s', '%s', %s, '%s')", 
						jO.getString("id"),	c_id, jO.getJsonObject("from").getString("id"),
						msg, jO.getString("created_time"));
				s1.executeUpdate(sql);				
			}					
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}

	public void insertReplyCount(String rep_id, String ts, int l_count) {
		String sql = "";
		try{
			String l_str;
			if (l_count == -1) {
				l_str = "null";
			} else {
				l_str = Integer.toString(l_count);
			}
			
			Statement s1 = connection.createStatement();
			sql = String.format("SELECT * FROM reply_count_table "
							  + "WHERE reply_id = '%s' AND ts_reply = '%s'", 
							  rep_id, ts);
			ResultSet rs = s1.executeQuery(sql);
			
			if (!rs.isBeforeFirst()) {
				sql = String.format("INSERT INTO reply_count_table "
								  + "VALUES ('%s', '%s', %s)", 
								  rep_id, ts, l_str);
				s1.executeUpdate(sql);
			} else {
				System.out.println("Recursion!! Updating reply like count table anyways......");
				sql = String.format("UPDATE reply_count_table SET like_count = %s "
							  	  + "WHERE reply_id = '%s' AND ts_reply = '%s'", 
							  	  l_str, rep_id, ts);
				s1.executeUpdate(sql);							
			}
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}
	
	public void insertReplyLike(String rep_id, JsonObject jsonObject, String ts) {
		String sql = "";
		try {			
			Statement s1 = connection.createStatement();
			
			//First check if post is already available:
			sql = String.format("SELECT * FROM reply_likes_table WHERE reply_id = '%s' AND user_id = '%s'", rep_id, jsonObject.getString("id"));
			ResultSet rs = s1.executeQuery(sql);
			if (!rs.isBeforeFirst()) {
				sql = String.format("INSERT INTO reply_likes_table VALUES ('%s', '%s', '%s')", jsonObject.getString("id"), rep_id, ts);
				s1.executeUpdate(sql);				
			}					
			s1.close();
		} catch (SQLException e) {
			System.out.println("Error executing statement: " + sql);
			e.printStackTrace();
		}
	}
}